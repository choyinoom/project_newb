package com.project.myApplication.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import com.project.myApplication.domain.FileMetadata;

public class JpaFileMetadataRepository implements FileMetadataRepository{

	private final EntityManager em;
	
	public JpaFileMetadataRepository(EntityManager em) {
		this.em = em;
	}
	
	@Override
	public FileMetadata save(FileMetadata file) {
		em.persist(file);
		return file;
	}
	
	@Override
	public Optional<FileMetadata> findById(Long id) {
		FileMetadata file = em.find(FileMetadata.class, id);
		return Optional.ofNullable(file);
	}
	
	@Override
	public List<FileMetadata> findByRespositoryIdAndUploadId(Long repositoryId, Long uploadId) {
		List<FileMetadata> result = em.createQuery("SELECT f FROM FileMetadata f WHERE f.repositoryId = ?1 and f.uploadId =  ?2", FileMetadata.class)
				.setParameter(1, repositoryId)
				.setParameter(2, uploadId)
				.getResultList();
		
		return result;
	}

	@Override
	public void deleteById(Long id) {
		delete(findById(id).get());
	}
	
	public void delete(FileMetadata file) {
		em.remove(file);
	}
	
}
