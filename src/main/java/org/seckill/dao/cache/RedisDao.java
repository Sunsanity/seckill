package org.seckill.dao.cache;

import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * redis的数据访问对象
 * @author SJW
 *
 */

public class RedisDao {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final JedisPool jedisPool;
	
	public RedisDao(String ip,int port){
		jedisPool = new JedisPool(ip,port);
	}
	
	
	private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);
	
	//从redis中拿出seckill对象
	public Seckill getSeckill(long seckillId){
		//redis逻辑代码
		//先获取到一个jedis对象
		try {
			Jedis jedis = jedisPool.getResource();
			try {
				//这个key作用是通过它来从redis里面取出我们需要的缓存对象
				String key = "seckill:" + seckillId;
				//redis内部并没有帮我们实现序列化功能,所以我们取出的对象实际上是一个二进制文件,所以我们需要通过反序列化方法来将取到的
				//二进制对象转换成我们需要的seckill对象,这里我们使用protostuff来实现反序列化的操作,不用java自带的serilizeble
				//接口,因为protostuff的速度更快
				byte[] bytes = jedis.get(key.getBytes());
				//判断是否拿到了seckill的二进制对象
				if(bytes != null){
					Seckill seckill = schema.newMessage();
					//调用工具类实现反序列化得到我们的seckill JAVA对象
					ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
					return seckill;
				}
			} finally{
				jedis.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return null;
	}
	
	
	//向redis中put一个seckill对象
	public String putSeckill(Seckill seckill){
		//这个过程是把seckill对象序列化成一个二进制数组,然后存到redis里面
		try {
				Jedis jedis = jedisPool.getResource();
			try {
				String key = "seckill:" + seckill.getSeckillId();
				byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema, 
						LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
				int timeout = 60 * 60; //缓存时间一小时
				String result = jedis.setex(key.getBytes(), timeout, bytes);
				return result;
			} finally{
				jedis.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	
}
