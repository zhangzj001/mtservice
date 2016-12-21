package cn.jugame.fs;

import org.apache.commons.lang.StringUtils;

import cn.jugame.util.ByteHelper;
import cn.jugame.util.Common;

/**
 * 数据存储器。<br>
 * 当放入数据成功之后可以得到一个令牌，以后需要取这个数据则需要通过令牌来索引并取得。
 * @author zimT_T
 *
 */
public class Luggage {
	
	private FileBucket bucket;
	
	private String bucketName(){
		return "bucket_" + Common.now("yyyyMMddHHmmss") + "-" + Common.rand(10, 99);
	}
	
	/**
	 * 获取一个filebucket用于存储数据，如果容量不够了则新建一个。判断容量是否需要传入参数length，用于表明准备存储的数据大小。
	 * @param length
	 * @return
	 */
	private FileBucket getBucket(long length){
		if(bucket == null){
			return new FileBucket(bucketName());
		}
		
		//如果已经存在一个bucket了，看看是否满了，如果满了就换一个
		if(bucket.isFull(length)){
			bucket.close();
			bucket = new FileBucket(bucketName());
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
		System.setProperty("user.dir", "D:/fs");
		
		FileBucket bucket = new FileBucket("test");
		if(bucket.add(new byte[10]) == -1){
			System.out.println("add 10 fail");
		}
		if(bucket.add(new byte[20]) == -1){
			System.out.println("add 20 fail");
		}
		
		for(int i=0; i<bucket.size(); ++i){
			byte[] bs = bucket.get(0);
			System.out.println(ByteHelper.bytesToHexString(bs));
		}
		
		INodeFile inodeFile = bucket.getINodeFile();
		System.out.println(inodeFile.version());
		System.out.println(inodeFile.vnodeFileCount());
		System.out.println(inodeFile.offset());
	}
}
