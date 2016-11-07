package cn.jugame.channel;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.MtChannelStream;
import cn.jugame.mt.MtPackage;
import cn.jugame.util.ByteHelper;
import cn.jugame.util.JuConfig;

public class JobChannelStream implements MtChannelStream{
	
	private static Logger logger = LoggerFactory.getLogger(JobChannelStream.class);
	
	//selector读超时时间
	private int so_timeout = JuConfig.getValueInt("so_timeout") * 1000;
	
	//-------------------------------------------------------------------------------------
	//写数据
	private boolean write_channel(SocketChannel channel, ByteBuffer buffer){
		Selector selector = null;
		
		try{
			selector = Selector.open();
			channel.register(selector, SelectionKey.OP_WRITE);
			
			while(selector.select(so_timeout) > 0){
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				if(it.hasNext()){
					SelectionKey readyKey = it.next();
					it.remove();
					
					int size = 0;
					while(buffer.remaining() > 0 && (size = channel.write(buffer)) > 0);
					
					//读满了数据了，这时候就可以不再等待了
					if(buffer.remaining() == 0){
						break;
					}
				}
			}
			
			return buffer.remaining()==0;
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			return false;
		}finally{
			try{
				if(selector != null)
					selector.close();
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public boolean write_channel(SocketChannel channel, MtPackage resp){
		try{
			byte[] bs = resp.getData();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] len_bytes = ByteHelper.int2ByteArray(bs.length);
			baos.write(len_bytes);
			baos.write(bs);
			
			bs = baos.toByteArray();
			ByteBuffer buf = ByteBuffer.wrap(new byte[bs.length]);
			buf.put(bs);
			buf.flip();
			
			//这里写也有可能返回size=0
			if(!write_channel(channel, buf))
				return false;
			
			return true;
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			return false;
		}
	}
	
	//------------------------------------------------------------------------------
	//读数据
	private byte[] read_channel(Selector selector, SocketChannel channel, int len) throws Exception{
		if(len == 0)
			return null;
		
		ByteBuffer buffer = ByteBuffer.wrap(new byte[len]);
        
        //一直把buffer读满为止
        while(selector.select(so_timeout) > 0){
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			if(it.hasNext()){
				SelectionKey readyKey = it.next();
				it.remove();
				
				int size = 0;
				while(buffer.remaining() > 0 && (size = channel.read(buffer)) > 0);
				
				//读满了数据了，这时候就可以不再等待了
				if(buffer.remaining() == 0){
					break;
				}

				//遇到客户端关socket
				if(size == -1){
					return null;
				}
			}
		}
    	
        //把数据倒出来
        buffer.flip();
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        
        return bytes;
	}
	
	@Override
	public MtPackage read_channel(SocketChannel channel){
		Selector selector = null;
		try{
			selector = Selector.open();
			channel.register(selector, SelectionKey.OP_READ);
			
			//头4个字节代表长度
			byte[] len_bytes = read_channel(selector, channel, 4);
			if(len_bytes == null)
				return null;
			
			int len = ByteHelper.bytesToInt(len_bytes);
			if(len <= 0)
				return null;
			
			byte[] data = read_channel(selector, channel, len);
			if(data == null)
				return null;

			return new JobPackage(data);
		}catch(java.nio.channels.ClosedChannelException e){
        	//这种客户端自己断开连接导致读错误的情况就忽略吧
        	logger.info(e.getMessage());
        	return null;
        }catch(java.io.IOException e){
        	//这种客户端自己断开连接导致读错误的情况就忽略吧
        	logger.info(e.getMessage());
        	return null;
        }catch(Exception e){
			logger.error(e.getMessage(), e);
			return null;
		}finally{
			if(selector != null){
				try{
					selector.close();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public boolean close_channel(SocketChannel channel) {
		try{
			channel.close();
			return true;
		}catch(Exception e){
			return false;
		}
	}

}
