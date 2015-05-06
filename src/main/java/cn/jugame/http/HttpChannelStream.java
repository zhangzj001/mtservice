package cn.jugame.http;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import cn.jugame.mt.MtChannelStream;
import cn.jugame.mt.MtPackage;

public class HttpChannelStream implements MtChannelStream{
	
	//selector读超时时间
	private int so_timeout = 3000;
	
	/**
	 * 构造器
	 * @param so_timeout 读超时秒数
	 */
	public HttpChannelStream(int so_timeout){
		this.so_timeout = so_timeout * 1000;
	}

	/**
	 * 从channel中读取数据，对于http协议，需要一边读取一边解析。
	 */
	@Override
	public MtPackage read_channel(SocketChannel channel) {
		HttpRequest request = new HttpRequest();
		HttpParser parser = new HttpParser();
		
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		Selector selector = null;
		
		try{
			selector = Selector.open();
			channel.register(selector, SelectionKey.OP_READ);
			
			int n = 0;
			while((n = selector.select(so_timeout)) > 0){
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				if(it.hasNext()){
					SelectionKey readyKey = it.next();
					it.remove();
					
					int size = channel.read(buffer);
					//遇到客户端关socket
					if(size == -1){
						return null;
					}
					
					//得到数据
					buffer.flip();
			        byte[] bytes = new byte[buffer.limit()];
			        buffer.get(bytes);
			        buffer.clear();
			        
					//进行http格式解析
			        if(parser.cycle_parse(request, bytes)){
			        	break;
			        }
				}
			}
			
			//慢读的情况，果断不鸟这个socket了 
			if(n == 0)
				return null;

			return request;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}finally{
			if(selector != null){
				try{
					selector.close();
				}catch(Exception e){e.printStackTrace();}
			}
		}
	}
	
	private boolean sync_write(SocketChannel channel, ByteBuffer buffer){
		Selector selector = null;
		
		try{
			selector = Selector.open();
			channel.register(selector, SelectionKey.OP_WRITE);
			
			while(selector.select() > 0){
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				if(it.hasNext()){
					SelectionKey readyKey = it.next();
					it.remove();
					
					int size = 0;
					while(buffer.remaining() > 0 && (size = channel.write(buffer)) > 0);
					
					//读满了数据了，这时候就可以不再等待了
					if(buffer.remaining() == 0)
						break;
				}
			}
	
			return buffer.remaining()==0;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}finally{
			try{
				if(selector != null)
					selector.close();
			}catch(Exception e){e.printStackTrace();}
		}
	}
	
	@Override
	public boolean write_channel(SocketChannel channel, MtPackage pack) {
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(pack.getData());
			byte[] bs = baos.toByteArray();
			
			ByteBuffer buf = ByteBuffer.allocate(bs.length);
			buf.put(bs);
			buf.flip();
			
			//这里写也有可能返回size=0
			if(!sync_write(channel, buf))
				return false;
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean close_channel(SocketChannel channel) {
		try{
			channel.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}

}
