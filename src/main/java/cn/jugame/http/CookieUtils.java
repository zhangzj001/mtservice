package cn.jugame.http;

import java.net.HttpCookie;

import org.apache.commons.lang.StringUtils;

import cn.jugame.util.Common;

class CookieUtils {
	public static String cookie2string(HttpCookie cookie){
		StringBuffer sb = new StringBuffer();
		sb.append(cookie.getName()).append("=").append(Common.url_encode(cookie.getValue()));
		if(StringUtils.isNotBlank(cookie.getPath())){
			sb.append("; Path=").append(cookie.getPath());
		}
		if(StringUtils.isNotBlank(cookie.getDomain())){
			sb.append("; Domain=").append(cookie.getDomain());
		}
		if(cookie.getMaxAge() >= 0){
			sb.append("; Max-Age=").append(cookie.getMaxAge());
		}
		if(cookie.getSecure()){
			sb.append("; Secure");
		}
		if(cookie.isHttpOnly()){
			sb.append("; HttpOnly");
		}
		if(cookie.getDiscard()){
			sb.append("; Discard");
		}
		return sb.toString();
	}
}
