package cn.jugame.mt;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class Buffer {
	
	//用来存储数据流的一个容器，源源不断地往这个容器里塞数据，同时再触发解析动作
	private final static int DEFAULT_CAPACITY = 1024;
	
	/*虽然是拿ByteBuffer来做缓冲区，但是并没有正确使用其中的position值，只有limit是对的！*/
	private byte[] inBuf;
	private int rPosition = 0; //当前读取指针位置
	private int limit = -1; //当前数据填充满的位置，也是读指针能到达的最后一个位置
	private int capacity; //当前容量大小
	
	public Buffer(){
		inBuf = new byte[DEFAULT_CAPACITY];
		capacity = inBuf.length;
	}
	
	public Buffer(int initCapacity){
		if(initCapacity <= 0)
			initCapacity = DEFAULT_CAPACITY;
		inBuf = new byte[initCapacity];
		capacity = inBuf.length;
	}
	
	private void extendBuffer(int length){
		int unUse = unuse();
		//空闲的空间不足以容纳这些字节，则要动态扩容
		if(unUse < length){
			capacity = inBuf.length + length*2;
			byte[] newBytes = new byte[capacity];
			System.arraycopy(inBuf, 0, newBytes, 0, inBuf.length);
			inBuf = newBytes;
		}
	}
	
	public void appendByteBuffer(ByteBuffer buf){
		int length = buf.remaining();
		extendBuffer(length);
		buf.get(inBuf, limit+1, length);
		limit += length;
	}
	
	/**
	 * 设置已读位置
	 * @param rPosition
	 */
	public void setReadPosition(int rPosition){
		if(this.rPosition > limit+1)
			throw new BufferOverflowException();
		this.rPosition = rPosition;
	}
	
	/**
	 * 获取当前读指针位置
	 */
	public int getReadPosition(){
		return rPosition;
	}
	
	/**
	 * 未读字节数量
	 * @return
	 */
	public int unread(){
		return limit - rPosition + 1;
	}
	
	/**
	 * 未用字节数量
	 * @return
	 */
	public int unuse(){
		return capacity - limit - 1;
	}
	
	/**
	 * 追加字节到缓冲区
	 * @param bs
	 */
	public void appendBytes(byte[] bs){
		extendBuffer(bs.length);
		
		//从limit这个位置开始追加数据
		System.arraycopy(bs, 0, inBuf, limit+1, bs.length);
		limit += bs.length;
	}
	
	private byte readByte(ByteArrayOutputStream out){
		if(unread() == 0)
			return -1;
		try{
			byte b = inBuf[rPosition++];
			out.write(b);
			return b;
		}catch(IndexOutOfBoundsException e){
			return -1;
		}
	}
	
	public byte[] readLineBytes(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		byte b = -1;
		while((b = readByte(out)) != -1){
			if(b == '\r' && readByte(out) == '\n'){
				break;
			}
		}
		//如果b==-1，说明没有读到完整的一行
		if(b == -1)
			return null;
		
		return out.toByteArray();
	}

	public String readLine(){
		byte[] bs = readLineBytes();
		if(bs == null)
			return null;
		try{
			return new String(bs, "UTF-8").trim();
		}catch(Exception e){
			return null;
		}
	}
	
	public byte[] read(int length){
		int unread = unread();
		if(unread >= length){
			byte[] bytes = new byte[length];
			System.arraycopy(inBuf, rPosition, bytes, 0, length);
			rPosition += length;
			return bytes;
		}
		return null;
	}
	
	public byte[] getUnreadBytes(){
		int n = unread();
		if(n == 0)
			return new byte[0];
		byte[] bs = new byte[n];
		System.arraycopy(inBuf, rPosition, bs, 0, n);
		return bs;
	}
	
	public void reset(boolean keepUnread){
		//还有未读数据，将这些数据复制到前面
		int n = unread();
		if(keepUnread && n > 0){
			System.arraycopy(inBuf, rPosition, inBuf, 0, n);
		}
		rPosition = 0;
		limit = keepUnread ? n-1 : -1;
	}
}
