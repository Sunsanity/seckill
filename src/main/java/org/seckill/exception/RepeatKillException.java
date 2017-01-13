package org.seckill.exception;
/**
 * 重复秒杀异常(运行时异常)
 * @author SJW
 *
 */
public class RepeatKillException extends SeckillException{

	public RepeatKillException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public RepeatKillException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
	
}
