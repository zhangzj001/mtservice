package cn.jugame.msg;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import cn.jugame.mt.Buffer;
import cn.jugame.mt.ProtocalParser;
import cn.jugame.util.ByteHelper;

public class MessageProtocalParser implements ProtocalParser{
	
	private Buffer buffer = new Buffer();

	private byte[] content = null;
	private int contentLength = 0;
	
	@Override
	public boolean parse(ByteBuffer buf) {
		buffer.appendByteBuffer(buf);
		
		//读取头部
		if(contentLength == 0 && buffer.unread() >= 4){
			contentLength = ByteHelper.bytesToInt(buffer.read(4));
		}
		
		//读取内容
		if(contentLength != 0 && buffer.unread() >= contentLength){
			content = buffer.read(contentLength);
			return true;
		}
		
		return false;
	}

	@Override
	public Object take() {
		return content;
	}

	@Override
	public void reset() {
		contentLength = 0;
		content = null;
		buffer.reset(true);
	}
	
	/**
	 * 将字节数据转换成消息体数据
	 * @param bytes
	 * @return
	 */
	public static byte[] toMessage(byte[] bytes){
		if(bytes == null)
			return null;
		
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		int len = bytes.length;
		try{
			o.write(ByteHelper.int2ByteArray(len));
			o.write(bytes);
			return o.toByteArray();
		}catch(Exception e){
			return null;
		}
	}
}
