package cn.jugame.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.NioService;
import cn.jugame.mt.ProtocalParser;
import cn.jugame.mt.ProtocalParserFactory;
import cn.jugame.mt.ServiceConfig;

public class HttpService {
	
	private static Logger logger = LoggerFactory.getLogger(HttpService.class);

	private int soTimeout = 3000; //默认3s的读超时，够长了
	private int port = 9999;
	private int reactorCount = Runtime.getRuntime().availableProcessors();
	private int worderCount = Runtime.getRuntime().availableProcessors();
	
	private NioService service;
	private HttpJob job;
	public HttpService(HttpJob job){
		this.job = job;
	}
	
	public int getSoTimeout() {
		return soTimeout;
	}

	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getReactorCount() {
		return reactorCount;
	}

	public void setReactorCount(int reactorCount) {
		this.reactorCount = reactorCount;
	}

	public int getWorderCount() {
		return worderCount;
	}

	public void setWorderCount(int worderCount) {
		this.worderCount = worderCount;
	}

	public boolean init(){
		ServiceConfig config = new ServiceConfig();
		config.setSoTimeout(soTimeout);
		
		service = new NioService(port);
		service.setReactorCount(reactorCount);
		service.setWorkerCount(worderCount);
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
