package cn.jugame.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 这是一个基于倒计时的执行器，当timeout到期的时候将执行runnable.run()，同时clocker线程终止。<br>
 * 这个设计是用于显示地设置一个超时器，超时之后执行一定的操作。
 * @author zimT_T
 *
 */
public class Clocker extends Thread{
	private AtomicInteger isResetTimeout = new AtomicInteger(0);
	
	private long timeout;
	private Runnable runnable;
	/**
	 * 基于倒计时的执行器构造函数
	 * @param runnable 执行函数
	 * @param timeout 超时时间，毫秒
	 */
	public Clocker(Runnable runnable, long timeout){
		this.runnable = runnable;
		this.timeout = timeout;
	}
	
	/**
	 * 重置倒计时，并设置新的倒计时为timeout毫秒
	 * @param timeout 倒计时，毫秒
	 */
	public void reset(long timeout){
		this.timeout = timeout;
		reset();
	}
	
	/**
	 * 重置倒计时
	 */
	public void reset(){
		//标记为被唤醒的
		isResetTimeout.set(1);
		
		//唤醒当前线程
		synchronized (this) {
			this.notify();	
		}
	}

	@Override
	public void run(){
		try{
			while(true){
				//陷入沉睡
				synchronized (this) {
					this.wait(timeout);	
				}
				
				//如果是重置倒计时而被唤醒了，重新沉睡就好
				//但如果是超时醒来的，执行runnable逻辑，完了之后再重新沉睡
				if(0 == isResetTimeout.getAndSet(0)){
					runnable.run();
					//执行完就退出
					break;
				}else{
					System.out.println("这是重置倒计时而被 唤醒的，没机会跑runnable");
				}
			}
		}catch (InterruptedException e) {
			//线程在wait的时候被interrupt了
		}
	}
	
	public static void main(String[] args) {
		Clocker clocker = new Clocker(new Runnable() {
			@Override
			public void run() {
				System.out.println("ffffffffffffuck");
			}
		}, 200);
		clocker.start();
		
		while(true){
			try{Thread.sleep(100);}catch (Exception e) {}
			clocker.reset();
		}
	}
}
