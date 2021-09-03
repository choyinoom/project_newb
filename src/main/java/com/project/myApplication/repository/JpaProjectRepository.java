package com.project.myApplication.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import com.project.myApplication.domain.Project;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JpaProjectRepository implements ProjectRepository{

	private final EntityManager em;
	
	public JpaProjectRepository(EntityManager em) {
		this.em = em;
	}
	
	@Override
	public Project save(Project project) {
		em.persist(project);
		return project;
	}

	@Override
	public Optional<Project> findById(Long id) {
		Project project = em.find(Project.class, id);
		return Optional.ofNullable(project);
	}

	@Override
	public List<Project> findByOwner(String owner) {
		List<Project> result = em.createQuery("select p from Project p where p.owner = :owner", Project.class)
				.setParameter("owner", owner)
				.getResultList();
		return result;
	}

	@Override
	public Optional<Project> findByOwnerAndName(String owner, String name) {
		List<Project> result = em.createQuery("SELECT p FROM Project p WHERE p.owner = ?1 and p.name = ?2", Project.class)
				.setParameter(1, owner)
				.setParameter(2, name)
				.getResultList();
		return result.stream().findAny();
	}

}
