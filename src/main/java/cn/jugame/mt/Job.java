package cn.jugame.mt;

import java.nio.channels.SocketChannel;

public interface Job {
	/**
	 * 接收到用户数据，并进行处理
	 * @param channel
	 * @param packet
	 * @return 返回true表示处理成功，此时服务端将维持连接，返回false表示任务处理失败，此时服务端会主动断开客户端连接
	 */
	public boolean doJob(NioSocket socket, Object packet);
	
	/**
	 * 关闭channel前做点事情吧
	 * @param channel
	 */
	public void beforeCloseSocket(NioSocket socket);
}
