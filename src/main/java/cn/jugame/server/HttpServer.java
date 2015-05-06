package cn.jugame.server;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.jugame.http.HttpJob;
import cn.jugame.http.HttpRequest;
import cn.jugame.http.HttpResponse;
import cn.jugame.http.HttpService;
import cn.jugame.util.Common;
import cn.jugame.util.helper.net.HttpFetcher;

public class HttpServer extends HttpJob{
	
	static final String LOG_USER_APPS = "/user_apps";
	
	private Map<String, String> parseParams(String queryString){
		TreeMap<String, String> params = new TreeMap<String, String>();
		String[] ss = queryString.split("&");
		for(int i=0; i<ss.length; ++i){
			String[] kv = ss[i].split("=");
			if(kv.length == 2){
				String k = kv[0].trim();
				String v = Common.url_decode(kv[1].trim());
				params.put(k, v);
				
				System.out.println("param: " + k + "=>" + v);
			}
		}
		return params;
	}

	@Override
	protected boolean handleRequest(HttpRequest request, HttpResponse response) {
		Map<String, String> params = parseParams(request.getQueryString());
		String uri = request.getUri();
		
		//记录用户请求日志
		if(uri.indexOf(LOG_USER_APPS) != -1){
			request.getData();
		}
		
		response.setContent("hello world");
		return true;
	}
	
	
	//这一行用来启动spring
	public static ApplicationContext ctx = new ClassPathXmlApplicationContext("spring-config.xml");
	public static void main(String[] args) {
		HttpService service = new HttpService(new HttpServer());
		if(!service.init()){
			System.out.println("启动Service失败");
			return;
		}
		service.run();
	}
}
