package cn.jugame.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class HttpResponse {
	
	private String protocol = "HTTP/1.1";
	private int statusCode = 200;
	private String statusMsg = "OK";
	private TreeMap<String, List<String>> headers = new TreeMap<String, List<String>>();
	private byte[] content = new byte[0];
	
	public HttpResponse(){
		//设置一些基本的头部
		setHeader("Content-Type", "text/plain;charset=utf-8");
		setHeader("Content-Length", String.valueOf(this.content.length));
		setHeader("Server", "MT/3.0");
	}
	
	public void setProtocol(String protocol){
		this.protocol = protocol;
	}
	
	public void setStatusCode(int statusCode){
		this.statusCode = statusCode;
	}
	
	public void setStatusMsg(String statusMsg){
		this.statusMsg = statusMsg;
	}
	
	public void setContent(byte[] content){
		this.content = content;
		
		//顺便设置content-length头部
		setHeader("Content-Length", String.valueOf(this.content.length));
	}
	
	public void setContent(String content){
		try{
			setContent(content.getBytes("UTF-8"));
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
	}
	
	public byte[] getContent(){
		return content;
	}
	
	public void setHeader(String name, String value){
		List<String> vs = headers.get(name);
		if(vs == null){
			vs = new LinkedList<>();
			headers.put(name, vs);
		}
		
		//目前为止，我只知道Set-Cookie是支持多个同名头的 ，所以除了Set-Cookie头之外，其余头部都采取覆盖的方式
		if(!"Set-Cookie".equalsIgnoreCase(name))
			vs.clear();
		
		vs.add(value);
	}
	
	public void setCookie(HttpCookie cookie){
		setHeader("Set-Cookie", CookieUtils.cookie2string(cookie));
	}
	
	private byte[] parseResponse(){
		StringBuffer buf = new StringBuffer();
		//响应头第一行
		buf.append(this.protocol).append(" ").append(statusCode).append(" ").append(statusMsg).append("\r\n");
		//响应头的头部
		for(Entry<String, List<String>> e : headers.entrySet()){
			for(String v : e.getValue()){
				buf.append(e.getKey()).append(": ").append(v).append("\r\n");
			}
		}
		//数据体分隔行
		buf.append("\r\n");
		
		try{
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			stream.write(buf.toString().getBytes("UTF-8"));
			stream.write(content);
			return stream.toByteArray();
		}catch(IOException e){
			e.printStackTrace();
			return new byte[0];
		}
	}
	
	public Map<String, List<String>> getHeaders(){
		return headers;
	}

	public byte[] getData() {
		return parseResponse();
	}

}
