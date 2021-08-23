package com.project.myApplication.domain;

import lombok.Data;
import lombok.NonNull;

@Data
public class Project {

	private Long id;
	private String hash;
	@NonNull private String owner;
	@NonNull private String name;
	private String description;
	@NonNull private String visibility;
	
	private String createTime;
	private String updatedTime;
	
	public Project(String owner, String name, String description, String visibility) {
		this.owner = owner;
		this.name = name;
		this.description = description;
		this.visibility = visibility;
	}
	
	public Project(String owner, String name) {
		this.owner = owner;
		this.name = name;
	}
	
	
}
