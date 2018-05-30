### A very simple TCP server
  This component is based on java-nio, and has been used in a core message service in 8868.cn for many years, and proved stable and reliable. It's simpler and easier than Netty, but as fast as Netty (you can try yourself ^_^).
  You can just easily use it like this demo, what you need to do is to implement the method: `doJob` 

#### Message-Service Demo:{
public class TestServer extends MessageService{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());


	@Override
	protected boolean doJob(NioSocket socket, JSONObject data) {
		System.out.println("receive a message: " + data);
		
		//send back this message!!
		if(!send(socket, data)){
			//terminate this socket!
			return false;
		}
		
		//keep-alive
		return true;
	}

	private boolean send(NioSocket socket, JSONObject content){
		if(!socket.isOpen()){
			logger.info("remote channel closed...");
			return false;
		}
		try{
			byte[] bs = MessageProtocalParser.toMessage(content.toString().getBytes("UTF-8"));
			return socket.send(bs);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		TestServer server = new TestServer();
		if(!server.init()){
			return;
		}
		server.run();
	}
}
}

### Http-Service Demo：{
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
}

