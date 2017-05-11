package com.wang.model;

import java.io.Serializable;

/**
 * @author wxe
 * @since 1.0.0
 */
@SuppressWarnings("serial")
public class Agent implements Serializable{
	private String id;
	
	private String name;
	
	private String sex;
	
	private String location;
	
	private String loginName;
	
	private String agentNo;
	
	private String creater;
	
	public Agent(){
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getAgentNo() {
		return agentNo;
	}

	public void setAgentNo(String agentNo) {
		this.agentNo = agentNo;
	}

	public String getCreater() {
		return creater;
	}

	public void setCreater(String creater) {
		this.creater = creater;
	}

	public Agent(	String id, 
					String name, 
					String sex, 
					String location,
					String loginName, 
					String agentNo, 
					String creater) {
		super();
		this.id = id;
		this.name = name;
		this.sex = sex;
		this.location = location;
		this.loginName = loginName;
		this.agentNo = agentNo;
		this.creater = creater;
	}

	
}
