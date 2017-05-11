package com.wang.service;

import java.util.List;

import com.wang.model.Agent;

/**
 * @author wxe
 * @since 1.0.0
 */
public interface CacheService {
	/**
	 * 将agent放入到agent中
	 * @param agent
	 * @return
	 */
	Agent putInCache(String key,List<Agent> list);
	/**
	 * 根据id去缓存中获取
	 * @param id
	 * @return
	 */
	Agent getByIdFromCache(String id);
	/**
	 * 从缓存中获取
	 * @return
	 */
	List<Agent> getFromCache();

}
