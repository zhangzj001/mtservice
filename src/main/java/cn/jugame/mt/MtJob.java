package cn.jugame.mt;

import java.nio.channels.SocketChannel;

public interface MtJob {
	/**
	 * 接收到用户数据，并进行处理
	 * @param channel
	 * @param bs
	 * @return
	 */
	public boolean do_job(SocketChannel channel, MtPackage bs);
	
	/**
	 * 关闭channel前做点事情吧
	 * @param channel
	 */
	public void before_close_channel(SocketChannel channel);
	
}
