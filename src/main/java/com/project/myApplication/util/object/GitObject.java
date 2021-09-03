package com.project.myApplication.util.object;

import java.io.Serializable;

public interface GitObject extends Serializable{
	public int getSize();
	public String getHash();
}
