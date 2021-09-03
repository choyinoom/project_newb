package com.project.myApplication.domain;

import javax.persistence.Entity;
import javax.persistence.GenerationType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;
import lombok.NonNull;

@Data
@Entity
public class Project {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NonNull private String owner;
	@NonNull private String name;
	private String description;
	@NonNull private String visibility;
	
	private String createTime;
	private String updateTime;
	
	public Project() {
		
	}
	
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
