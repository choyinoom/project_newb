package com.project.myApplication.domain;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import lombok.Data;

@Data
@Entity

public class FileMetadata {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String fileId;
	private Long repositoryId;
	private Long uploadId;
	private String name;
	private Long size;
	private String contentType;
	private String directory;
	
	public FileMetadata() {
		
	}
}
