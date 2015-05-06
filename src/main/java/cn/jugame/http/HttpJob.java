package cn.jugame.http;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Map.Entry;

import cn.jugame.mt.MtJob;
import cn.jugame.mt.MtPackage;
import cn.jugame.mt.MtServer;
import cn.jugame.util.Common;

public abstract class HttpJob implements MtJob{
	protected MtServer serv;
	public void setMtServer(MtServer serv) {
		this.serv = serv;
	}
	
	protected abstract boolean handleRequest(HttpRequest req, HttpResponse resp);
	
	@Override
	public boolean do_job(SocketChannel channel, MtPackage bs) {
		//处理用户请求
		HttpRequest request = (HttpRequest)bs;
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
		
		//将数据写回客户端
		serv.write_channel(channel, response);
		
		//总是返回true，意味着必须让客户端去关闭这个连接，这可以有效避免服务器陷入TimeWait泛滥的境地
		//不过有可能恶意的客户端一直hold着不放，还好我有LRUChannelManager
		return true;
	}

	@Override
	public void before_close_channel(SocketChannel channel) {
	}
}
