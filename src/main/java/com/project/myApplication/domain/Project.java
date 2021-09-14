package com.project.myApplication.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.NonNull;

@Data
@Entity
public class Project implements Comparable<Project>{

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NonNull private String owner;
	@NonNull private String name;
	private String description;
	@NonNull private String visibility;
	
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
	
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
	
	@Override
	public int compareTo(Project o) {
		return o.getUpdateTime().compareTo(this.getUpdateTime());
	}
	
}
