package cn.jugame.lab.cluster;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

public class zkClient {
	public static void main(String[] args) {
		RetryPolicy retryPolicy = new RetryNTimes(10, 100);
		CuratorFramework client = CuratorFrameworkFactory.newClient("192.168.0.162:2181", retryPolicy);
		client.start();
		
		
	}
}
