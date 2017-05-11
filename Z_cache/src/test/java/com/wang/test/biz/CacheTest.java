package com.wang.test.biz;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.wang.common.Constants;
import com.wang.model.Agent;
import com.wang.service.CacheService;
import com.wang.test.SpringTester;

/**
 * @author wxe
 * @since 1.0.0
 */
public class CacheTest extends SpringTester{
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(10);
	
	@Autowired
	private CacheService cacheService;
	
	@Test
	public void testProcess() throws InterruptedException{
		Agent agent = new Agent();
		agent.setAgentNo("123");
		
		List<Agent> list = Lists.newArrayList();
		list.add(agent);
		cacheService.putInCache(Constants.AGENT_CACHE_KEY,list);
		Thread.sleep(300);
		cacheService.getFromCache();
	}
	
	@Test
	public void testCache() throws InterruptedException{
		Agent agent = new Agent();
		agent.setAgentNo("123");
		
		List<Agent> list = Lists.newArrayList();
		list.add(agent);
		cacheService.putInCache(Constants.AGENT_CACHE_KEY,list);
		Thread.sleep(300);
		cacheService.getFromCache();
	}

}
