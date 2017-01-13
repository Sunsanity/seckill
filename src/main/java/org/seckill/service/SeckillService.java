package org.seckill.service;

import java.util.List;

import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;

/**
 * 商品库存service接口
 * @author SJW
 *
 */

public interface SeckillService {
	
	/**
	 * 查询所有可秒杀商品方法
	 * @return
	 */
	List<Seckill> getSeckillList();
	
	/**
	 * 根据ID查询单个可秒杀商品
	 * @return
	 */
	Seckill getById(long seckillId);
	
	/**
	 * 根据商品ID查询单个商品的秒杀详情:如果秒杀开始就输出一个秒杀地址
	 * 如果秒杀还没有到开启时间就输出当前时间和秒杀时间
	 * 返回类型封装成一个存储秒杀信息的类
	 * @param seckillId
	 */
	Exposer exportSeckillUrl(long seckillId);
	
	/**
	 * 执行秒杀操作
	 * @param seckillId
	 * @param userPhone
	 * @param md5 验证之前得到的MD5和执行秒杀给出的MD5是否一致,如果一直可以允许执行秒杀操作
	 */
	SeckillExecution executeSeckill(long seckillId,long userPhone,String md5)
			throws SeckillException,RepeatKillException,SeckillCloseException;
	
	
	
	/**
	 * 执行秒杀操作  通过存储过程调用秒杀操作
	 * @param seckillId
	 * @param userPhone
	 * @param md5 验证之前得到的MD5和执行秒杀给出的MD5是否一致,如果一直可以允许执行秒杀操作
	 */
	SeckillExecution executeSeckillProcedure(long seckillId,long userPhone,String md5);
}
