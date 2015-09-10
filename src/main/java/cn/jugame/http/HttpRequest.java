package cn.jugame.http;

import java.util.Map;
import java.util.TreeMap;

import cn.jugame.mt.MtPackage;
import cn.jugame.util.Common;

public class HttpRequest implements MtPackage{
	
	private String method = "";
	private String uri = "";
	private String protocol = "";
	private TreeMap<String, String> headers = new TreeMap<String, String>();
	private byte[] content = new byte[0];
	private String queryString = "";

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
		
		//顺便把queryString解析好
		int idx = uri.indexOf("?");
		if(idx >= 0){
			this.setQueryString(uri.substring(idx+1));
		}
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setData(byte[] content) {
		this.content = content;
	}

	public void setHeader(String name, String value){
		headers.put(name, value);
	}
	
	public String getHeader(String name){
		return headers.get(name);
	}
	
	public Map<String, String> getHeaders(){
		return headers;
	}

	@Override
	public byte[] getData() {
		return content;
	}
	
	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public boolean isReady() {
		return true;
	}

}
