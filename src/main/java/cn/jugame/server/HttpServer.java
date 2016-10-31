package cn.jugame.server;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.jugame.http.HttpCookie;
import cn.jugame.http.HttpJob;
import cn.jugame.http.HttpRequest;
import cn.jugame.http.HttpResponse;
import cn.jugame.http.HttpService;
import cn.jugame.http.HttpSession;
import cn.jugame.util.Common;

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
		System.out.println("uri => " + uri);
		System.out.println("data.length => " + request.getData().length);
		
		Map<String, String> headers = request.getHeaders();
		for(Entry<String, String> e : headers.entrySet()){
			System.out.println("header => " + e.getKey() + " : " + e.getValue());
		}
		
		try{
		System.out.println("data => " + new String(request.getData(), "UTF-8"));
		}catch(Exception e){}
		
		List<HttpCookie> cookies = request.getCookies();
		for(HttpCookie cookie : cookies){
			System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue());
		}
		
//		HttpSession session = request.session();
//		System.out.println("SessionId => " + session.getId());
		
//		byte[] bs = Common.file_get_contents("D:/book/[MySQL核心技术手册(第二版)].pdf");
//		response.setHeader("Content-Type", "application/octet-stream");
//		response.setHeader("Content-Length", String.valueOf(bs.length));
//		response.setHeader("Content-Disposition", "attachment; filename=myfile.pdf");
//		response.setContent(bs);
		
		HttpCookie cookie = new HttpCookie("a", "b");
		cookie.setPath(request.getUri());
		cookie.setMaxAge(10);
		response.setCookie(cookie);
		response.setCookie(new HttpCookie("c", "d"));
		
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
