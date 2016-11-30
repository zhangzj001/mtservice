package cn.jugame.msg;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import cn.jugame.mt.ProtocalParseException;
import cn.jugame.mt.ProtocalParser;

public class MessageProtocalParser implements ProtocalParser{

	//数据包头部指定的长度值 
	private ByteBuffer header = ByteBuffer.wrap(new byte[4]);
	private ByteBuffer content = null; //这个要看读到的header指示有多长的数据包才能初始化
	
	private static final int MAX_MESSAGE_SIZE = 4*1024*1024; //一个消息长度到达4M，那已经很夸张了

	@Override
	public boolean parse(ByteBuffer buf) {
		//还没解析完头部
		if(header.remaining() != 0){
			int remaining = header.remaining();
			for(int i=0; i<remaining; ++i){
				try{
					header.put(buf.get());
				}catch(BufferUnderflowException e){
					break;
				}
			}
			//如果头部还没读完
			if(header.remaining() != 0)
				return false;
			//头部读全了，此时可以得到content的长度了，初始化content
			header.flip();
			int len = header.getInt();
			//FIXME 这里需要对len进行长度判断！
			if(len > MAX_MESSAGE_SIZE)
				throw new ProtocalParseException("消息长度过大:" + len);
			content = ByteBuffer.wrap(new byte[len]);
		}
		
		//解析内容部分
		if(content.remaining() > 0){
			int min = Math.min(buf.remaining(), content.remaining());
			byte[] bs = new byte[min];
			buf.get(bs);
			content.put(bs);
		}
		
		return content.remaining() == 0;
	}

	@Override
	public Object take() {
		//还没就绪
		if(content.remaining() != 0)
			return null;
		
		content.flip();
		byte[] bs = new byte[content.limit()];
		content.get(bs);
		return bs;
	}

	@Override
	public void reset() {
		header = ByteBuffer.wrap(new byte[4]);
		content = null;
	}
}
