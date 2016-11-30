package cn.jugame.mt.sm;

import cn.jugame.mt.NioSocket;

public interface SocketManager {
	/**
	 * 添加socket管理，如果容量已满，将根据策略弹出一个socket。
	 * @param socket
	 * @return
	 */
	public NioSocket add(NioSocket socket);
	
	/**
	 * 从管理器中移除socket
	 * @param socket
	 */
	public void remove(NioSocket socket);
	
	/**
	 * 当前被托管socket的总数
	 * @return
	 */
	public int size();
	
}
