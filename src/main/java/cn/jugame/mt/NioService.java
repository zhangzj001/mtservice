package cn.jugame.mt;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private LRUSocketManager mng;
	private int port;
	private int so_timeout = 10 * 1000; //默认10s作为读超时
	private int reactorCount;
	private List<Reactor> reactors = new ArrayList<Reactor>();
	private Job job;
	private ProtocalParserFactory parserFactory;
	private ExecutorService reactorService;
	public NioService(int port, int reactorCount, int capacity){
		this.port = port;
		this.mng = new LRUSocketManager(capacity);
		this.reactorCount = reactorCount;
	}
	
	public void setJob(Job job){
		this.job = job;
	}
	
	public void setProtocalParserFactory(ProtocalParserFactory parserFactory){
		this.parserFactory = parserFactory;
	}
	
	public boolean init(){
		//初始化reactor
		for(int i=0; i<reactorCount; ++i){
			Reactor reactor = new Reactor("reactor_" + i, job);
			if(!reactor.init()){
				logger.error("初始化reactor失败");
			}
			reactors.add(reactor);
		}
		//一个reactor都没有，还启动个毛!
		if(reactors.size() == 0){
			return false;
		}
		
		//判断一下必要的组件必须设置了
		if(job == null){
			logger.error("没有设置Job");
			return false;
		}
		if(parserFactory == null){
			logger.error("没有设置StreamReader");
			return false;
		}
		
		//启动reactor
		reactorService = Executors.newFixedThreadPool(reactors.size());
		for(int i=0; i<reactors.size(); ++i){
			reactorService.execute(reactors.get(i));
		}
		
		return true;
	}
	
	/**
	 * 设置socketchannel数据读取的超时秒数，默认为10s
	 * @param timeout
	 */
	public void setSoTimeout(int timeout){
		so_timeout = timeout * 1000;
	}
	
	/**
	 * 获取处理socket的reactor数量
	 * @return
	 */
	public int getReactorCount(){
		return reactors.size();
	}

	/**
	 * 接收请求。这是一个死循环+阻塞等待的方法，除非抛出异常或者强制关闭
	 */
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
						doAccept(channel);
					}
				}
			}
		}catch(Throwable e){
			logger.error("accpet error", e);
		}finally{
			//这里是不应该来到的地方，来到了那必然发生了灾难！！
			try{
				logger.debug("WTF！！！ServerSocketChannel has got some problems...");
				if(serv_channel != null && serv_channel.isOpen()){
					logger.debug("WTF！！ServerSocketChannel is still alive, i have to kill it and restart it.");
					serv_channel.close();
				}
			}catch(Throwable e){
				logger.error("accept error", e);
			}
		}
	}
	
	private int index = 0;
	private Reactor pick(){
		return reactors.get(index++ % reactors.size());
	}

	private void doAccept(ServerSocketChannel serv_channel) throws Exception {
		SocketChannel channel = serv_channel.accept();
		channel.configureBlocking(false);
		channel.socket().setReuseAddress(true);
		channel.socket().setSoTimeout(so_timeout); //10s的数据读取时间，避免客户端慢读
		NioSocket socket = new NioSocket(channel, this.parserFactory);
		
		//创建channel的时候更新LRU管理器
		NioSocket oldSocket = mng.add(socket);
		//有溢出现象
		if(oldSocket != null){
			if(oldSocket.isOpen()){
				logger.warn("存在socket溢出现象，关闭最早的socket...");
				closeNioSocket(oldSocket);
			}
		}
		
		//挑一个reactor出来把channel塞进去做轮询
		Reactor reactor = pick();
		if(!reactor.add(socket)){
			logger.error("接收socket出现错误，关闭这个这个socket");
			socket.close();
		}
	}
	
	private void closeNioSocket(NioSocket socket){
		//任务在关闭channel前可以做一些事情
		job.beforeCloseSocket(socket);
		//关闭channel前先从LRU管理器中移除
		mng.remove(socket);
		//最后关闭这个socket
		socket.close();
	}
	
}
