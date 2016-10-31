package cn.jugame.http;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import cn.jugame.mt.MtPackage;
import cn.jugame.util.Common;

public class HttpRequest implements MtPackage{
	
	private String method = "";
	private String uri = "";
	private String protocol = "";
	private TreeMap<String, String> headers = new TreeMap<String, String>();
	private byte[] content = new byte[0];
	private String queryString = "";
	private String sessionId;
	
	//存储cookie的容器
	private CookieManager cm = new CookieManager();

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
		CookieStore cookieStore = cm.getCookieStore();
    	String[] vs = Common.array_filter(value.split(";"));
    	for(String v : vs){
			List<HttpCookie> list = HttpCookie.parse(v);
			for(HttpCookie cookie : list){
				try{cookieStore.add(new URI(getUri()), cookie);}catch(Exception e){e.printStackTrace();}
				//如果是SESSIONID，就将值保存下来
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

	public List<HttpCookie> getCookies(){
		return cm.getCookieStore().getCookies();
	}
	
	public HttpCookie getCookie(String name){
		if(StringUtils.isBlank(name))
			return null;
		
		List<HttpCookie> cookies = cm.getCookieStore().getCookies();
		for(HttpCookie cookie : cookies){
			if(name.equals(cookie.getName()))
				return cookie;
		}
		return null;
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
}
