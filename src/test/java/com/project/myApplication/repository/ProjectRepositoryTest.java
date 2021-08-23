package com.project.myApplication.repository;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.myApplication.domain.Project;
import static org.assertj.core.api.Assertions.assertThat;
public class ProjectRepositoryTest {

	ProjectRepository projectRepository = new ProjectRepository();
	
	@AfterEach
	void afterEach() {
		projectRepository.clearStore();
	}
	
	@Test
	void save() {
		//given
		Project project = new Project("user", "first project", "this is awesome", "public");
		
		//when
		Project savedProject = projectRepository.save(project);
		
		//then
		Map<String, Project> projectMap = projectRepository.findByOwner("user");
		Project find = projectMap.get("first project");
		assertThat(find).isEqualTo(savedProject);
	}
}
