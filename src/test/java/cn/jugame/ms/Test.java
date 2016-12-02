package cn.jugame.ms;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.jugame.msg.MessageProtocalParser;
import cn.jugame.msg.MessageProtocalParser;
import cn.jugame.mt.NioService;
import cn.jugame.mt.ProtocalParser;
import cn.jugame.mt.ProtocalParserFactory;

public class Test {
	private static ApplicationContext ctx = new ClassPathXmlApplicationContext("spring-config.xml");
	public static void main(String[] args) {
		NioService service = new NioService(9999);
		service.setJob(new MsJob());
		service.setProtocalParserFactory(new ProtocalParserFactory() {
			@Override
			public ProtocalParser create() {
				return new MessageProtocalParser();
			}
		});
		if(!service.init()){
			System.out.println("初始化NioService失败");
			return;
		}
		service.accpet();
		
		System.out.println("结束了...");
	}
}
