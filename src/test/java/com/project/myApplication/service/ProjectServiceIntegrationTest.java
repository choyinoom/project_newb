package com.project.myApplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.project.myApplication.domain.Project;
import com.project.myApplication.repository.ProjectRepository;

@SpringBootTest
@Transactional
public class ProjectServiceIntegrationTest {

	@Autowired ProjectService projectService;
	@Autowired ProjectRepository projectRepository;
	
	@Test
	public void 레포지토리추가() throws Exception {
		//Given
		Project project = new Project();
		project.setOwner("ruby");
		project.setName("Java Application For Newbie");
		
		//When
		Long saveId = projectService.add(project);
		
		//Then
		Project findProject = projectRepository.findById(saveId).get();
		assertEquals(project.getName(), findProject.getName());
	}
	
	@Test
	public void 중복_레포지토리_예외() throws Exception {
		//Given
		Project project1 = new Project();
		project1.setOwner("ruby");
		project1.setName("Kotlin Application");
		
		Project project2 = new Project();
		project2.setOwner("ruby");
		project2.setName("Kotlin Application");
		
		//When
		projectService.add(project1);
		IllegalStateException e = assertThrows(IllegalStateException.class, 
				() -> projectService.add(project2));
		//Then
		assertThat(e.getMessage()).isEqualTo("이미 존재하는 프로젝트 이름입니다.");
	}
}
