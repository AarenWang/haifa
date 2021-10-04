package org.wrj.haifa;

import java.io.Serializable;

public class UserInfo implements Serializable {
	/**
  * 
  */
	private static final long serialVersionUID = 1L;
	private String name;
	private String address;
	private double age;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public double getAge() {
		return age;
	}

	public void setAge(double age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}