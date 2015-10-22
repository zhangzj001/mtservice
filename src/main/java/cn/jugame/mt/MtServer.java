package cn.jugame.mt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于nio的一个服务端。
 * 注意：所有socketchannel的管理，都必须调用write_channel，read_channel和close_channel方法。
 * 只有这样才能将socketchannel都管理在LRUChannelManager的范围内。
 * @author zimT_T
 *
 */
public class MtServer {
	
	private static Logger logger = LoggerFactory.getLogger("watcher");

	/**
	 * 用来做轮询的一个内部类
	 * 这个类存在的价值纯粹就是我不希望代码写得一个括号套一个括号。。。虽然现在也有点这样。。
	 * @author zimT_T
	 *
	 */
	private class Looper implements Runnable{
		Selector recv_selector;
		public Looper(Selector recv_selector){
			this.recv_selector = recv_selector;
		}
		
		private void do_run(){
			int n = 0;
			try{
				n = recv_selector.select();
			}catch(Exception e){
				logger.error("error", e);
			}
			
			//加上这行是为了recv_selector.wakeup和channel.register两行代码进行同步处理。
			synchronized (this) {}
			
			if(n > 0){
				Iterator<SelectionKey> it = recv_selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey readyKey = it.next();
					it.remove();
					
					SocketChannel channel = (SocketChannel)readyKey.channel();
					logger.debug("channel ready for read, channel.hashCode=>" + channel.hashCode());
					
					//XXX 必须要先读出数据来，否则如果先进了队列，而队列又来不及读取，while就会进入死循环
					MtPackage req = read_channel(channel);
					//如果遇到了IO错误，一般是客户端自己关掉了socket
					if(req == null){
						logger.debug("some io error while reading from client, possibly client closes channel");
						close_channel(channel);
						continue;
					}

					//如果数据还没完全准备好，那就等准备好了再说
					if(!req.isReady()){
						continue;
					}
					
					//把channel和数据一块打包带进队列中处理
					if(!do_jobs(channel, req)){
						close_channel(channel);
					}
				}
			}
		}
		
		@Override
		public void run() {
			while(true){
				do_run();
			}
		}
	}
	
	private LRUChannelManager mng;
	private MtChannelStream stream;
	private List<Looper> loopers = new ArrayList<>();
	private int port;
	private int thread_count;
	private int so_timeout = 10 * 1000; //默认10s作为读超时
	public MtServer(int port, int thread_count, int capacity, MtChannelStream stream){
		this.port = port;
		this.thread_count = thread_count;
		this.mng = new LRUChannelManager(capacity);
		this.stream = stream;
	}
	
	private int index = 0;
	private Looper get_looper(){
		if(index >= loopers.size())
			index = 0;
		return loopers.get(index++);
	}
	
	/**
	 * 设置socketchannel数据读取的超时秒数，默认为10s
	 * @param timeout
	 */
	public void setSoTimeout(int timeout){
		so_timeout = timeout * 1000;
	}
	
	private LinkedList<MtJob> jobs = new LinkedList<>();
	public void register(MtJob job){
		jobs.add(job);
	}
	
	private boolean do_jobs(SocketChannel channel, MtPackage req){
		for(MtJob job : jobs){
			//如果存在doJob失败的情况，直接断开客户端连接
			if(!job.do_job(channel, req)){
				return false;
			}
		}
		return true;
	}
	
	private void do_loop() throws Exception{
		ExecutorService service = Executors.newFixedThreadPool(loopers.size());
		for(int i=0; i<loopers.size(); ++i){
			service.execute(loopers.get(i));
		}
	}

	/**
	 * 单独一个线程专门用来运转recv_selector
	 * @throws Exception
	 */
	public boolean loop(){
		try{
			//初始化recv_selectors
			for(int i=0; i<thread_count; ++i){
				Selector selector = Selector.open();
				loopers.add(new Looper(selector));
			}
		}catch(IOException e){
			logger.error("error", e);
			return false;
		}
		
		//开始轮询recv_selector
		new Thread(){
			public void run() {
				try{
					do_loop();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}.start();
		
		return true;
	}

	public void accpet() {
		ServerSocketChannel serv_channel = null;
		try{
			final Selector selector = Selector.open();
			serv_channel = ServerSocketChannel.open();
	
			// 非阻塞
			serv_channel.configureBlocking(false);
			serv_channel.socket().setReuseAddress(true);
			serv_channel.socket().bind(new InetSocketAddress(this.port));
	
			// 注册accept
			serv_channel.register(selector, SelectionKey.OP_ACCEPT);
			
			logger.debug("监听ing...");
			while(selector.select() > 0) {
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey readyKey = it.next();
					it.remove();
					if(readyKey.isValid() && readyKey.isAcceptable()){
						//接收客户端请求
						ServerSocketChannel channel = (ServerSocketChannel)readyKey.channel();
						logger.debug("new connection coming, channel.hashCode=>" + channel.hashCode());
						do_accept(channel);
					}
				}
			}
		}catch(Exception e){
			logger.error("error", e);
		}finally{
			//这里是不应该来到的地方，来到了那必然发生了灾难！！
			try{
				logger.debug("WTF！！！ServerSocketChannel got some problems...");
				if(serv_channel != null && serv_channel.isOpen()){
					logger.debug("WTF！！ServerSocketChannel is still alive, i have to kill it and restart it.");
					serv_channel.close();
				}
			}catch(Exception e){
				logger.error("error", e);
			}
		}
	}

	private void do_accept(ServerSocketChannel serv_channel) throws Exception {
		SocketChannel channel = serv_channel.accept();
		channel.configureBlocking(false);
		channel.socket().setReuseAddress(true);
		channel.socket().setSoTimeout(so_timeout); //10s的数据读取时间，避免客户端慢读
		
		//创建channel的时候更新LRU管理器
		SocketChannel old_channel = mng.add_channel(channel);
		//有溢出现象
		if(old_channel != null){
			if(old_channel.isOpen()){
				logger.warn("存在socketchannel溢出现象，关闭最早的socketchannel...");
				close_channel(old_channel);
			}
		}
		
		//注册到selector中等待内容读取
		logger.debug("register channel to recv_selector for looping");
		//挑一个selector出来把channel塞进去轮询
		Looper looper = get_looper();
		synchronized (looper) {
			logger.info("regist channel【" + channel.hashCode() + "】 into looper【" + looper.hashCode() + "】");
			looper.recv_selector.wakeup();
			channel.register(looper.recv_selector, SelectionKey.OP_READ);
		}
	}
	
	public MtPackage read_channel(SocketChannel channel){
		MtPackage req = stream.read_channel(channel);
		//如果读到数据，此时更新channel
		if(req != null){
			mng.update_channel(channel);
		}
		return req;
	}
	
	public boolean write_channel(SocketChannel channel, MtPackage resp){
		//如果写数据成功，也更新channel
		if(stream.write_channel(channel, resp)){
			mng.update_channel(channel);
			return true;
		}
		return false;
	}
	
	public boolean close_channel(SocketChannel channel){
		for(MtJob job : jobs){
			job.before_close_channel(channel);
		}
		
		//关闭channel前先从LRU管理器中移除
		mng.remove_channel(channel);
		return stream.close_channel(channel);
	}
	
}
