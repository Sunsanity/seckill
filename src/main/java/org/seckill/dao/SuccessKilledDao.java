package org.seckill.dao;

import org.apache.ibatis.annotations.Param;
import org.seckill.entity.SuccessKilled;

public interface SuccessKilledDao {
	
	/**
	 * 插入一条成功秒杀的记录
	 * @param seckillId	秒杀商品ID
	 * @param userPhone	秒杀成功用户电话
	 * @return 	返回的是数据库的更新条数 正常应该更新一条记录  但是因为某种原因如果条数是0,代表增加秒杀记录失败,可以根据逻辑执行特定的操作
	 */
	int insertSuccessKilled(@Param("seckillId") long seckillId,@Param("userPhone") long userPhone);
	
	/**
	 * 根据商品ID查询一条成功秒杀的商品详情以及用户成功秒杀记录
	 * @param seckillId 商品ID
	 * @return	返回一条秒杀记录	同时SuccessKilled里面还有商品对象的属性,所以意味着同时能返回成功记录的商品对象
	 */
	SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckillId,@Param("userPhone") long userPhone);
}
