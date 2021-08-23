package com.project.myApplication.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.project.myApplication.domain.Project;

@Repository
public class ProjectRepository {

	private static Map<String, Map<String, Project>> store = new ConcurrentHashMap<>();
	private static AtomicLong sequence = new AtomicLong(0);
	
	public Project save(Project project) {
		project.setId(sequence.getAndIncrement());
		String owner = project.getOwner();
		Map<String, Project> projects = findByOwner(owner); // Map<������Ʈ �̸�, ������Ʈ>
		
		if(projects == null) {
			projects = new HashMap<>();
		}
		projects.put(project.getName(), project);
		store.put(owner, projects);
		return project;
	}
	
	
	public Map<String, Project> findByOwner(String owner) {
		return store.get(owner);
	}
	
	
	public Project findByName(String owner, String name) {
		Map<String, Project> map = store.get(owner);
		Project project = map.get(name);
		return project;
	}
	
	public AtomicLong getSequence() {
		return sequence;
	}
	
	public void clearStore() {
        store.clear();
    }
}
