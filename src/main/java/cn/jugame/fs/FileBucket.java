package cn.jugame.fs;

import java.util.ArrayList;
import java.util.List;

public class FileBucket {
	private INodeFile inodeFile;
	private VNodeFile[] vnodeFiles = new VNodeFile[16];
	private String nodeName;
	public FileBucket(String nodeName){
		inodeFile = INodeFile.open(nodeName);
		this.nodeName = nodeName;
	}
	
	public INodeFile getINodeFile(){
		return inodeFile;
	}
	
	public VNodeFile[] getVNodeFiles(){
		if(inodeFile == null)
			return new VNodeFile[0];
		VNodeFile[] files = new VNodeFile[inodeFile.vnodeFileCount()];
		for(int i=0; i<inodeFile.vnodeFileCount(); ++i){
			files[i] = vnodeFiles[i];
		}
		return files;
	}
	
	/**
	 * 根据序号打开vnodefile，如果该vnodefile已经打开则从缓存中获取
	 * @param number 序号，从1到16
	 * @return
	 */
	private VNodeFile openVNodeFile(int number){
		if(number > vnodeFiles.length || number <= 0)
			return null;
		VNodeFile vnodeFile = vnodeFiles[number-1];
		if(vnodeFile == null){
			vnodeFile = VNodeFile.open(nodeName, number);
			vnodeFiles[number-1] = vnodeFile;
		}
		return vnodeFile;
	}
	
	/**
	 * 是否已满。inodefile满了或者关联的16个vnodefile都满了，就返回true
	 * @return
	 */
	public boolean isFull(long length){
		if(inodeFile.isFull())
			return true;
		
		//看最后一个VNodeFile是否满了，如果满了就是满了
		VNodeFile lastVNodeFile = null;
		for(VNodeFile vnodeFile : vnodeFiles){
			if(vnodeFile != null)
				lastVNodeFile = vnodeFile;
		}
		if(lastVNodeFile != null && lastVNodeFile.isFull(length))
			return true;
		
		return false;
	}
	
	/**
	 * 追加数据<br>
	 * 如果超过了inodefile的最大容量，将导致追加失败，否则将尝试在关联的最后一个vnodefile中追加数据，如果超过了vnodefile的容量，将自动创建一个新的vnodefile关联进来并
	 * 进行数据追加。如果创建的vnodefile超过了上限（16个），也同样会导致追加数据失败。
	 * @param bs
	 * @return
	 */
	public long add(byte[] bs){
		//这个inodefile满了
		if(inodeFile.isFull())
			return -1;
		
		//在最后一个VNodeFile中追加
		VNodeFile vnodeFile = openVNodeFile(inodeFile.vnodeFileCount());
		//如果vnodefile满了，尝试创建新的vnodefile
		if(vnodeFile.isFull(bs.length)){
			if(!inodeFile.associateNewVnodeFile())
				return -1;
			vnodeFile = openVNodeFile(inodeFile.vnodeFileCount());
		}
		if(vnodeFile == null)
			return -1;
		
		//追加数据到vnodefile中
		long offset = vnodeFile.length();
		if(offset == -1)
			return -1;
		if(!vnodeFile.add(bs))
			return -1;
		//添加inode
		INode inode = INode.create(false, false, inodeFile.vnodeFileCount(), bs.length, offset, nodeName);
		if(inode == null)
			return -1;
		if(!inodeFile.add(inode))
			return -1;
		return inodeFile.size() - 1;
	}
	
	/**
	 * 获取第index个插入的数据
	 * @param index
	 * @return
	 */
	public byte[] get(long index){
		INode inode = inodeFile.get(index);
		if(inode == null)
			return null;
		VNodeFile vnodeFile = openVNodeFile(inode.vnodeNumber());
		return vnodeFile.get(inode.getDataOffset(), inode.getDataLength());
	}
	
	/**
	 * 当前filebucket中数据的数量
	 * @return
	 */
	public long size(){
		return inodeFile.size();
	}
	
	/**
	 * 释放这个filebucket关联的所有资源
	 */
	public void close(){
		if(inodeFile != null){
			inodeFile.close();
		}
		for(VNodeFile vnodeFile : vnodeFiles){
			if(vnodeFile != null)
				vnodeFile.close();
		}
	}
	
	/**
	 * 获取这个bucket的名称
	 * @return
	 */
	public String getNodeName(){
		return this.nodeName;
	}
}
