package cn.jugame.http;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import cn.jugame.util.Common;

public class HttpCookie {
	
	private String name;
	private String value;
	private String path;
	private long maxAge = -1;
	private String domain;
	private boolean secure = false;
	private boolean httpOnly = false;
	
	public HttpCookie(String name, String value) {
		this.name = name;
		this.value = value;
	}
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name).append("=").append(Common.url_encode(value));
		if(StringUtils.isNotBlank(path)){
			sb.append("; Path=").append(path);
		}
		if(maxAge >= 0){
			sb.append("; Max-Age=").append(maxAge);
		}
		if(StringUtils.isNotBlank(domain)){
			sb.append("; Domain=").append(domain);
		}
		if(secure){
			sb.append("; Secure");
		}
		if(httpOnly){
			sb.append("; HttpOnly");
		}
		return sb.toString();
	}
	
	public static List<HttpCookie> parse(String s){
		List<HttpCookie> cookies = new LinkedList<>();
		
		//XXX 还是沿用java.net.HttpCookie的解析方式
		List<java.net.HttpCookie> cs = java.net.HttpCookie.parse(s);
		for(java.net.HttpCookie c : cs){
			HttpCookie cookie = new HttpCookie(c.getName(), c.getValue());
			cookie.setHttpOnly(c.isHttpOnly());
			cookie.setDomain(c.getDomain());
			cookie.setMaxAge(c.getMaxAge());
			cookie.setPath(c.getPath());
			cookie.setSecure(c.getSecure());
			cookies.add(cookie);
		}
		return cookies;
	}
}
