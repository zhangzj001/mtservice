package cn.jugame.http;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * 对请求数据流进行协议解析
 * @author zimT_T
 *
 */
public class HttpParser {
	
	//http请求头的名称标准化
	private static final TreeMap<String, String> HEADER_FORMAT = new TreeMap<String, String>();
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
	
	//解析第一行状态行
	private final static int PARSE_STATUS_LINE = 0;
	//解析http请求头部
	private final static int PARSE_HEADER = 1;
	//解析http请求体 
	private final static int PARSE_BODY = 2;
	//解析完成了
	private final static int PARSE_FINISH = 3;
	
	//默认的请求体长度，如果是GET方法那肯定是0，如果是POST则需要读取content_length头部，如果没有这个头部，认为也是0
	private int content_length = 0;
	
	//当前状态
	private int current_state = PARSE_STATUS_LINE;
	
	//用来存储数据流的一个容器，源源不断地往这个容器里塞数据，同时再触发解析动作
	private int capacity = 1024;
	private int curr_index = 0;
	private ByteBuffer buf = ByteBuffer.allocate(capacity);
	
	private byte read_byte(int pos){
		try{
			return buf.get(pos);
		}catch(IndexOutOfBoundsException e){
			return -1;
		}
	}
	
	private byte[] read_line_bytes(){
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
	
	private void append_bytes(byte[] bs){
		//如果容量不够了
		if(buf.remaining() < bs.length){
			buf.flip();
			capacity = buf.capacity() + bs.length * 2;
			ByteBuffer new_buf = ByteBuffer.allocate(capacity);
			new_buf.put(buf);
			buf = new_buf;
		}
		
		buf.put(bs);
	}
	
	private String read_line(){
		byte[] bs = read_line_bytes();
		if(bs == null)
			return null;
		try{
			return new String(bs, "UTF-8").trim();
		}catch(Exception e){
			return null;
		}
	}
	
	private String[] parse_status_line(String s){
		String[] ss = s.split(" ", 3);
		return ss;
	}
	
	private String[] parse_header(String s){
		if(s.length() == 0)
			return new String[0];
		return s.split(":", 2);
	}
	
	private byte[] parse_body(){
		if(content_length == 0)
			return new byte[0];
		
		int remaining = buf.position() - curr_index;
		if(remaining >= content_length){
			//将position定在curr_index的位置，往后的都是数据
			buf.position(curr_index);
			byte[] bs = new byte[content_length];
			buf.get(bs);
			return bs;
		}
		
		return null;
	}
	
	public boolean cycle_parse(HttpRequest request, byte[] bs) throws Exception{
		//先把字节流append进去
		append_bytes(bs);	
		
		while(current_state != PARSE_FINISH){
			if(current_state == PARSE_STATUS_LINE){
				String s = read_line();
				//这一波没数据了
				if(s == null)
					break;
				
				String[] ss = parse_status_line(s);
				//状态行必须是3个字段
				if(ss.length != 3){
					return false;
				}
				
				request.setMethod(ss[0]);
				request.setUri(ss[1]);
				request.setProtocol(ss[2]);
				
				current_state = PARSE_HEADER;
			}
			else if(current_state == PARSE_HEADER){
				String s = read_line();
				//这一波没数据了
				if(s == null)
					break;
				
				String[] kv = parse_header(s);
				if(kv.length == 2){
					String k = fixHeaderName(kv[0].trim());
					String v = kv[1].trim();
					request.setHeader(k, v);
					//如果content_length，则记下这个长度
					if("Content-Length".equalsIgnoreCase(k)){
						try{
							content_length = Integer.parseInt(v);
						}catch(Exception e){e.printStackTrace();}
					}
				}
				//头部解析完成
				if(kv.length == 0){
					current_state = PARSE_BODY;
					continue;
				}
				if(kv.length == 1){
					//忽略这种错误格式的头部
					continue;
				}
			}
			else if(current_state == PARSE_BODY){
				byte[] content = parse_body();
				//这一波没数据了
				if(content == null)
					break;
				
				request.setData(content);
				current_state = PARSE_FINISH;
			}
		}
		
		return current_state==PARSE_FINISH;
	}
	
	private String fixHeaderName(String name){
		String s = name.toLowerCase().replace("-", "");
		if(HEADER_FORMAT.containsKey(s))
			return HEADER_FORMAT.get(s);
		return name;
	}
}
