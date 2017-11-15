package cn.jugame.mt;

import cn.jugame.mt.sm.SocketManager;

public interface INio {
	/**
	 * 获取任务
	 * @return
	 */
	public Job getJob();
	
	/**
	 * 获取socket管理器
	 * @return
	 */
	public SocketManager getSocketManager();
	
	/**
	 * 获取任务执行器
	 * @return
	 */
	public TaskExecutor getTaskExecutor();
}
