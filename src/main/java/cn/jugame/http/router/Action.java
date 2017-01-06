package cn.jugame.http.router;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.http.HttpRequest;
import cn.jugame.http.HttpResponse;

public class Action {
	private String uri;
	private Object controller;
	private Method method;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public Action(String uri, Object controller, Method method){
		this.uri = uri;
		this.controller = controller;
		this.method = method;
	}

	public String getUri() {
		return uri;
	}
	
	public void invoke(HttpRequest req, HttpResponse resp){
		try{
			method.invoke(controller, req, resp);
		}catch(Exception e){
			logger.error("Action invoke error", e);
		}
	}
}
