package org.seckill.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
@Service
public class SeckillServiceImpl implements SeckillService{
	//盐值,制作MD5用的
	String slat = "sdfsf1@#RET#$%^$%GWEAWE";
	
	//注入redisdao用于缓存exportSeckillUrl方法
	@Autowired
	private RedisDao redisDao;
	
	//日志
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	//需要调用的DAO注入
	@Autowired
	private SeckillDao seckillDao;
	@Autowired
	private SuccessKilledDao successKilledDao;
	
	public List<Seckill> getSeckillList() {
		return seckillDao.queryAll(0, 4);
	}

	public Seckill getById(long seckillId) {
		return seckillDao.queryById(seckillId);
	}

	
	public Exposer exportSeckillUrl(long seckillId) {
		//所有用户等到秒杀开启时都需要等待这个接口的暴露,这个接口的暴露只是查一下秒杀地址,所以可以将他加入到缓存中,
		//这样一个用户通过数据库得到暴露地址后,暴露地址加入缓存中,这样以后的用户想要得到这个地址不需要访问数据库就可以在缓存中得到了
		
		//优化点1:缓存优化
		//第一步,访问redis
		Seckill seckill = redisDao.getSeckill(seckillId);
		if(seckill==null){
			//根据ID查询秒杀单品是否存在
			//第二步,如果redis中寻找的seckill是空,直接去数据库中查找对象
			seckill = seckillDao.queryById(seckillId);
			//判断秒杀单品是否为空,如果为空给出一个暴露对象,不为空的话继续判断是否在可秒杀时间段内
			if(seckill == null){
				return new Exposer(false,seckillId);
			}else{
				//数据库中找到对象后将其放到redis里面
				redisDao.putSeckill(seckill);
			}
		}
		
		//如果单品不为空,判断当前系统时间是否在秒杀允许时间段内
		Date now = new Date();
		Date startTime = seckill.getStartTime();
		Date endTime = seckill.getEndTime();
		if(now.getTime()<startTime.getTime() || now.getTime()>endTime.getTime()){
			return new Exposer(false,seckillId,now.getTime(),startTime.getTime(),endTime.getTime());
		}
		String md5 = getMD5(seckillId);
		return new Exposer(true,md5,seckillId);
	}
	
	//转换特定字符串到一个加密密码的算法,不可逆
	private String getMD5(long seckillId){
		String base = seckillId + "/" + slat;
		String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
		return md5;
	}
	
	//执行秒杀业务层操作
	/**
	 * 使用注解控制事务方法的优点:
	 * 1.开发团队可以达成一致的约定,明确标注事务方法的编程风格
	 * 2.保证事务方法的执行时间尽可能的短,不要穿插一些其他的网络操作,如果一定需要的话剥离到事务方法之外
	 * 3.不是所有的业务都需要事务,比如只有一条修改操作的业务或者一些查询业务,标注事务方法可以避免涉及到不需要事务的方法上的多余配置
	 */
	@Transactional
	public SeckillExecution executeSeckill(long seckillId, long userPhone,
			String md5) throws SeckillException, RepeatKillException,
			SeckillCloseException {
		
		//判断MD5值是否正常
		if(md5==null || !md5.equals(getMD5(seckillId))){
			throw new SeckillException("seckill data rewrite");
		}
		//如果MD5值正确的话先执行减库存操作
		Date nowTime = new Date();
		
		
		//源代码的顺序发生变化
		//原来是先减库存再插入购买明细
		//现在先插入购买明细,然后再减库存,因为减库存是对一条数据的竞争,访问时会产生行级锁,这一过程不能并发
		//而插入购买明细可以并发执行,放在减库存前面执行的目的是减少行级锁的持有时间,这样大量的用户秒杀的操作执行时间会更短
		//其中的网络延迟和gc操作的时间会缩短一倍
		try {
			//如果减库存操作成功继续执行插入购买明细方法
			int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
			//判断影响行数是否为0,如果为0说明用户重复秒杀操作,直接抛出异常
			if(insertCount<=0){
				throw new RepeatKillException("repeat seckill");
			}else{
				int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
				//判断影响行数是否为0,如果为0说明秒杀已经结束或者库存不足,直接抛出一个秒杀结束异常
				if(updateCount<=0){
					throw new SeckillCloseException("seckill over");
				}else{
					SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
					return new SeckillExecution(seckillId,SeckillStatEnum.SUCCESS,successKilled);
				}
			}
		} catch (SeckillCloseException e1){
			throw e1;
		} catch (RepeatKillException e2){
			throw e2;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			//所有编译时异常转化为运行时异常,因为spring识别出运行时异常时可以执行rollback操作
			throw new SeckillException("seckill inner error" + e.getMessage());
		}
		
	}
	
	
	
	//通过存储过程调用执行秒杀操作
	//之前这个方法需要抛出多个异常,目的是告诉spring的声明式事务什么逻辑下rooback,什么逻辑下commit,
	//因为springmvc是通过运行时异常来处理事务的commit和rollback的,现在的方法是调用存储过程执行秒杀实物,已经不需要
	//在本地通过springmvc来处理事务了,所以这些异常可以不用再抛出了
	public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
		//判断MD5
		if(md5==null || !md5.equals(getMD5(seckillId))){
			return new SeckillExecution(seckillId, SeckillStatEnum.DATA_REWRITE);
		}
		//获取一下系统时间
		Date killTime = new Date();
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("seckillId", seckillId);
		map.put("phone", userPhone);
		map.put("killTime", killTime);
		map.put("result", null);
		//用map传递参数的目的是存入result,调用存储过程得到结果后,可以再从map中取出result,这时的result已经被赋值成功
		try {
			seckillDao.killByProcedure(map);
			//获取result
			int result = MapUtils.getInteger(map, "result", -2);
			if(result==1){
				SuccessKilled sk = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
				return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, sk);
			}else{
				return new SeckillExecution(seckillId, SeckillStatEnum.stateOf(result));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
		}
	}
	/*
	org.springframework.dao.TransientDataAccessResourceException: 
		### Error querying database.  Cause: java.sql.SQLException: Parameter number 4 is not an OUT parameter
		### The error may exist in file [F:\Java2EEWorkSpace\seckill\target\classes\mapper\SeckillDao.xml]
		### The error may involve org.seckill.dao.SeckillDao.killByProcedure
		### The error occurred while executing a query
		### SQL: call execute_seckill(    ?,    ?,    ?,    ?   )
		### Cause: java.sql.SQLException: Parameter number 4 is not an OUT parameter
		; SQL []; Parameter number 4 is not an OUT parameter; nested exception is java.sql.SQLException: Parameter number 4 is not an OUT parameter
*/
}
