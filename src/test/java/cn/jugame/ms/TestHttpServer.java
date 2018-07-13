package cn.jugame.ms;

import cn.jugame.http.HttpJob;
import cn.jugame.http.HttpRequest;
import cn.jugame.http.HttpResponse;
import cn.jugame.http.HttpService;

public class TestHttpServer extends HttpJob{
	@Override
	protected boolean handleRequest(HttpRequest req, HttpResponse resp){
		//we can get query-string
		System.out.println(req.getQueryString());
		
		//we can get Request body here
		byte[] bs = req.getData();
		if(bs == null || bs.length == 0){
			return true;
		}
		//echo this request
		resp.setContent(bs);
		
		//keep-alive
		return true;
		//Or we can return false to abort connection!!
		//return false;
	}
	
	public static void main(String[] args) {
		HttpService service = new HttpService(new TestHttpServer());
		service.setReactorCount(4);
		service.setWorderCount(16);
		service.setPort(8080);
		if(!service.init()){
			return;
		}
		service.run();
	}
}
