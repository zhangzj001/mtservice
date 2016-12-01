package cn.jugame.mt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutor {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ExecutorService executorService;
	private Context context;
	public TaskExecutor(int workerCount, Context context){
		this.executorService = Executors.newFixedThreadPool(workerCount);
		this.context = context;
	}
	
	public void execute(final NioSocket nioSocket, final Object packet){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try{
					if(!context.getJob().doJob(nioSocket, packet)){
						//任务执行失败，则断开socket
						context.releaseSocket(nioSocket);
					}
				}catch(Throwable e){logger.error("doJob error", e);}
			}
		});
	}
}
