package cn.jugame.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class MemcachedUtils {
    private static MemcachedClient client;
    
    public static final int TIMEOUT = 2000;//操作超时时间，单位 毫秒
    private static final Logger logger = LoggerFactory.getLogger(MemcachedUtils.class);
    
    public static void init(String server, int poolSize) {
        if(client != null) {
            return;
        }
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(server));
        builder.setCommandFactory(new BinaryCommandFactory());
        builder.setEnableHealSession(true);
        builder.setConnectionPoolSize(poolSize);
        
        try {
            client =  builder.build();
        } catch (IOException e) {
            client  =  null;
        }
    }
    

    /**
     * 
     * @param key
     * @param exp 秒
     * @param value
     * @return
     */
    public static boolean set(String key, int exp, Object value) {
        try {
            return client.set(key, exp, value);
        } catch(Exception e) {
            logger.error(e.toString());
            return false;
        }
    }
    
    public static boolean del(String key) {
        try {
            return client.delete(key);
        } catch(Exception e) {
            logger.error(e.toString());
            return false;
        }
    }
    
    public static <T> T get(String key) {
        try {
            return client.get(key, MemcachedUtils.TIMEOUT);
        } catch(Exception e) {
            logger.error(e.toString());
            return null;
        }
    }
    
    public static long incr(String key, int exp) {
        return incr(key, 1, exp);
    }
    
    public static long incr(String key, long delta, int exp) {
        try {
            return client.incr(key, delta, 1, MemcachedUtils.TIMEOUT, exp);
        } catch(Exception e) {
            logger.error(e.toString());
            return -1;
        }
    }
    
    public static long decr(String key, int exp) {
        return decr(key, 1, exp);
    }
    
    public static long decr(String key, long delta, int exp) {
        try {
            return client.decr(key, delta, 1, MemcachedUtils.TIMEOUT, exp);
        } catch(Exception e) {
            logger.error(e.toString());
            return -1;
        }
    }
    
    public static boolean add(String key, int exp, Object value){
    	try{
    		return client.add(key, exp, value);
    	}catch(Exception e){
    		logger.error(e.toString());
            return false;
    	}
    }
    
    public static long addCount(String key, long delta){
    	try{
    		//FIXME 基于memcache的计数器不能设置过期时间，是个问题！！
    		return client.getCounter(key).addAndGet(delta);
    	}catch(Exception e){
    		logger.error("addCount.error", e);
    		return -1;
    	}
    }
    
    public static long getCount(String key){
    	try{
    		return client.getCounter(key).get();
    	}catch(Exception e){
    		logger.error("getCount.error", e);
    		return -1;
    	}
    }
}
