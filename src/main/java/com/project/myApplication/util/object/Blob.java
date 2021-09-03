package com.project.myApplication.util.object;

public class Blob implements GitObject{

	private static final long serialVersionUID = 9099619072272624542L;
	private final String hash;
	private final int size;
	
	public Blob(String hash, int size) {
		this.hash = hash;
		this.size = size;
	}
	
	@Override
	public int getSize() {
		return size;
	}

	@Override
	public String getHash() {
		return hash;
	}

}
