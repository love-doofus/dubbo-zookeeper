package com.wang.service.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.wang.common.Constants;
import com.wang.common.utils.JsonUtils;
import com.wang.model.Agent;
import com.wang.service.CacheService;

/**
 * @author wxe
 * @since 1.0.0
 */
@Service
public class CacheServiceImp implements CacheService{
	//没有回源策略的缓存
	private static  Cache<String, List<Agent>> AGENT_CACHE = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MICROSECONDS)
															.maximumSize(5).build();
	//具有回源策略的缓存
	private static  LoadingCache<String, List<Agent>> AGENT_CACHE_DATA = CacheBuilder.newBuilder().expireAfterWrite(200, TimeUnit.MILLISECONDS)
																 .maximumSize(5).build(
																		 new CacheLoader<String,  List<Agent>>(){

																			@Override
																			public List<Agent> load(String key) throws Exception {
																				//数据库中获取
																				
																				return getFromDatabase(key);
																			}
																			 
																			/**
																			 * 从数据库中读取所有数据
																			 * @return
																			 */
																			public List<Agent> getFromDatabase(String key){
																				System.out.println("数据回源啦。。。。。。。");
																				//数据只是用来测试
																				List<Agent> list = Lists.newArrayList();
																				Agent agent1 = new Agent("1", 
																										"wang",
																										"1",
																										"陕西",
																										null,
																										null,
																										null);
																				list.add(agent1);
																				return list;
																			}
																		 }
																  );

	@Override
	public Agent putInCache(String key,List<Agent> list) {
//		//-----------------------------------------
//		AGENT_CACHE.put(key, list);
//		System.out.println(" AGENT_CACHE添加缓存："+JsonUtils.toJson(list));
		//----------------------------
		AGENT_CACHE_DATA.put(key, list);
		System.out.println(" AGENT_CACHE_DATA添加缓存："+JsonUtils.toJson(list));
		return null;
	}

	@Override
	public Agent getByIdFromCache(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Agent> getFromCache() {
		 List<Agent> result = null;
		 try {
			result = getAgentCacheData();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return result;
	}
	
	public List<Agent> getAgentCache(){
		List<Agent> list = AGENT_CACHE.getIfPresent(Constants.AGENT_CACHE_KEY);
		ImmutableList<Agent> imlist = null;
		if (list != null) {
			imlist = ImmutableList.copyOf(list);
		}
		System.out.println(" AGENT_CACHE获取缓存："+JsonUtils.toJson(imlist));
		return imlist;
	}
	/**
	 * 过期自动数据回源
	 * @return
	 * @throws ExecutionException
	 */
	public List<Agent> getAgentCacheData() throws ExecutionException{
		List<Agent> list = AGENT_CACHE_DATA.get(Constants.AGENT_CACHE_KEY);
		ImmutableList<Agent> imList = null;
		if (list != null) {
			imList = ImmutableList.copyOf(list);
		}
		
		System.out.println(" AGENT_CACHE_DATA获取缓存："+JsonUtils.toJson(imList));
		return imList ;
	}

	
}
