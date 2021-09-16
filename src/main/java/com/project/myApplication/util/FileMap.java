package com.project.myApplication.util;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FileMap {

	String name;
	String hash;
	String type;
	String mtime;
	
	@Override
	public String toString() {
		return String.format("%s %s %s %s", type, name, hash, mtime);
		
	}
}
