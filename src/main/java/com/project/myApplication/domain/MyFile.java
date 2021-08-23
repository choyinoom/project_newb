package com.project.myApplication.domain;

import java.sql.Blob;

import lombok.Data;

@Data
public class MyFile {

	private String uuid;
	private String name;
	private String path;
	private Long size;
	private String date;
	private String status; // add or delete
	
	private String commitId; // link to commit
}
