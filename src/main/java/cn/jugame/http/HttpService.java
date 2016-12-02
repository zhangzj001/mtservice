package cn.jugame.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.NioService;
import cn.jugame.mt.ProtocalParser;
import cn.jugame.mt.ProtocalParserFactory;
import cn.jugame.mt.ServiceConfig;
import cn.jugame.util.JuConfig;

public class HttpService {
	
	private static Logger logger = LoggerFactory.getLogger(HttpService.class);
	
	private NioService service;
	private HttpJob job;
	public HttpService(HttpJob job){
		this.job = job;
	}
	
	public boolean init(){
		int so_timeout = JuConfig.getValueInt("so_timeout");
		ServiceConfig config = new ServiceConfig();
		config.setSoTimeout(so_timeout);
		
		service = new NioService(JuConfig.getValueInt("server_port"));
		service.setReactorCount(JuConfig.getValueInt("server_reactor_count"));
		service.setWorkerCount(JuConfig.getValueInt("server_worker_count"));
		service.setJob(this.job);
		service.setConfig(config);
		service.setProtocalParserFactory(new ProtocalParserFactory() {
			@Override
			public ProtocalParser create() {
				return new HttpParser();
			}
		});
		return service.init();
	}
	
	public void run(){
		//开始监听用户请求
		logger.info("服务启动成功，开始监听用户请求");
		while(!service.accpet()){
			logger.error("服务出现错误，重启Acceptor!");
		}
	}
}
