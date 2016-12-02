package cn.jugame.msg;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.Job;
import cn.jugame.mt.NioService;
import cn.jugame.mt.NioSocket;
import cn.jugame.mt.ProtocalParser;
import cn.jugame.mt.ProtocalParserFactory;
import cn.jugame.mt.ServiceConfig;
import cn.jugame.util.Common;
import cn.jugame.util.JuConfig;
import cn.jugame.util.M1;
import net.sf.json.JSONObject;

public abstract class MessageService implements Job{

	private static Logger logger = LoggerFactory.getLogger(MessageService.class);
	
	private int server_port = 9999;
	private int worker_count = 16;
	private NioService service;
	
	public void setPort(int port){
		this.server_port = port;
	}
	
	public void setWorkerCount(int count){
		this.worker_count = count;
	}
	
	/**
	 * 对数据进行加密压缩
	 * @param s
	 * @return
	 */
	protected byte[] encode(String s){
		try{
			byte[] bs = s.getBytes("UTF-8");
			bs = M1.encode(bs);
			bs = Common.gzencode(bs);
			return bs;
		}catch(Exception e){
			logger.error("encode.error", e);
			return new byte[0];
		}
	}
	
	/**
	 * 对数据解压解密
	 * @param bs
	 * @return
	 */
	protected String decode(byte[] bs){
		try{
			//对数据内容先进行解压，再进行m1解密
			bs = Common.gzdecode(bs);
			if(bs == null)
				return null;
			StringBuffer sb = new StringBuffer();
			if(M1.decode(bs, sb) != 0)
				return null;
			
			return sb.toString();
		}catch(Exception e){
			logger.error("decode.error", e);
			return null;
		}
	}
	
	@Override
	public boolean doJob(NioSocket socket, Object packet) {
		try{
			//处理数据， 对数据内容先进行解压，再进行m1解密
			byte[] bs = (byte[])packet;
			String content = decode(bs);
			if(StringUtils.isBlank(content))
				return false;
			
			JSONObject json = JSONObject.fromObject(content);
			return doJob(socket, json);
		}catch(Exception e){
			logger.error("doJob error", e);
			return false;
		}
	}
	
	public boolean init(){
		int so_timeout = JuConfig.getValueInt("so_timeout");
		ServiceConfig config = new ServiceConfig();
		config.setSoTimeout(so_timeout);
		
		service = new NioService(this.server_port);
		service.setWorkerCount(this.worker_count);
		service.setJob(this);
		service.setConfig(config);
		service.setProtocalParserFactory(new ProtocalParserFactory() {
			@Override
			public ProtocalParser create() {
				return new MessageProtocalParser();
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

	/**
	 * 子类实现这个方法来处理数据。
	 * @param channel
	 * @param data
	 * @return
	 */
	protected abstract boolean doJob(NioSocket socket, JSONObject data);
	
	@Override
	public void beforeCloseSocket(NioSocket socket) {
		//默认什么也不做
	}
}
