package cn.jugame.mt;

import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * socketchannel的管理采用LRU策略，这主要是为了避免客户端宕机或者有什么乱七八糟的情况，导致socket关闭了但是没有fin包过来，导致服务端不知道对端是否关闭的情况发生。
 * 有了LRU的管理之后，客户端心跳就显得不是那么必要了，尤其是当针对移动终端应用时，可以避免心跳带来的额外带宽开销和电量耗损。
 * 
 * 这里使用了一个LinkedHashSet来存放所有的channel，当某个channel有读写的时候，将此channel移到链表末尾。
 * 也即是说，越不经常使用的channel将会越靠近表头。当新增新的channel加入时，如果超过了最大容量，此时将表头的channel移除。
 * @author zimT_T
 *
 */
public class LRUChannelManager {
	private int capacity;
	public LRUChannelManager(int capacity){
		this.capacity = capacity;
	}
	
	private LinkedHashSet<SocketChannel> channels = new LinkedHashSet<SocketChannel>();
	
	public SocketChannel add_channel(SocketChannel channel){
		synchronized (this) {
			SocketChannel old_channel = null;
			//如果超过最大容量，移除开头的channel
			if(channels.size() >= capacity){
				Iterator<SocketChannel> it = channels.iterator();
				if(it.hasNext()){
					old_channel = it.next();
					it.remove();
				}
			}
			
			//追加这个channel
			channels.add(channel);
			
			return old_channel;
		}
	}
	
	public void update_channel(SocketChannel channel){
		synchronized (this) {
			if(channels.contains(channel)){
				channels.remove(channel);
				channels.add(channel);
			}
		}
	}
	
	public void remove_channel(SocketChannel channel){
		synchronized (this) {
			channels.remove(channel);
		}
	}
	
	public int size(){
		return channels.size();
	}
	
}
