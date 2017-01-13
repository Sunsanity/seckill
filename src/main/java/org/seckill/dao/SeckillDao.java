package org.seckill.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.seckill.entity.Seckill;

public interface SeckillDao {
	
	/**
	 * 减库存的方法
	 * @param seckillId 商品ID
	 * @param killTime	秒杀时间
	 * @return	返回的是数据库的更新条数 正常应该更新一条记录  但是因为某种原因如果条数是0,代表减库存操作失败,可以根据逻辑执行特定的操作
	 */
	int reduceNumber(@Param("seckillId") long seckillId,@Param("killTime") Date killTime);
	
	/**
	 * 通过ID查询库存商品方法
	 * @param seckillId 
	 * @return 商品对象
	 */
	Seckill queryById(long seckillId);
	
	/**
	 * 根据偏移量查找全部的商品列表
	 * @param offset	偏移量
	 * @param limit	查询的数量
	 * @return	商品列表
	 */
	//两个形式参数的需要加上mybatis的注解用于让DAO实现类来识别出对应的形式参数
	List<Seckill> queryAll(@Param("offset") int offset,@Param("limit") int limit);
	
	
	/**
	 * 使用存储过程执行秒杀操作
	 * @param paramMap
	 */
	void killByProcedure(Map<String,Object> paramMap);
	
	
}
