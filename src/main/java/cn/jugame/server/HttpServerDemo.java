package cn.jugame.server;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import cn.jugame.http.HttpRequest;
import cn.jugame.http.HttpResponse;
import cn.jugame.http.HttpService;
import cn.jugame.http.HttpSession;
import cn.jugame.http.Multipart;
import cn.jugame.http.SimpleHttpRequestHandler;
import cn.jugame.http.multipart.FilePart;
import cn.jugame.http.multipart.ParamPart;
import cn.jugame.http.multipart.Part;
import cn.jugame.http.router.ActionKey;
import cn.jugame.http.router.ControllerKey;
import cn.jugame.util.Common;

@ControllerKey(value="/demo")
public class HttpServerDemo{
	
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
	
	@ActionKey
	public void sayHello(HttpRequest request, HttpResponse response){
		System.out.println("queryString=>" + request.getQueryString());
		String uri = request.getUri();
		System.out.println("uri => " + uri);
		System.out.println("data.length => " + request.getData().length);
		System.out.println("Method: " + request.getMethod());
		
		Map<String, String> headers = request.getHeaders();
		for(Entry<String, String> e : headers.entrySet()){
			System.out.println("header => " + e.getKey() + " : " + e.getValue());
		}
		
		List<HttpCookie> cookies = request.getCookies();
		for(HttpCookie cookie : cookies){
			System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue());
		}
		
		//创建session
		HttpSession session = request.session();
		System.out.println("SessionId => " + session.getId());
		
		//下载文件
//		byte[] bs = Common.file_get_contents("D:/book/[MySQL核心技术手册(第二版)].pdf"); //呵呵呵
//		System.out.println("file.length => " + bs.length);
//		response.setHeader("Content-Type", "application/octet-stream");
//		response.setHeader("Content-Length", String.valueOf(bs.length));
//		response.setHeader("Content-Disposition", "attachment; filename=myfile.pdf");
//		response.setContent(bs);
		
		HttpCookie cookie = new HttpCookie("a", "b");
		cookie.setPath(request.getUri());
		cookie.setMaxAge(10);
		response.setCookie(cookie);
		response.setCookie(new HttpCookie("c", "d"));
		
		//解析参数
		if(request.isMultipart()){
			System.out.println("multipart结构的参数");
			Multipart mr = new Multipart(request);
			for(String name : mr.paramNames()){
				Part p = mr.getPart(name);
				if(p.isParam()){
					ParamPart part = (ParamPart)p;
					System.out.println("ParamPart: " + part.getName() + "=>" + part.getStringValue());
				}
				else if(p.isFile()){
					FilePart part = (FilePart)p;
					byte[] bs = part.getFileContent();
					Common.file_put_contents("D:/" + part.getFileName(), bs, false);
					System.out.println("FilePart: " + part.getName() + ", filename=>" + part.getFileName() + ", content-type=>" + part.getContentType() + ", filepath=>" + part.getFilePath() + ", file.length=>" + bs.length);
				}
			}
		}else{
			System.out.println("非multipart结构的参数");
			try{
			System.out.println("data => " + new String(request.getData(), "UTF-8"));
			}catch(Exception e){}
		}
		
		response.setContent("hello world");
	}
	
	//这一行用来启动spring
	public static void main(String[] args) {
		HttpService service = new HttpService(new SimpleHttpRequestHandler());
		if(!service.init()){
			System.out.println("启动Service失败");
			return;
		}
		service.run();
	}
}
