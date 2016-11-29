package cn.jugame.http;

import java.io.ByteArrayInputStream;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletInputStream;

import org.apache.commons.lang.StringUtils;
import org.springframework.mock.web.DelegatingServletInputStream;

import cn.jugame.mt.MtPackage;
import cn.jugame.util.Common;

public class HttpRequest {
	
	private String method = "";
	private String uri = "";
	private String protocol = "";
	private TreeMap<String, String> headers = new TreeMap<String, String>();
	private byte[] content = new byte[0];
	private String queryString = "";
	private String sessionId;
	
	//存储cookie的容器
	private Map<String, HttpCookie> cookieMap = new LinkedHashMap<String, HttpCookie>();

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
		name = AParser.fixHeaderName(name);
		headers.put(name, value);
		
		//如果是Cookie头部，设置cookie，顺便把sessionId给找出来
		if(!"Cookie".equals(name))
        	return;
    	String[] vs = Common.array_filter(value.split(";"));
    	for(String v : vs){
    		List<HttpCookie> cookies = HttpCookie.parse(v);
    		for(HttpCookie cookie : cookies){
	    		cookieMap.put(cookie.getName(), cookie);
	    		//顺便把sessionId保存下来
				if(SessionStorage.SESSIONID.equals(cookie.getName()))
					this.sessionId = cookie.getValue();
    		}
    	}
	}
	
	public String getHeader(String name){
		name = AParser.fixHeaderName(name);
		return headers.get(name);
	}
	
	public Map<String, String> getHeaders(){
		return headers;
	}

	public byte[] getData() {
		return content;
	}
	
	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	public List<HttpCookie> getCookies(){
		List<HttpCookie> list = new ArrayList<>();
		list.addAll(cookieMap.values());
		return list;
	}
	
	public HttpCookie getCookie(String name){
		if(StringUtils.isBlank(name))
			return null;
		return cookieMap.get(name);
	}
	
	public String getContentType(){
		//header中寻找content-type字段
		String contentType = getHeader("Content-Type");
		if(StringUtils.isBlank(contentType))
			contentType = "text/plain";
		return contentType;
	}
	
	public int getContentLength(){
		return content.length;
	}
	
	public ServletInputStream getInputStream(){
		return new DelegatingServletInputStream(new ByteArrayInputStream(content));
	}
	
	private HttpSession getSession(boolean create){
		//如果没有，就说明要新建session，如果有，就去存储中寻找这个session出来
		if(StringUtils.isBlank(sessionId)){
			return create ? SessionStorage.create() : null;
		}
		
		HttpSession session = SessionStorage.get(sessionId);
		if(session == null)
			return create ? SessionStorage.create() : null;
		
		return session;
	}
	
	public HttpSession session(boolean create){
		HttpSession session = getSession(create);
		if(session != null){
			this.sessionId = session.getId();
		}

		return session;
	}
	
	public HttpSession session(){
		return session(true);
	}
	
	/**
	 * 是否为multipart类型的请求
	 * @return
	 */
	public boolean isMultipart(){
		String type = this.getHeader("Content-Type");
		return (type!=null && type.toLowerCase().startsWith("multipart/form-data"));
	}
	
}
