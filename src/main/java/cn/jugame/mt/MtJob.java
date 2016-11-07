package cn.jugame.mt;

import java.nio.channels.SocketChannel;

public interface MtJob {
	/**
	 * 接收到用户数据，并进行处理
	 * @param channel
	 * @param bs
	 * @return 返回true表示维持用户连接，false表示方法执行完便主动断开用户连接
	 */
	public boolean do_job(SocketChannel channel, MtPackage bs);
	
	/**
	 * 关闭channel前做点事情吧
	 * @param channel
	 */
	public void before_close_channel(SocketChannel channel);
	
}
