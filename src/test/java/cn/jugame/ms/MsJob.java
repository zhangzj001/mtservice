package cn.jugame.ms;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.msg.MessageProtocalParser;
import cn.jugame.mt.Job;
import cn.jugame.mt.NioService;
import cn.jugame.mt.NioSocket;
import cn.jugame.mt.ProtocalParser;
import cn.jugame.mt.ProtocalParserFactory;
import cn.jugame.util.ByteHelper;
import cn.jugame.util.Common;
import cn.jugame.util.M1;
import net.sf.json.JSONObject;

public class MsJob implements Job{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
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
		byte[] bs = (byte[])packet;
		String s = decode(bs);
		System.out.println("收到客户端消息：" + s);
		
		JSONObject data = JSONObject.fromObject(s);
		if("test".equals(data.getString("action"))){
			JSONObject json = new JSONObject();
			json.put("client", "system");
			json.put("action", data.getString("action") + "_reply");
			json.put("time", System.currentTimeMillis());
			byte[] bytes = encode(json.toString());
			try{
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				os.write(ByteHelper.int2ByteArray(bytes.length));
				os.write(bytes);
				if(!socket.send(os.toByteArray())){
					logger.error("发送消息失败");
					return false;
				}
				return true;
			}catch(Exception e){
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	@Override
	public void beforeCloseSocket(NioSocket socket) {
		//do nothing
	}

	
	public static void main(String[] args) {
		NioService service = new NioService(9999);
		service.setJob(new MsJob());
		service.setReactorCount(4);
		service.setWorkerCount(16);
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
		if(!service.accpet()){
			System.out.println("NioService启动失败");
			return;
		}
		
		System.out.println("结束了...");
	}
}
