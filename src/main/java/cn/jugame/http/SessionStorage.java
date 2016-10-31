package cn.jugame.http;

import cn.jugame.util.Cache;

class SessionStorage {
	
	private SessionStorage(){}
	
	public static final String SESSIONID = "SESSIONID";
	
	//session的过期时间，秒数
	private static int sessionTimeout = 1800;
	
	public static void setSessionTimeout(int timeout){
		sessionTimeout = timeout;
	}
	
	public static HttpSession create(){
		HttpSession session = new HttpSession();
		Cache.set(session.getId(), sessionTimeout, session);
		return session;
	}
	
	public static HttpSession get(String sessionId){
		return Cache.get(sessionId);
	}
}
