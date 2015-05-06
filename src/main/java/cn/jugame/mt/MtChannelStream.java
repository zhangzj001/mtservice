package cn.jugame.mt;

import java.nio.channels.SocketChannel;

public interface MtChannelStream {

	public MtPackage read_channel(SocketChannel channel);
	
	public boolean write_channel(SocketChannel channel, MtPackage resp);
	
	public boolean close_channel(SocketChannel channel);
}
