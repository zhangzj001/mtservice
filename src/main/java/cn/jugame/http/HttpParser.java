package cn.jugame.http;

import java.nio.ByteBuffer;
import java.util.TreeMap;

import cn.jugame.mt.ProtocalParser;

/**
 * 对请求数据流进行协议解析
 * @author zimT_T
 *
 */
public class HttpParser extends AParser implements ProtocalParser{
	
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
	
	private HttpRequest request = new HttpRequest();
	
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
		
		return read(content_length);
	}
	
	public boolean cycle_parse(byte[] bs){
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

	@Override
	public boolean parse(ByteBuffer buf) {
		//将buf的内容导入内部缓冲区
		byte[] bs = new byte[buf.remaining()];
		buf.get(bs);
		if(!cycle_parse(bs))
			return false;
		
		//解析成功了，但是这里有可能this.inBuf中还存在一些剩余数据
		//如果真的有，那说明可能是第二个http请求的头部，需要将数据还给buf，重新定位position即可 
//		System.out.println("remaining => " + this.remaining());
//		if(this.remaining() > 0){
//			buf.position(buf.limit() - this.remaining());
//		}
		
		return true;
	}

	@Override
	public Object take() {
		if(current_state != PARSE_FINISH)
			return null;
		return request;
	}

	@Override
	public void reset() {
		current_state = PARSE_STATUS_LINE;
		request = new HttpRequest();
		resetBuf();
	}
}
