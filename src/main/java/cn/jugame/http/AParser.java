package cn.jugame.http;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.TreeMap;

public abstract class AParser {
	//http请求头的名称标准化
	public static final TreeMap<String, String> HEADER_FORMAT = new TreeMap<String, String>();
	static{
		HEADER_FORMAT.put("accept", "Accept");
		HEADER_FORMAT.put("acceptcharset", "Accept-Charset");
		HEADER_FORMAT.put("acceptlanguage", "Accept-Language");
		HEADER_FORMAT.put("acceptencoding", "Accept-Encoding");
		HEADER_FORMAT.put("authorization", "Authorization");
		HEADER_FORMAT.put("cachecontrol", "Cache-Control");
		HEADER_FORMAT.put("connection", "Connection");
		HEADER_FORMAT.put("contenttype", "Content-Type");
		HEADER_FORMAT.put("cookie", "Cookie");
		HEADER_FORMAT.put("expect", "Expect");
		HEADER_FORMAT.put("from", "From");
		HEADER_FORMAT.put("host", "Host");
		HEADER_FORMAT.put("ifmatch", "If-Match");
		HEADER_FORMAT.put("ifmodifiedsince", "If-Modified-Since");
		HEADER_FORMAT.put("ifnonematch", "If-None-Match");
		HEADER_FORMAT.put("ifunmodifiedsince", "If-Unmodified-Since");
		HEADER_FORMAT.put("pragma", "Pragma");
		HEADER_FORMAT.put("proxyauthorization", "Proxy-Authorization");
		HEADER_FORMAT.put("range", "Range");
		HEADER_FORMAT.put("referer", "Referer");
		HEADER_FORMAT.put("upgrage", "Upgrage");
		HEADER_FORMAT.put("useragent", "User-Agent");
		HEADER_FORMAT.put("via", "Via");
		HEADER_FORMAT.put("warning", "Warning");
	}

	public static String fixHeaderName(String name){
		String s = name.toLowerCase().replace("-", "");
		if(HEADER_FORMAT.containsKey(s))
			return HEADER_FORMAT.get(s);
		return name;
	}
	
	private int curr_index = 0;
	//用来存储数据流的一个容器，源源不断地往这个容器里塞数据，同时再触发解析动作
	private int capacity = 1024;
	private ByteBuffer inBuf = ByteBuffer.wrap(new byte[capacity]);
	
	protected byte read_byte(int pos){
		try{
			return inBuf.get(pos);
		}catch(IndexOutOfBoundsException e){
			return -1;
		}
	}
	
	protected byte[] read_line_bytes(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int pos = curr_index;
		byte b = -1;
		while((b = read_byte(pos++)) != -1){
			out.write(b);
			if(b == '\r' && read_byte(pos) == '\n')
				break;
		}
		//如果b==-1，说明没有读到完整的一行
		if(b == -1)
			return null;
		
		curr_index = pos+1;
		return out.toByteArray();
	}
	
	protected void append_bytes(byte[] bs){
		//如果容量不够了
		if(inBuf.remaining() < bs.length){
			inBuf.flip();
			capacity = inBuf.capacity() + bs.length * 2;
			ByteBuffer new_buf = ByteBuffer.wrap(new byte[capacity]);
			new_buf.put(inBuf);
			inBuf = new_buf;
		}
		
		inBuf.put(bs);
	}

	protected String read_line(){
		byte[] bs = read_line_bytes();
		if(bs == null)
			return null;
		try{
			return new String(bs, "UTF-8").trim();
		}catch(Exception e){
			return null;
		}
	}
	
	protected byte[] read(int content_length){
		int remaining = inBuf.position() - curr_index;
		if(remaining >= content_length){
			//将position定在curr_index的位置，往后的都是数据
			inBuf.position(curr_index);
			byte[] bs = new byte[content_length];
			inBuf.get(bs);
			return bs;
		}
		return null;
	}
	
	protected int remaining(){
		return inBuf.limit() - curr_index;
	}
	
	protected void resetBuf(){
		inBuf.clear();
	}
}
