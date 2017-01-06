package cn.jugame.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.http.router.Action;
import cn.jugame.http.router.Router;
import sun.util.logging.resources.logging;

public class SimpleHttpRequestHandler extends HttpJob{
	
	private Router router = new Router();
	public SimpleHttpRequestHandler() {
		//初始化路由器
		router.init();
	}

	@Override
	protected boolean handleRequest(HttpRequest req, HttpResponse resp) {
		String method = req.getMethod();
		//根据方法来处理
		if("GET".equalsIgnoreCase(method))
			return doGet(req, resp);
		else if("POST".equalsIgnoreCase(method))
			return doPost(req, resp);
		else
			return doOtherMethod(req, resp);
	}
	
	protected boolean doGet(HttpRequest req, HttpResponse resp){
		return doHandle(req, resp);
	}
	
	protected boolean doPost(HttpRequest req, HttpResponse resp){
		return doHandle(req, resp);
	}
	
	protected boolean doOtherMethod(HttpRequest req, HttpResponse resp){
		return true;
	}
	
	/**
	 * 请求处理方法，在<code>doGet</code>和<code>doPost</code>中都默认调用该方法处理。
	 * @param req
	 * @param resp
	 * @return
	 */
	protected boolean doHandle(HttpRequest req, HttpResponse resp){
		Action action = router.get(req.getUri());
		if(action != null){
			action.invoke(req, resp);
		}
		return true;
	}
}

