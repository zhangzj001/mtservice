package cn.jugame.fs;

import cn.jugame.util.ByteHelper;

/**
 * 每个inode的组成如下：<br>
 * 1. 头2位为占位，固定为00<br>
 * 2. 是否压缩，1位<br>
 * 3. 是否M1加密，1位<br>
 * 4. vnode文件路径索引，4位<br>
 * 5. 数据长度，3字节，最大值为16Mb<br>
 * 6. 数据偏移，4字节<br>
 * 
 * @author zimT_T
 *
 */
public class INode {
	
	public static INode create(boolean compressed, boolean encrypted, int associateVnodeFileNumber, long dataLength, long dataOffset, String nodeName){
		//作一些简单的判断
		if(associateVnodeFileNumber > 16 || associateVnodeFileNumber <= 0)
			return null;
		if(dataLength < 0 || dataOffset < 0)
			return null;
		
		long d1 = ((compressed ? 0x20 : 0x00) | (encrypted ? 0x10 : 0x00) | associateVnodeFileNumber) << 24;
		d1 |= dataLength;
		long d2 = dataOffset;
		
		long d = (d1<<32) | d2;
		return new INode(ByteHelper.long2ByteArray(d), nodeName);
	}
	
	private byte[] bytes;
	private String nodeName;
	INode(byte[] bytes, String nodeName){
		this.bytes = bytes;
		this.nodeName = nodeName;
	}
	
	public long value(){
		return ByteHelper.bytesToLong(bytes, 0);
	}
	
	/**
	 * INode节点占用的空间大小，8字节
	 * @return
	 */
	public static int sizeof(){
		return 8;
	}
	
	public boolean isCompressed(){
		return (bytes[0] & 0x20) > 0;
	}
	
	public boolean isEncrypted(){
		return (bytes[0] & 0x10) > 0;
	}
	
	public int vnodeNumber(){
		return bytes[0] & 0x0F;
	}
	
	public int getDataLength(){
		return (bytes[1] << 16) + (bytes[2] << 8) + bytes[3];
	}
	
	public int getDataOffset(){
		return (bytes[4] << 24) + (bytes[5] << 16) + (bytes[6] << 8) + bytes[7];
	}
	
	@Override
	public String toString() {
		return ByteHelper.bytesToHexString(bytes);
	}
}
