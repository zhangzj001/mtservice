package cn.jugame.mt;

import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reactor implements Runnable{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private Selector selector = null;
	private String name;
	private Context context;
	public Reactor(String name, Context context){
		this.name = name;
		this.context = context;
	}
	
	public boolean init(){
		try{
			this.selector = Selector.open();
			return true;
		}catch(Throwable e){
			logger.error("error", e);
			return false;
		}
	}
	
	public boolean add(NioSocket nioSocket){
		return add(nioSocket, SelectionKey.OP_READ);
	}
	
	public boolean add(NioSocket nioSocket, int ops){
		synchronized (this) {
			this.selector.wakeup();
			return nioSocket.register(this, ops);
		}
	}
	
	public Selector selector(){
		return this.selector;
	}
	
	@Override
	public void run() {
		try{
			logger.info("Reactor[" + name + "]开始运行...");
			while(true){
				int n = 0;
				try{
					n = selector.select();
				}catch(ClosedSelectorException e){
					//selector关闭了，直接退出
					logger.info("reactor【" + name + "】因selector关闭而退出了");
					return;
				}
				
				//加上这行是为了recv_selector.wakeup和channel.register两行代码进行同步处理。
				synchronized (this) {}
				if(n <= 0)
					continue;
				
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey readyKey = it.next();
					it.remove();
					NioSocket nioSocket = (NioSocket)readyKey.attachment();
					
					//如果是读事件
					if(readyKey.isValid() && readyKey.isReadable()){
						System.out.println("reading........");
						//读取数据到缓冲区
						int r = nioSocket.read();
						if(r == -1){
							logger.debug("关闭客户端socket[" + nioSocket.hashCode() + "]连接");
							context.releaseSocket(nioSocket);
							continue;
						}
						
						//如果数据已经可以构成应用层包，取出来干活
						Object packet = null;
						while((packet = nioSocket.popPacket()) != null){
							context.getTaskExecutor().execute(nioSocket, packet);
						}
					}
					//如果是写事件
					if(readyKey.isValid() && readyKey.isWritable()){
						System.out.println("writing......");
						if(!nioSocket.write()){
							logger.info("socket发送数据异常，关闭这个连接");
							context.releaseSocket(nioSocket);
							continue;
						}
					}
				}
			}
		}
		catch(ClosedSelectorException e){
			//这种情况是调用selector.close导致的。
		}
		catch(Throwable e){
			logger.error("Reactor[" + name + "]发生错误", e);
		}
		finally{
			logger.error("Reactor[" + name + "]即将关闭");
			try{this.selector.close();}catch(Exception e){logger.error("Reactor[" + name + "]关闭时发生错误", e);}
		}
	}
	
	public void stop(){
		try{selector.close();}catch(Throwable e){logger.error("close error", e);}
	}
}
