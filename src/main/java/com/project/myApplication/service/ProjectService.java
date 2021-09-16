package com.project.myApplication.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;

import com.project.myApplication.domain.Project;
import com.project.myApplication.repository.ProjectRepository;
import com.project.myApplication.util.Util;

@Transactional
public class ProjectService {

	private final ProjectRepository projectRepository;

	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	public Long add(Project project) {
		validateDuplicateProject(project);
		Instant now = Util.getInstance().getTime().toInstant();
		LocalDateTime local = LocalDateTime.ofInstant(now, ZoneId.of("Asia/Seoul"));
		project.setCreateTime(local);
		project.setUpdateTime(local);
		projectRepository.save(project);
		return project.getId();

	}

	private void validateDuplicateProject(Project project) {
		try {
			findByOwnerAndName(project.getOwner(), project.getName());
			throw new IllegalStateException("이미 존재하는 프로젝트 이름입니다.");
		} catch (NoSuchElementException nse) {
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

	public void updateProjectUpdateTime(Long id, ZonedDateTime now) {
		Instant instant = now.toInstant();
		LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
		projectRepository.updateUpdateTime(id, local);
	}

	public List<Project> findByQuery(String owner, String q, String type, String sort) {
		List<Project> result = (List<Project>) projectRepository.findByQuery(owner, q, type);
		if (result.size() > 0) {
			sort(result, sort);
		}
		return result;
	}
	
	private void sort(List<Project> repositories, String sort) {
		switch (sort) {
		case "":
			Collections.sort(repositories);
			break;
		case "Name":
			Comparator<Project> byName = (Project project1, Project project2) -> project1.getName()
					.compareTo(project2.getName());
			Collections.sort(repositories, byName);
			break;
		}
	}

}
