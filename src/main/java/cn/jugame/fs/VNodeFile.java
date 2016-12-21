package cn.jugame.fs;

import java.io.File;

public class VNodeFile {
	
	public static final long MAX_SIZE = 4*1024*1024*1024L; //4G大小
	
	public static VNodeFile open(String nodeName, int number){
		try{
			FileAccessor accessor = new FileAccessor(new File(nodeName + ".vnode." + number));
			return new VNodeFile(accessor, nodeName);
		}catch(Exception e){
			return null;
		}
	}
	
	private String nodeName;
	private FileAccessor accessor;
	
	private VNodeFile(FileAccessor accessor, String nodeName){
		this.accessor = accessor;
		this.nodeName = nodeName;
	}
	
	public void close(){
		if(accessor != null)
			accessor.close();
	}
	
	public long length(){
		return accessor.length();
	}
	
	/**
	 * vnodefile是否还装得下length个字节
	 * @param length
	 * @return
	 */
	public boolean isFull(long length){
		long offset = accessor.length();
		if(offset < 0)
			return false;
		return (offset + length) > MAX_SIZE;
	}
	
	/**
	 * 在vnodefile中追加数据
	 * @param bs
	 * @return
	 */
	public boolean add(byte[] bs){
		if(!accessor.seekEnd())
			return false;
		return accessor.write(bs);
	}
	
	/**
	 * 获取数据
	 * @param offset
	 * @param length
	 * @return
	 */
	public byte[] get(int offset, int length){
		//超长了
		if(offset + length > accessor.length())
			return null;
		
		byte[] bs = new byte[length];
		if(!accessor.seek(offset))
			return null;
		if(!accessor.readFully(bs))
			return null;
		return bs;
	}
}
