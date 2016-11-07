package cn.jugame.channel;


import cn.jugame.mt.MtPackage;

/**
 * im服务使用的数据包
 * @author zimT_T
 *
 */
public class JobPackage implements MtPackage{

	private byte[] bs;
	public JobPackage(byte[] bs){
		this.bs = bs;
	}
	
	@Override
	public byte[] getData() {
		return bs;
	}

	@Override
	public boolean isReady() {
		return true;
	}
	
}
