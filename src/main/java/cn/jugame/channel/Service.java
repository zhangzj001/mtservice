package cn.jugame.channel;

import java.nio.channels.SocketChannel;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.MtJob;
import cn.jugame.mt.MtPackage;
import cn.jugame.mt.MtServer;
import cn.jugame.util.Common;
import cn.jugame.util.JuConfig;
import cn.jugame.util.M1;
import net.sf.json.JSONObject;

public abstract class Service implements MtJob{

	private static Logger logger = LoggerFactory.getLogger(Service.class);
	
	protected MtServer serv;
	private int server_port = 9999;
	private int server_thread_count = 16;
	private int max_connections = 10000;
	
	public void setPort(int port){
		this.server_port = port;
	}
	
	public void setWorkerCount(int count){
		this.server_thread_count = count;
	}
	
	public void setMaxConnections(int max){
		this.max_connections = max;
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
	public boolean do_job(SocketChannel channel, MtPackage packs) {
		try{
			//处理数据， 对数据内容先进行解压，再进行m1解密
			byte[] bs = packs.getData();
			String content = decode(bs);
			if(StringUtils.isBlank(content))
				return false;
			
			JSONObject json = JSONObject.fromObject(content);
			return do_job(channel, json);
		}catch(Exception e){
			logger.error("error", e);
			return false;
		}
	}
	
	public boolean init(){
		try{
			serv = new MtServer(this.server_port, 
					this.server_thread_count, 
					this.max_connections,
					new JobChannelStream());
			
			//设置读超时
			serv.setSoTimeout(JuConfig.getValueInt("socket_timeout"));
			serv.register(this);
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public void run(){
		logger.info("服务启动成功，开始监听用户请求");
		System.out.println("启动服务成功，开始监听用户请求.");
		System.out.println("---------------------------");

		//启动工作线程
		if(!serv.loop()){
			System.out.println("启动工作线程失败，服务退出");
			return;
		}

		//死循环监听连接，因为accept中的server_channel有可能因为各种原因退出。
		serv.accpet();
	}

	/**
	 * 子类实现这个方法来处理数据。
	 * @param channel
	 * @param data
	 * @return
	 */
	protected abstract boolean do_job(SocketChannel channel, JSONObject data);
}
