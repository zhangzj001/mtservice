package cn.jugame.http;

import java.net.HttpCookie;
import java.nio.channels.SocketChannel;

import cn.jugame.mt.Job;
import cn.jugame.mt.MtJob;
import cn.jugame.mt.MtPackage;
import cn.jugame.mt.MtServer;
import cn.jugame.mt.NioSocket;
import cn.jugame.util.Common;
import sun.util.logging.resources.logging;

public abstract class HttpJob implements Job{
	
	protected abstract boolean handleRequest(HttpRequest req, HttpResponse resp);
	
	@Override
	public boolean doJob(NioSocket socket, Object packet) {
		//处理用户请求
		HttpRequest request = (HttpRequest)packet;
		HttpResponse response = new HttpResponse();
		
		if(!handleRequest(request, response))
			return false;
		
		//如果请求头说明有Accept-Encoding=gzip，则返回的数据也gzip压缩
		String s = request.getHeader("Accept-Encoding");
		if(s != null && s.indexOf("gzip") != -1){
			//同时对数据进行压缩处理
			byte[] content = response.getContent();
			if(content != null && content.length > 0){
				response.setContent(Common.gzencode(content));
				response.setHeader("Content-Encoding", "gzip");
			}
		}
		
		//如果请求头中有说明Connection:keep-alive的，返回的头部也带上这个，并且保持这个连接
		s = request.getHeader("Connection");
		if("keep-alive".equalsIgnoreCase(s) || "keepalive".equalsIgnoreCase(s)){
			response.setHeader("Connection", "keep-alive");
		}else{
			response.setHeader("Connection", "close");
		}
		
		//如果存在session，则在Set-Cookie头部中带上SESSIONID
		HttpSession session = request.session(false);
		if(session != null){
			HttpCookie cookie = new HttpCookie("SESSIONID", session.getId());
			cookie.setPath("/");
			cookie.setHttpOnly(true);
			response.setCookie(cookie);
		}
		
		//将数据写回客户端
		if(!socket.send(response.getData())){
			return false;
		}
		
		//总是返回true，意味着必须让客户端去关闭这个连接，这可以有效避免服务器陷入TimeWait泛滥的境地
		//不过有可能恶意的客户端一直hold着不放，还好我有LRUChannelManager
		return true;
	}

	@Override
	public void beforeCloseSocket(NioSocket socket) {
		//do nothing
	}
}
