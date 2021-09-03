package com.project.myApplication.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.myApplication.PropertiesConfig;
import com.project.myApplication.domain.FileMetadata;
import com.project.myApplication.repository.FileMetadataRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FileSystemStorageService {

	private final String tmpLocation;
	private final FileMetadataRepository fileRepository;
	
	@Autowired
	public FileSystemStorageService(PropertiesConfig config, FileMetadataRepository fileRepository) {
		this.tmpLocation = config.getStorageTmp();
		this.fileRepository = fileRepository;
	}
	
	
	/**
	 * 파일 메타정보를 db에 저장하고, 임시파일은 서버에 저장한다.
	 * @param file
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public FileMetadata store(MultipartFile file, FileMetadata data) throws Exception {
		FileMetadata save = null;
		try {
			if (file.isEmpty()) {
				throw new Exception("EmptyFileException");
			}
			String key = String.valueOf(data.getRepositoryId());
			Path tmpPath = Paths.get(tmpLocation, key);
			Files.createDirectories(tmpPath);
			
			File tmpFile = File.createTempFile("pre", ".tmp", tmpPath.toFile());
			log.debug(tmpFile.getAbsolutePath());
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			
			// db에 저장
			String fileId = tmpFile.getName().substring(3,10);
			data.setFileId(fileId);
			save = fileRepository.save(data);
			
		} catch (IOException e) {
			log.error("Failed to store a file");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return save;
	}
	
	public void deleteById(Long id) {
		fileRepository.deleteById(id);
	}
	
	public List<FileMetadata> findByRespositoryIdAndUploadId(Long repositoryId, Long uploadId) {
		return fileRepository.findByRespositoryIdAndUploadId(repositoryId, uploadId);
	}
}
