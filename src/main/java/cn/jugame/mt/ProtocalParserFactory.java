package cn.jugame.mt;

public interface ProtocalParserFactory {
	/**
	 * 构建一个ProtocalParser实例。<br>
	 * 该方法最终将为每个NioSocket产生一个独立的协议解析器，为了避免socket之间解析的同步问题，请务必每次产生一个新实例!
	 * @return
	 */
	public ProtocalParser create();
}
