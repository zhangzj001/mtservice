package cn.jugame.fs;

import org.apache.commons.lang.StringUtils;

import cn.jugame.util.ByteHelper;

/**
 * 数据存储器。<br>
 * 存储器由64个FileBucket组成，每个FileBucket可以容纳最多4M个节点或者最大64G数据（看哪个值先达到上限）。所以
 * 一个存储器的最大容量是 256M 个节点或者 4T 的数据，看哪个值先达到上限。<br>
 * 当放入数据成功之后可以得到一个令牌，以后需要取这个数据则需要通过令牌来索引并取得。<br>
 * <b>FIXME 我本来想拿这个来做消息数据的落盘，但是又面临着离线消息的存储问题，离线消息必须存储成一个FIFO队列</b>
 * @author zimT_T
 *
 */
public class Luggage {
	
	private FileBucket[] buckets = new FileBucket[64];
	private String name;
	public Luggage(String name){
		this.name = name;
	}
	
	private int currentBucketIndex = 0;
	
	private FileBucket getCurrentBucket(){
		if(currentBucketIndex >= buckets.length)
			return null;
		
		FileBucket bucket = buckets[currentBucketIndex];
		//没有bucket就新建一个来用
		if(bucket == null){
			bucket = buckets[currentBucketIndex] = new FileBucket(name + "_" + currentBucketIndex);
		}
		return bucket;
	}
	
	/**
	 * 获取一个filebucket用于存储数据，如果容量不够了则新建一个。判断容量是否需要传入参数length，用于表明准备存储的数据大小。
	 * @param length
	 * @return
	 */
	private FileBucket getBucket(long length){
		//获取一个可用的bucket，如果满了就换下一个，直到能容纳下数据为止。
		FileBucket bucket = null;
		while((bucket = getCurrentBucket()) != null && bucket.isFull(length)){
			bucket.close();
			currentBucketIndex++;
		}
		return bucket;
	}
	
	public Token saveData(byte[] bs){
		FileBucket bucket = getBucket(bs.length);
		long index = bucket.add(bs);
		if(index == -1)
			return null;
		
		Token token = new Token();
		token.setNodeName(bucket.getNodeName());
		token.setIndex(index);
		return token;
	}
	
	public byte[] getData(Token token){
		if(token == null || StringUtils.isBlank(token.getNodeName()) || token.getIndex() < 0)
			return null;
		
		FileBucket bucket = null;
		try{
			bucket = new FileBucket(token.getNodeName());
			return bucket.get(token.getIndex());
		}finally{
			if(bucket != null)
				bucket.close();
		}
	}
	
	public static void main(String[] args) throws Exception{
		Luggage luggage = new Luggage("test");
		
		byte[] bs = new byte[100];
		Token token = luggage.saveData(bs);
		luggage.getData(token);
	}
}
