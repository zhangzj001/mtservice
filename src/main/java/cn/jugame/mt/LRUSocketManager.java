package cn.jugame.mt;

import java.util.Iterator;
import java.util.LinkedHashSet;

class LRUSocketManager {
	private int capacity;
	public LRUSocketManager(int capacity){
		this.capacity = capacity;
	}
	
	private LinkedHashSet<NioSocket> channels = new LinkedHashSet<NioSocket>();
	
	public NioSocket add(NioSocket socket){
		synchronized (this) {
			NioSocket old_channel = null;
			//如果超过最大容量，移除开头的channel
			if(channels.size() >= capacity){
				Iterator<NioSocket> it = channels.iterator();
				if(it.hasNext()){
					old_channel = it.next();
					it.remove();
				}
			}
			
			//追加这个channel
			channels.add(socket);
			
			return old_channel;
		}
	}
	
	public void update(NioSocket socket){
		synchronized (this) {
			if(channels.contains(socket)){
				channels.remove(socket);
				channels.add(socket);
			}
		}
	}
	
	public void remove(NioSocket socket){
		synchronized (this) {
			channels.remove(socket);
		}
	}
	
	public int size(){
		return channels.size();
	}
}
