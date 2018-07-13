package cn.jugame.http;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jugame.mt.Job;
import cn.jugame.mt.NioSocket;
import cn.jugame.util.Common;

public abstract class HttpJob implements Job{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected abstract boolean handleRequest(HttpRequest req, HttpResponse resp);
	
	protected boolean autoGzip = true;
	
	public boolean isAutoGzip() {
		return autoGzip;
	}

	public void setAutoGzip(boolean autoGzip) {
		this.autoGzip = autoGzip;
	}

	@Override
	public boolean doJob(NioSocket socket, Object packet) {
		//处理用户请求
		HttpRequest request = (HttpRequest)packet;
		HttpResponse response = new HttpResponse();
		
		//补上远程IP信息
		try{
			InetSocketAddress remoteAddr = (InetSocketAddress)socket.javaSocket().getRemoteAddress();
			request.setRemoteAddress(remoteAddr);
			InetSocketAddress localAddr = (InetSocketAddress)socket.javaSocket().getLocalAddress();
			request.setLocalAddress(localAddr);
		}catch(Exception e){
			logger.error("error", e);
			return false;
		}
		
		if(!handleRequest(request, response))
			return false;
		
		//如果请求头说明有Accept-Encoding=gzip，则返回的数据也gzip压缩，如果支持自动压缩的话
		String s = request.getHeader("Accept-Encoding");
		if(s != null && s.indexOf("gzip") != -1 && isAutoGzip()){
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
		byte[] respData = response.getData();
		if(!socket.send(respData)){
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
