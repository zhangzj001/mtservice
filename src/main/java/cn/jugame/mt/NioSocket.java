package cn.jugame.mt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioSocket {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/*默认缓冲区大小*/
	private static final int DEFAULT_BUFF_SIZE = 4096; //4k

	//写缓冲区
	private Buffer outBuf;
	//协议解释器（由解释器提供读缓冲）
	private ProtocalParser parser;
	//应用层数据包
	private LinkedList<Object> packets = new LinkedList<>();
	
	private Reactor currReactor;
	private SocketChannel channel;
	
	//最大写缓冲
	private int maxSendBufferSize;
	private int readBufferSize;
	
	//附件 -- 这招特别好用！
	private Object attachment;
	
	public NioSocket(SocketChannel channel, ProtocalParserFactory factory) {
		this.channel = channel;
		//构建第一个parser
		this.parser = factory.create();
		
		//默认使用socket的读缓冲大小
		readBufferSize = DEFAULT_BUFF_SIZE;
		maxSendBufferSize = DEFAULT_BUFF_SIZE;
		outBuf = new Buffer(DEFAULT_BUFF_SIZE);
	}

	public Object getAttachment() {
		return attachment;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	public void setReadBufferSize(int readBufferSize){
		if(readBufferSize > 0)
			this.readBufferSize = readBufferSize;
	}
	
	public void setMaxSendBufferSize(int maxSendBufferSize){
		if(maxSendBufferSize > 0)
			this.maxSendBufferSize = maxSendBufferSize;
	}
	
	public SocketChannel javaSocket(){
		return this.channel;
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
			logger.error("socket[" + hashCode() + "]", e);
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
				int curr = outBuf.getReadPosition();
				int n = channel.write(ByteBuffer.wrap(outBuf.getUnreadBytes()));
				outBuf.setReadPosition(curr + n);
				//如果能顺利将写缓冲区中的数据全部输出，复用这个outBuf，并将OP_WRITE事件从reactor中移除
				//如果还没有写完，那就等下一次write的时候将outBuf继续输出
				if(outBuf.unread() == 0){
					outBuf.reset(true);
					currReactor.add(this, SelectionKey.OP_READ);
				}
				return true;
			}
		}catch(IOException e){
			//远程主机强迫关闭了连接
			logger.error("对端主机已关闭，无法写数据: " + e.getMessage());
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
			if(bs.length > maxSendBufferSize){
				logger.info("超过响应报文最大长度，拒绝响应客户端，将断开连接");
				return false;
			}
			outBuf.appendBytes(bs);
			//这个socket需要支持写事件了
			return currReactor.add(this, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
		}catch(ClosedChannelException e){
			//对端主机关闭了channel，注册失败
			logger.debug("socket已关闭，无法注册事件");
			return false;
		}catch(Exception e){
			logger.error("Reactor.add error", e);
			return false;
		}
	}
	
	/**
	 * 关闭socketchannel，同时清空缓冲区<br>
	 * 如果是为了释放资源，请调用Context.releaseSocket来执行，那样才能完美地释放所有相关资源!
	 */
	void close(){
		if(channel != null){
			try{channel.close();}catch(Exception e){logger.error("error", e);}
		}
		if(outBuf != null){
			outBuf.reset(false);
		}
		if(parser != null){
			parser.reset();
			parser = null;
		}
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
