package com.tm.demo.springboot.model;

public class Vehicle {
	private String vehiclenumber;
	
	private String brand;
	
	private String country;
	
	private String modelname;
	
	private String modelyear;

	public String getVehiclenumber() {
		return vehiclenumber;
	}

	public void setVehiclenumber(String vehiclenumber) {
		this.vehiclenumber = vehiclenumber;
	}

	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getModelname() {
		return modelname;
	}
	public void setModelname(String modelname) {
		this.modelname = modelname;
	}
	public String getModelyear() {
		return modelyear;
	}
	public void setModelyear(String modelyear) {
		this.modelyear = modelyear;
	}

}
