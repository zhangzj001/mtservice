package cn.jugame.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author cai9911
 *
 */
public class JuConfig {
	public static final String CONFIG_FILE_NAME = "resources.properties";
	public static Properties props = new Properties();

	static {
		try {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
			props.load(is);
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 读取配置文件
	 */
	public static void reload(){
	    try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
            props.load(is);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * 获得配置的key
	 * @param key key-name
	 * @return key-value
	 */
	public static String getValue(String key) {
		return props.getProperty(key);
	}

	/**
	 * 获得配置的key，返回值转型为Int
	 * @param key key-name
	 * @return key-value
	 */
	public static int getValueInt(String key){
		return Common.convert2Int(getValue(key));
	}
	
	/**
	 * 读取所有配置
	 * @return
	 */
	public static Properties getProps(){
	    return props;
	}
	
	/**
	 * 设置配置的key
	 * @param key key-name
	 * @param value key-value 
	 */
	public static void updateProperties(String key, String value) {
		props.setProperty(key, value);
	}
}
