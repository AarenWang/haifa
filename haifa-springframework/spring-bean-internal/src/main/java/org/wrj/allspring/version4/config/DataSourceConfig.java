package org.wrj.allspring.version4.config;

import org.springframework.beans.factory.annotation.Value;

public class DataSourceConfig {
	
	
	@Value("${url}")
	private String url;
	
	@Value("${user_name}")
	private String userName;
	
	
	@Value("${passowrd}")
	private String passowrd;


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String getUserName() {
		return userName;
	}


	public void setUserName(String userName) {
		this.userName = userName;
	}


	public String getPassowrd() {
		return passowrd;
	}


	public void setPassowrd(String passowrd) {
		this.passowrd = passowrd;
	}
	
	
	

}
