package cn.jugame.mt;

import java.nio.ByteBuffer;

/**
 * 应用层协议解析器<br>
 * 每个协议解析器实例最多只能解析得到一个协议包，即调用parse成功之后可以通过take方法取得协议包，再通过reset方法重置后重新再进行解析。
 * @author zimT_T
 *
 */
public interface ProtocalParser {
	/**
	 * 递进式地进行数据解析，直到能够成功解析一个完整的协议包时，返回true<br>
	 * 当该方法返回true之后，重复调用该方法将总是得到true，且已经无法再继续进行解析了
	 * @param buf 数据缓冲区 
	 * @return 成功解析到一个应用层数据，返回true，否则false
	 */
	public boolean parse(ByteBuffer buf);
	
	/**
	 * 在parse方法成功解析到一个数据包时，调用该方法能够取得这个数据包。<br>
	 * 如果还没有成功得到一个数据包，该方法将返回null
	 * @return
	 */
	public Object take();
	
	/**
	 * 重置解释器的状态，重置之后再调用parse将从头开始解析
	 */
	public void reset();
}
