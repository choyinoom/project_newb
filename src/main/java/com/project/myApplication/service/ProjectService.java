package com.project.myApplication.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;

import com.project.myApplication.domain.Project;
import com.project.myApplication.repository.ProjectRepository;

@Transactional
public class ProjectService {

	private final ProjectRepository projectRepository;
	
	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}
	
	public Long add(Project project) {
		validateDuplicateProject(project);
		projectRepository.save(project);
		return project.getId();
		
	}
	
	private void validateDuplicateProject(Project project) {
		try {
			findByOwnerAndName(project.getOwner(), project.getName());
			throw new IllegalStateException("이미 존재하는 프로젝트 이름입니다.");
		} catch(NoSuchElementException nse) {
			// do nothing;
		}
	}
	
	public List<Project> findByOwner(String owner) {
		return projectRepository.findByOwner(owner);
	}
	
	public Project findByOwnerAndName(String owner, String name) {
		return projectRepository.findByOwnerAndName(owner, name)
				.orElseThrow(() -> new NoSuchElementException("존재하지 않는 레포지토리입니다."));
	}
	
}
