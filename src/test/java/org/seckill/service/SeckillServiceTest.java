package org.seckill.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * service集成测试类
 * 
 * @author SJW
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:spring/spring-dao.xml",
		"classpath:spring/spring-service.xml" })
public class SeckillServiceTest {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SeckillService seckillService;

	@Test
	public void testGetSeckillList() throws Exception {
		List<Seckill> list = seckillService.getSeckillList();
		logger.info("list={}", list);
	}

	@Test
	public void testGetById() throws Exception {
		long id = 1000;
		Seckill seckill = seckillService.getById(id);
		logger.info("seckill={}", seckill);
	}
	
	
	

	@Test
	public void testExportSeckillUrl() throws Exception {
		long id = 1000;
		Exposer exposer = seckillService.exportSeckillUrl(id);
		logger.info("exposer={}", exposer);
		/**
		 * Exposer [exposed=false, md5=null, seckillId=1000, now=1483689058095,
		 * start=1446307200000, end=1446393600000]
		 */
	}
	@Test
	public void testEcecuteSeckill() throws Exception {
		long id = 1000;
		long phone = 1861231211223123L;
		String md5 = "a072263731a28ddc966426c7b267f71a";
		// 以下代码遇到异常时单元测试不会通过,可以trycatch起来,利用日志来避免异常的错误
		try {
			SeckillExecution execution = seckillService.executeSeckill(id,
					phone, md5);
			logger.info("result={}", execution);
		} catch (RepeatKillException e) {
			logger.error(e.getMessage());
		} catch (SeckillCloseException e) {
			logger.error(e.getMessage());
		}
	}
	
	
	//以上两个单元测试正规的话应该写在一起变成一个测试流程,下面的代码才是测试代码的完整正确的逻辑
	@Test
	public void testSeckillLogic() throws Exception {
		long id = 1000;
		Exposer exposer = seckillService.exportSeckillUrl(id);
		if(exposer.isExposed()){
			logger.info("exposer={}", exposer);
			long phone = 1861231211223123L;
			String md5 = exposer.getMd5();
			// 以下代码遇到异常时单元测试不会通过,可以trycatch起来,利用日志来避免异常的错误
			try {
				SeckillExecution execution = seckillService.executeSeckill(id,
						phone, md5);
				logger.info("result={}", execution);
			} catch (RepeatKillException e) {
				logger.error(e.getMessage());
			} catch (SeckillCloseException e) {
				logger.error(e.getMessage());
			}
		}else{
			//秒杀未开启
			logger.warn("exposer={}", exposer);
		}
	}
	
	//测试通过存储过程执行秒杀操作是否成功
	@Test
	public void executeSeckillProcedure(){
		long seckillId = 1000;
		long phone = 18686903261L;
		Exposer exposer = seckillService.exportSeckillUrl(seckillId);
		if(exposer.isExposed()){
			String md5 = exposer.getMd5();
			SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId, phone, md5);
			logger.info(execution.getStateInfo());
		}
	}
	
	
	
	
}
