package cn.jugame.ms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.msg.MessageProtocalParser;
import cn.jugame.msg.MessageService;
import cn.jugame.mt.NioSocket;
import net.sf.json.JSONObject;

public class TestServer extends MessageService{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());


	@Override
	protected boolean doJob(NioSocket socket, JSONObject data) {
		System.out.println("receive a message: " + data);
		
		//send back this message!!
		if(!send(socket, data)){
			//terminate this socket!
			return false;
		}
		
		//keep-alive
		return true;
	}

	private boolean send(NioSocket socket, JSONObject content){
		if(!socket.isOpen()){
			logger.info("remote channel closed...");
			return false;
		}
		try{
			byte[] bs = MessageProtocalParser.toMessage(content.toString().getBytes("UTF-8"));
			return socket.send(bs);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		TestServer server = new TestServer();
		if(!server.init()){
			return;
		}
		server.run();
	}

}
