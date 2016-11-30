package cn.jugame.mt;

public class ServiceConfig {
	private int maxSendBufferSize = -1;
	private int readBufferSize = -1;
	private int soTimeout = 10 * 1000; //默认10s
	
	public int getMaxSendBufferSize() {
		return maxSendBufferSize;
	}
	public void setMaxSendBufferSize(int maxSendBufferSize) {
		this.maxSendBufferSize = maxSendBufferSize;
	}
	public int getReadBufferSize() {
		return readBufferSize;
	}
	public void setReadBufferSize(int readBufferSize) {
		this.readBufferSize = readBufferSize;
	}
	public int getSoTimeout() {
		return soTimeout;
	}
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}
	
}
