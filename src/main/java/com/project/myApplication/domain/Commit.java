package com.project.myApplication.domain;

import lombok.Data;

@Data
public class Commit {

	private Long id;
	private String hash;
	private String message;
	private String description;
	
	private Long repositoryId;
}
