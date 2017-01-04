package cn.jugame.fs;

import java.io.File;

/**
 * 设定inode文件最大约为32M，vnode文件最大约为4Gb。<br>
 * 每个inodefile头部为：4字节。每个inode节点恒定占用8字节，那么文件最大容量约为32Mb/8=4M个节点<br>
 * 假设每个vnode数据大小为1kb，则4M个vnode节点大小为4Gb。<br>
 * 这里最大容量是基于估算的，实际情况可能因为每个vnode节点大小不一样有所不同，但一个inode最多关联16个vnode文件，那么此时平均每个vnode的大小为16kb<br>
 * 
 * 每个inodefile的头部组成如下：<br>
 * 2. 版本号，4位
 * 3. 关联的vnode文件数量，4位，最大值为16
 * 4. 当前文件inode读指针偏移，3字节，但其最大值为4M
 * 
 * @author zimT_T
 *
 */
public class INodeFile {
	
	public static final int MAX_SIZE = 4*1024*1024; //4M个节点，每个8字节
	
	public static INodeFile open(String nodeName){
		try{
			FileAccessor accessor = new FileAccessor(new File(nodeName + ".inode"));
			//文件不存在就创建
			if(accessor.length() == 0){
				accessor.writeInt(0x10000000);//版本号=1，关联vnode文件数=0，inode读指针偏移=0
			}
			
			return new INodeFile(accessor, nodeName);
		}catch(Exception e){
			return null;
		}
	}
	
	private class Header{
		int version = 0;
		int associateVnodeFileCount = 0;
		int offset = 0;
		
		public int serialize(){
			return 0xFFFFFFFF & (version << 28) & (associateVnodeFileCount << 24) & offset;
		}
		
		public void from(int head){
			this.version = (head & 0xF0000000) >> 28;
			this.associateVnodeFileCount = ((head & 0x0F000000) >> 24) + 1;
			this.offset = (head & 0x00FFFFFF);
		}
	}
	
	private FileAccessor accessor;
	private String nodeName;
	private Header header = new Header();
	
	private INodeFile(FileAccessor accessor, String nodeName){
		this.accessor = accessor;
		this.nodeName = nodeName;
		parseHeader();
	}
	
	/**
	 * 解析inodefile的头部数据
	 * @return
	 */
	private boolean parseHeader(){
		if(!accessor.seekFront())
			return false;
		if(accessor.length() < 4)
			return false;
		int head = accessor.readInt();
		if(head == -1)
			return false;
		
		header.from(head);
		return true;
	}

	/**
	 * 更新头部信息
	 * @return
	 */
	private boolean updateHeader(){
		if(!accessor.seekFront())
			return false;
		return accessor.writeInt(header.serialize());
	}
	
	public int version(){
		return header.version;
	}
	
	public int vnodeFileCount(){
		return header.associateVnodeFileCount;
	}
	
	public int offset(){
		return header.offset;
	}
	
	public long size(){
		//可以通过文件大小除以inode大小得到
		long len = accessor.length() - 4;
		return len / INode.sizeof();
	}
	
	public INode get(long idx){
		if(idx < 0)
			return null;
		long offset = 4 + INode.sizeof() * idx;
		//读不够8个字节
		if(offset + 8 > accessor.length())
			return null;
		accessor.seek(offset);
		
		//读取8个字节的INode数据出来
		byte[] bytes = new byte[8];
		accessor.readFully(bytes);
		return new INode(bytes, nodeName);
	}
	
	public void close(){
		if(accessor != null){
			accessor.close();
		}
	}
	
	public boolean isFull(){
		return size() > MAX_SIZE;
	}
	
	public boolean add(INode node){
		if(!accessor.seekEnd())
			return false;
		return accessor.writeLong(node.value());
	}
	
	public boolean associateNewVnodeFile(){
		//如果可关联的vnodefile数量满了
		if(header.associateVnodeFileCount >= 16)
			return false;
		
		++header.associateVnodeFileCount;
		
		//不仅需要新建vnodefile，还需要刷到inodefile的头部
		return updateHeader();
	}
	
	/**
	 * 标记当前读取inode的偏移索引值。该值的合法范围是一个0到size()的整数，当值为size()时，下一个读取的数据将会是null
	 * @param offset
	 * @return
	 */
	public boolean markOffset(int offset){
		if(offset > size())
			return false;
		header.offset = offset;
		return updateHeader();
	}
}
