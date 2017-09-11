package cn.jugame.lab.cluster;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unicast {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private DatagramSocket socket;
	
	private int soTimeout = 5000; //默认5s钟读取超时！
	
	public boolean init(int port){
		return init("0.0.0.0", port);
	}
	
	public boolean init(String addr, int port){
		try{
			socket = new DatagramSocket(port, InetAddress.getByName(addr));
			socket.setReuseAddress(true);
			socket.setSoTimeout(soTimeout);
			
			return true;
		}catch(Exception e){
			logger.error("unicast init error", e);
			return false;
		}
	}
	
	public boolean send(String msg, String targetHost, int targetPort){
		try{
			return send(msg.getBytes("UTF-8"), targetHost, targetPort);
		}catch(Exception e){
			logger.error("send error", e);
			return false;
		}
	}
	
	public boolean send(byte[] msg, String targetHost, int targetPort){
		if(socket == null)
			return false;
		
		try{
			DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(targetHost), targetPort);
			socket.send(packet);
			return true;
		}catch(Exception e){
			logger.error("send error", e);
			return false;
		}
	}
	
	public DatagramPacket recv(DatagramPacket packet){
		if(socket == null)
			return null;
		
		try{
			socket.receive(packet);
			return packet;
		}catch(Exception e){
			logger.error("recv error", e);
			return null;
		}
	}
	
	public void close(){
		if(socket == null)
			return;
		
		socket.close();
		socket = null;
	}
	
	public static void main(String[] args) throws Exception{
		final Unicast unicast1 = new Unicast();
		if(!unicast1.init(9101)){
			System.out.println("unicast1 fail!");
			return;
		}
		final Unicast unicast2 = new Unicast();
		if(!unicast2.init(9102)){
			System.out.println("unicast2 fail!");
			return;
		}

		System.out.println("启动unicast1... ");
		new Thread(){
			public void run() {
				while(true){
					DatagramPacket packet = unicast1.recv(new DatagramPacket(new byte[1024], 1024));
					if(packet == null){
						System.out.println("暂时没有数据...");
						return;
					}
					try{
						System.out.println("recv => " + new String(packet.getData(), 0, packet.getLength(), "UTF-8"));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			};
		}.start();
		
		try{
			Thread.sleep(2000);
		}catch(Exception e){}
		System.out.println("启动unicast2 ... ");
		
		new Thread(){
			public void run() {
				for(int i=0; i<10; ++i){
					try{
						Thread.sleep(1000);
						unicast2.send("hello world", "192.168.0.107", 9101);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			};
		}.start();
	}
}
