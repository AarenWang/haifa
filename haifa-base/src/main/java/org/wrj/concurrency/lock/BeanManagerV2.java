package org.wrj.concurrency.lock;

import java.util.concurrent.ConcurrentHashMap;

public class BeanManagerV2 {
	private ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
	
	public Object getBean(String key)
	{
		Object bean = map.get(key);
		if(bean == null)
		{
			map.put(key, createBean());
			bean = map.get(key);
		}
		return bean;
	}
	
	public Object createBean()
	{
		//
		return new Object();
	}
}
