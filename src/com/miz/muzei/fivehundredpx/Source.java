package com.miz.muzei.fivehundredpx;

public class Source {

	private String name, url;

	public Source(String name, String urlName) {
		this.name = name;
		this.url = urlName;
	}

	public String getName() {
		return name;
	}

	public String getUrlName() {
		return url;
	}

	@Override
	public String toString() {
		return getName();
	}
}
