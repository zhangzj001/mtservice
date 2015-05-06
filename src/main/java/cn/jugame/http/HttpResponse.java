package cn.jugame.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import cn.jugame.mt.MtPackage;

public class HttpResponse implements MtPackage{
	
	private String protocol = "HTTP/1.1";
	private int statusCode = 200;
	private String statusMsg = "OK";
	private TreeMap<String, String> headers = new TreeMap<String, String>();
	private byte[] content = new byte[0];
	
	//这个是整个响应报文
	private ByteArrayOutputStream stream = new ByteArrayOutputStream();
	
	public HttpResponse(){
		//设置一些基本的头部
		setHeader("Content-Type", "text/plain;charset=utf-8");
		setHeader("Content-Length", String.valueOf(this.content.length));
		setHeader("Server", "MT/1.0");
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
		headers.put(name, value);
	}
	
	private byte[] parseResponse(){
		StringBuffer buf = new StringBuffer();
		//响应头第一行
		buf.append(this.protocol).append(" ").append(statusCode).append(" ").append(statusMsg).append("\r\n");
		//响应头的头部
		for(Entry<String, String> e : headers.entrySet()){
			buf.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
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
	
	public Map<String, String> getHeaders(){
		return headers;
	}

	@Override
	public byte[] getData() {
		return parseResponse();
	}

}
