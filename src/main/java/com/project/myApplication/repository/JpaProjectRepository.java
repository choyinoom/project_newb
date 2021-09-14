package com.project.myApplication.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.project.myApplication.domain.Project;

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

	@Override
	public void updateUpdateTime(Long id, LocalDateTime local) {
		Project projectToUpdate = findById(id).get();
		projectToUpdate.setUpdateTime(local);
		save(projectToUpdate);
	}

	@Override
	public List<Project> findByQuery(String owner, String q, String type) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Project> cq = cb.createQuery(Project.class);
		
		Root<Project> project = cq.from(Project.class);
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(cb.equal(project.get("owner"), owner));
		
		if(!q.equals("")) {
			predicates.add(cb.like(cb.upper(project.get("name")), "%" + q.toUpperCase() + "%"));
		}
		if (!type.equals("")) {
			predicates.add(cb.equal(project.get("visibility"), type));
		}
		cq.where(predicates.toArray(new Predicate[0]));
		
		return em.createQuery(cq).getResultList();
	}
	
}
