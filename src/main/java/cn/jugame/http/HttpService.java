package cn.jugame.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.jugame.mt.MtServer;
import cn.jugame.util.Config;

public class HttpService {
	protected MtServer serv;
	
	private static Logger logger = LoggerFactory.getLogger(HttpService.class);
	
	private HttpJob job;
	public HttpService(HttpJob job){
		this.job = job;
	}
	
	public boolean init(){
		int so_timeout = Config.getValueInt("so_timeout");
		try{
			serv = new MtServer(Config.getValueInt("server_port"), 
					Config.getValueInt("server_thread_count"), 
					Config.getValueInt("max_connections"),
					new HttpChannelStream(so_timeout));
			//job关联一下mtserv
			job.setMtServer(serv);
			serv.register(job);
			
			//设置读超时
			serv.setSoTimeout(so_timeout);
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public void run(){
		try{
			//开始监听用户请求
			logger.info("服务启动成功，开始监听用户请求");
			System.out.println("启动服务成功，开始监听用户请求.");
			System.out.println("---------------------------");
			serv.loop();
			serv.accpet();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
