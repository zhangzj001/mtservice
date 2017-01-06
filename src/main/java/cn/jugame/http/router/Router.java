package cn.jugame.http.router;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.dreampie.ClassSearchKit;

public class Router {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private Map<String, Action> map = new ConcurrentHashMap<>();
	
	public void init(){
		List<Class<? extends Object>> list = ClassSearchKit.of(Object.class).includepaths().search();
		for(int i=0; i<list.size(); ++i){
			Class<?> clz = list.get(i);
			ControllerKey controllerKey = (ControllerKey)clz.getAnnotation(ControllerKey.class);
			if(controllerKey != null){
				logger.info("find controller: " + clz.toString());
				try{
					Object controller = clz.newInstance();
					this.add(controllerKey.value(), controller);
				}catch(Exception e){
					logger.error("router error", e);
				}
			}
		}
	}
	
	private void add(String path, Object controller){
		Method[] methods = controller.getClass().getDeclaredMethods();
		for(Method method : methods){
			ActionKey actionKey = (ActionKey)method.getAnnotation(ActionKey.class);
			if(actionKey != null){
				String uri = path + (StringUtils.isNotBlank(actionKey.value()) ? actionKey.value() : ("/"+method.getName()));
				Action action = new Action(uri, controller, method);
				logger.info("add router: " + action.getUri());
				map.put(action.getUri(), action);
			}
		}
	}
	
	public Action get(String uri){
		return map.get(uri);
	}
	
}
