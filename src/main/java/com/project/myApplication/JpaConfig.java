package com.project.myApplication;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.myApplication.repository.FileMetadataRepository;
import com.project.myApplication.repository.JpaMemberRepository;
import com.project.myApplication.repository.JpaFileMetadataRepository;
import com.project.myApplication.repository.JpaProjectRepository;
import com.project.myApplication.repository.MemberRepository;
import com.project.myApplication.repository.ProjectRepository;
import com.project.myApplication.service.MemberService;
import com.project.myApplication.service.ProjectService;

@Configuration
public class JpaConfig {

	private final DataSource dataSource;
	private final EntityManager em;
	
	public JpaConfig(DataSource dataSource, EntityManager em) {
		this.dataSource = dataSource;
		this.em = em;
	}
	
	@Bean
	public MemberService memberService() {
		return new MemberService(memberRepository());
	}
	
	@Bean
	public MemberRepository memberRepository() {
		return new JpaMemberRepository(em);
	}
	
	@Bean
	public ProjectService projectService() {
		return new ProjectService(projectRepository());
	}
	
	@Bean
	public ProjectRepository projectRepository() {
		return new JpaProjectRepository(em);
	}
	
	@Bean
	public FileMetadataRepository fileMetadataRepository() {
		return new JpaFileMetadataRepository(em);
	}
}
