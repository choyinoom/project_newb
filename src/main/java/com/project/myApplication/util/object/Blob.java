package com.project.myApplication.util.object;

public class Blob implements GitObject{

	private static final long serialVersionUID = 9099619072272624542L;
	private final String hash;
	private final int size;
	private String ctime;
	private String mtime;
	
	public Blob(String hash, int size, String ctime) {
		this.hash = hash;
		this.size = size;
		this.ctime = ctime;
		this.mtime = ctime;
	}
	
	public Blob(String hash, int size) {
		this.hash = hash;
		this.size = size;
	}
	
	public String getCtime() {
		return ctime;
	}
	
	public void setMtime(String mtime) {
		this.mtime = mtime;
	}
	
	@Override
	public int getSize() {
		return size;
	}

	@Override
	public String getHash() {
		return hash;
	}
	
	@Override
	public String getType() {
		return "blob";
	}
	
	@Override
	public String getMtime() {
		return mtime;
	}
}

