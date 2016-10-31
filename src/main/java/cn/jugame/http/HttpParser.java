package cn.jugame.http;

import java.util.TreeMap;

/**
 * 对请求数据流进行协议解析
 * @author zimT_T
 *
 */
public class HttpParser extends AParser{
	
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
}
