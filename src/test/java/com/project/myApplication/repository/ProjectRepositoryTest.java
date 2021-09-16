package com.project.myApplication.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.myApplication.domain.Project;

@SpringBootTest
@Transactional
public class ProjectRepositoryTest {

	@Autowired ProjectRepository projectRepository;
	
	
	@Test
	void 시간업데이트테스트() {
		//given
		LocalDateTime now = LocalDateTime.now();
		//when
		projectRepository.updateUpdateTime(2L, now);
		
		//then
		Project project = projectRepository.findById(2L).get();
		assertEquals(project.getUpdateTime(), now);
	}
}
