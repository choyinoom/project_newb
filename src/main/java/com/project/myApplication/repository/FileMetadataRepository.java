package com.project.myApplication.repository;

import java.util.List;
import java.util.Optional;

import com.project.myApplication.domain.FileMetadata;

public interface FileMetadataRepository {

	FileMetadata save(FileMetadata file);
	Optional<FileMetadata> findById(Long id);
	List<FileMetadata> findByRespositoryIdAndUploadId(Long repositoryId, Long uploadId);
	void deleteById(Long id);
}
