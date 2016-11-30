package cn.jugame.mt;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioSocket {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/*默认缓冲区大小*/
	private static final int DEFAULT_BUFF_SIZE = 8192;

	//写缓冲区
	private ByteBuffer outBuf;
	//协议解释器（由解释器提供读缓冲）
	private ProtocalParser parser;
	//应用层数据包
	private LinkedList<Object> packets = new LinkedList<>();
	
	private Reactor currReactor;
	private SocketChannel channel;
	
	//最大写缓冲
	private int maxSendBufferSize;
	private int readBufferSize;
	
	public NioSocket(SocketChannel channel, ProtocalParserFactory factory) {
		this.channel = channel;
		//构建第一个parser
		this.parser = factory.create();
		
		//默认使用socket的读缓冲大小
		try{
			readBufferSize = channel.socket().getReceiveBufferSize();
		}catch(Exception e){
			readBufferSize = DEFAULT_BUFF_SIZE;
		}
		maxSendBufferSize = readBufferSize*1024; //上升一个单位，8k变8m
		outBuf = ByteBuffer.wrap(new byte[readBufferSize]);
	}
	
	public void setReadBufferSize(int readBufferSize){
		if(readBufferSize > 0)
			this.readBufferSize = readBufferSize;
	}
	
	public void setMaxSendBufferSize(int maxSendBufferSize){
		if(maxSendBufferSize > 0)
			this.maxSendBufferSize = maxSendBufferSize;
	}
	
	/**
	 * 动态调整buf的容量，如果容量足够则返回当前的ByteBuffer，如果不够则扩容后返回新的ByteBuffer
	 * @param bs
	 */
	private ByteBuffer ensureLargeEnough(ByteBuffer buf, byte[] bs){
		if(buf.remaining() < bs.length){
			buf.flip();
			int capacity = buf.capacity() + bs.length * 2;
			ByteBuffer newBuf = ByteBuffer.wrap(new byte[capacity]);
			newBuf.put(buf);
			buf = newBuf;
		}
		return buf;
	}
	
	/**
	 * 从socket读取数据，并使用应用层协议解释器进行数据包解析
	 * @return
	 */
	int read(){
		try{
			ByteBuffer buf = ByteBuffer.wrap(new byte[readBufferSize]);
			int r = channel.read(buf);
			//如果能读到数据
			if(r > 0){
				//成功解析到一个数据包
				buf.flip();
				while(parser.parse(buf)){
					packets.push(parser.take());
					parser.reset();
				}
			}
			return r;
		}catch(IOException e){
			//远程主机强迫关闭了连接
			logger.error("socket[" + hashCode() + "]->" + e.getMessage());
			return -1;
		}catch(Throwable e){
			logger.error("读取socket数据异常", e);
			return -1;
		}
	}
	
	/**
	 * 将写缓冲区中的数据通过socket写出
	 * @param bs
	 * @return
	 */
	boolean write(){
		try{
			synchronized (outBuf) {
				outBuf.flip();
				int n = channel.write(outBuf);
				
				//如果能顺利将写缓冲区中的数据全部输出，复用这个outBuf，并将OP_WRITE事件从reactor中移除
				if(outBuf.remaining() == 0){
					outBuf.clear();
					currReactor.add(this, SelectionKey.OP_READ);
				}
				//如果还没有写完，将数据复制到新的ByteBuffer中
				else{
					int remaining = outBuf.remaining();
					byte[] remainingBytes = new byte[Math.max(DEFAULT_BUFF_SIZE, remaining)];
					outBuf.get(remainingBytes, 0, remaining);
					outBuf = ByteBuffer.wrap(remainingBytes);
					outBuf.position(remaining); //下一次append的时候能找到正确的位置
				}
				return true;
			}
		}catch(IOException e){
			//远程主机强迫关闭了连接
			return false;
		}catch(Throwable e){
			logger.error("写出socket数据异常", e);
			return false;
		}
	}
	
	/**
	 * 将数据存到写缓冲区，并激活reactor的OP_WRITE事件循环进行数据写出
	 * @param bs
	 */
	public boolean send(byte[] bs){
		synchronized (outBuf) {
			//如果超过了写缓冲区最大限制
			if(bs.length + outBuf.position() > maxSendBufferSize){
				logger.info("超过响应报文最大长度，拒绝响应客户端，将断开连接");
				return false;
			}
			outBuf = ensureLargeEnough(outBuf, bs);
			outBuf.put(bs);
			//这个socket需要读写了
			currReactor.add(this, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			return true;
		}
	}
	
	/**
	 * 将socket注册到reactor中进行事件循环
	 * @param reactor
	 * @return
	 */
	boolean register(Reactor reactor, int ops){
		if(channel == null)
			return false;
		
		this.currReactor = reactor;
		//默认只注册读事件
		try{
			channel.register(reactor.selector(), ops, this);
			return true;
		}catch(Exception e){
			logger.error("Reactor.add error", e);
			return false;
		}
	}
	
	/**
	 * 获取内部的socketchannel
	 * @return
	 */
	public SocketChannel javaSocket(){
		return channel;
	}
	
	/**
	 * 关闭socketchannel，同时清空缓冲区
	 */
	public void close(){
		if(channel != null){
			try{channel.close();}catch(Exception e){logger.error("error", e);}
		}
		if(outBuf != null){
			outBuf.clear();
		}
		parser.reset();
	}
	
	/**
	 * 这个niosocket是否处于连接状态
	 * @return
	 */
	public boolean isOpen(){
		return channel!=null && channel.isOpen();
	}
	
	Object popPacket(){
		try{return packets.pop();}catch(NoSuchElementException e){return null;}
	}
}
