package cn.jugame.mt;

import java.io.ByteArrayOutputStream;

public class Buffer {
	
	//用来存储数据流的一个容器，源源不断地往这个容器里塞数据，同时再触发解析动作
	private final static int DEFAULT_CAPACITY = 1024;
	
	/*虽然是拿ByteBuffer来做缓冲区，但是并没有正确使用其中的position值，只有limit是对的！*/
	private byte[] inBuf = new byte[DEFAULT_CAPACITY];
	private int position = 0; //当前读取指针位置
	private int limit = 0; //当前数据填充满的位置
	private int capacity = inBuf.length; //当前容量大小
	
	/**
	 * 未读字节数量
	 * @return
	 */
	public int unread(){
		return limit - position;
	}
	
	/**
	 * 未用字节数量
	 * @return
	 */
	public int unuse(){
		return capacity - limit;
	}
	
	/**
	 * 追加字节到缓冲区
	 * @param bs
	 */
	public void appendBytes(byte[] bs){
		int unUse = unuse();
		//空闲的空间不足以容纳这些字节，则要动态扩容
		if(unUse < bs.length){
			capacity = inBuf.length + bs.length*2;
			byte[] newBytes = new byte[capacity];
			System.arraycopy(inBuf, 0, newBytes, 0, inBuf.length);
		}
		
		//从limit这个位置开始追加数据
		System.arraycopy(bs, 0, inBuf, limit, bs.length);
		limit += bs.length;
	}
	
	private byte readByte(ByteArrayOutputStream out){
		try{
			byte b = inBuf[position++];
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
			System.arraycopy(inBuf, position, bytes, 0, length);
			position += length;
			return bytes;
		}
		return null;
	}
	
	public byte[] getUnreadBytes(){
		int n = unread();
		if(n == 0)
			return new byte[0];
		byte[] bs = new byte[n];
		System.arraycopy(inBuf, position, bs, 0, n);
		return bs;
	}
	
	public void reset(){
		inBuf = new byte[DEFAULT_CAPACITY];
	}
}
