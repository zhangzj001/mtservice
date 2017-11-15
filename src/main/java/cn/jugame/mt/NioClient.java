package cn.jugame.mt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.tree.DefaultTreeCellEditor.EditorContainer;

import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.sm.SocketManager;
import cn.jugame.util.ByteHelper;
import cn.jugame.util.Common;
import cn.jugame.util.M1;
import net.sf.json.JSONObject;

/**
 * 基于NIO的socket客户端
 * @author ASUS
 *
 */
public class NioClient implements INio{
	private static Logger logger = LoggerFactory.getLogger(NioClient.class);
	
	//心跳时间，毫秒
	private int heartbeat = 60000;
	//读缓存初始大小
	private int readBufferSize = 4096 * 1024 * 1024; //4M
	//写缓存限制大小
	private int maxSendBufferSize = 8192 * 1024 * 1024; //8M
	//socket超时时间
	private int soTimeout = 10000;
	
	//多个服务端
	private List<NioSocket> socketList = new ArrayList<>();
	private Job job;

	//协议解析
	private ProtocalParserFactory parserFactory;
	
	//reactor
	private Context context = new Context(this);
	private List<Reactor> reactors = new ArrayList<Reactor>();
	private ExecutorService reactorService;
	private int reactorCount = Runtime.getRuntime().availableProcessors(); //当前cpu的数量
	
	//工作线程
	private TaskExecutor taskExecutor;
	
	
	@Override
	public Job getJob() {
		return job;
	}

	@Override
	public SocketManager getSocketManager() {
		//FIXME 客户端暂时不用socket管理
		return null;
	}

	@Override
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}
	
	/**
	 * 设置socket的读缓冲初始化大小，默认4M
	 * @param readBufferSize
	 */
	public void setReadBufferSize(int readBufferSize){
		this.readBufferSize = readBufferSize;
	}

	/**
	 * 设置socket的写缓冲最大值，默认为8M
	 * @param maxSendBufferSize
	 */
	public void setMaxSendBufferSize(int maxSendBufferSize){
		this.maxSendBufferSize = maxSendBufferSize;
	}
	
	/**
	 * 设置reactor池的大小，默认为cpu的个数
	 * @param reactorCount
	 */
	public void setReactorCount(int reactorCount){
		this.reactorCount = reactorCount;
	}
	
	/**
	 * 设置心跳时间，默认为60s
	 * @param sec 心跳时间，秒
	 */
	public void setHeartbeatTime(int sec){
		this.heartbeat = sec*1000;
	}
	
	/**
	 * socket客户端，支持连接池和心跳
	 * @param connStr 连接配置串，可以配置多个IP端口，使用分号隔开即可。如 192.168.1.191:9999;192.168.1.192:9999:5
	 */
	public NioClient(String connStr, ProtocalParserFactory factory){
		this.socketList = doConnect(connStr);
		this.taskExecutor = new TaskExecutor(this.socketList.size(), context);
		this.parserFactory = factory;
	}
	
	private List<NioSocket> doConnect(String connStr){
		if(StringUtils.isBlank(connStr)){
			throw new RuntimeException("没有连接配置");
		}
		
		List<NioSocket> socketList = new ArrayList<NioSocket>();
		
		String[] ip_ports = Common.array_filter(connStr.split(";"));
		for(String ip_port : ip_ports){
			String[] es = Common.array_filter(ip_port.split(":"));
			if(es.length == 2){
				NioSocket socket = connectSocket(es[0], Integer.parseInt(es[1]));
				if(socket != null){
					socketList.add(socket);
				}
			}
			//连接池的方式
			else if(es.length == 3){
				int size = Integer.parseInt(es[3]);
				for(int i=0; i<size; ++i){
					NioSocket socket = connectSocket(es[0], Integer.parseInt(es[1]));
					if(socket != null){
						socketList.add(socket);
					}
				}
			}
			else{
				throw new RuntimeException("连接配置不正确=> " + ip_port);
			}
		}
		
		if(socketList.size() == 0)
			throw new RuntimeException("没有解析到任何连接配置=> " + connStr);
		
		return socketList;
	}
	
	private NioSocket connectSocket(String host, int port){
		SocketChannel channel = null;
		try{
			channel = SocketChannel.open(new InetSocketAddress(host, port));
			channel.configureBlocking(false);	//非阻塞模式
			channel.socket().setKeepAlive(true);
			channel.socket().setReuseAddress(true);
			channel.socket().setSoTimeout(soTimeout);
			channel.socket().setTcpNoDelay(true);
			
			//封装成niosocket再返回
			NioSocket socket = new NioSocket(channel, this.parserFactory);
			socket.setReadBufferSize(readBufferSize);
			socket.setMaxSendBufferSize(maxSendBufferSize);
			
			return socket;
		}catch(Throwable e){
			logger.error("endpoint connect error", e);
			if(channel != null){
				try{channel.close();}catch(Throwable e2) {logger.error("error", e2);}
			}
			return null;
		}
	}
	
	/**
	 * 初始化
	 */
	public boolean init(){
		for(int i=0; i<reactorCount; ++i){
			Reactor reactor = new Reactor("client-" + i, context);
			if(!reactor.init()){
				logger.error("初始化reactor失败");
				continue;
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
		
		//初始化连接池，并将所有socket注册到reactor中
		for(int i=0; i<socketList.size(); ++i){
			pick().add(socketList.get(i));
		}
		
		return true;
	}
	
	private int index = 0;
	private Reactor pick(){
		return reactors.get(index++ % reactors.size());
	}
	
	/**
	 * 发送数据
	 * @param bs
	 * @return
	 */
	int soIndex = 0;
	public boolean send(byte[] bs){
		//挑选一个niosocket发送数据即可
		return socketList.get(soIndex++ % socketList.size()).send(bs);
	}
	
	public void stop(){
		//停止所有reactor
		for(Reactor reactor : reactors){
			reactor.stop();
		}
		
		//停止reactor线程池
		reactorService.shutdown();
		
		//停止工作线程，这里会等所有工作完成之后才会退出
		taskExecutor.stop();
		
		//将所有socket关闭
		for(NioSocket socket : socketList){
			socket.close();
		}
	}
}
