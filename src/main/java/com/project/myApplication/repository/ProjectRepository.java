package com.project.myApplication.repository;

import java.util.List;
import java.util.Optional;

import com.project.myApplication.domain.Project;

public interface ProjectRepository {

	Project save(Project project);
	Optional<Project> findById(Long id);
	List<Project> findByOwner(String owner);
	Optional<Project> findByOwnerAndName(String owner, String name);
}
