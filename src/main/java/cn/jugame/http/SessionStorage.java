package cn.jugame.http;

import cn.jugame.util.MemcachedUtils;

public class SessionStorage {
	private static boolean isInit = false;
	public static void init(String mcIp, int mcPoolSize){
		MemcachedUtils.init(mcIp, mcPoolSize);
		isInit = true;
	}
	
	private SessionStorage(){}
	
	public static final String SESSIONID = "SESSIONID";
	
	//session的过期时间，秒数
	private static int sessionTimeout = 1800;
	
	public static void setSessionTimeout(int timeout){
		sessionTimeout = timeout;
	}
	
	public static HttpSession create(){
		if(!isInit)
			return null;
		HttpSession session = new HttpSession();
		MemcachedUtils.set(session.getId(), sessionTimeout, session);
		return session;
	}
	
	public static HttpSession get(String sessionId){
		if(!isInit)
			return null;
		return MemcachedUtils.get(sessionId);
	}
}
