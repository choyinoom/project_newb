package com.project.myApplication.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.project.myApplication.domain.FileMetadata;
import com.project.myApplication.domain.Project;
import com.project.myApplication.repository.MemberRepository;
import com.project.myApplication.service.FileMetadataService;
import com.project.myApplication.service.ObjectStorageService;
import com.project.myApplication.service.ProjectService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class FileUploadController {

	private final ProjectService projectService;
	private final FileMetadataService fileMetadataService;
	private final ObjectStorageService objectStorageService;
	
	@Autowired
	private MemberRepository memberRepository;
	
	@Autowired
	public FileUploadController(ProjectService projectService, FileMetadataService fileMetadataService, ObjectStorageService objectStorageService) {
		this.projectService = projectService;
		this.fileMetadataService = fileMetadataService;
		this.objectStorageService = objectStorageService;
	}
	
	
	/**
     * 레파지토리에 새로운 파일을 추가하기 위한 폼을 반환
     * @param owner
     * @param projectName
     * @param model
     * @return
     */
    @GetMapping("/users/{owner}/{projectName}/upload")
    public String uploadForm(@PathVariable String owner, @PathVariable String projectName, Model model) {
    	
    	try {
    		Project project = projectService.findByOwnerAndName(owner, projectName);
    		model.addAttribute("uploadId", System.currentTimeMillis());
        	model.addAttribute("project", project);
    	} catch (NoSuchElementException nse) {
    		return "error/404";
    	} catch (Exception e) {
    		log.error("Unexpected Error from ProjectController: uploadForm", e);
    	}
    	
    	return "web/uploadForm";
    }
    
    
    /**
     * 레파지토리에 새로운 파일을 추가하기 위한 post 요청 처리
     * @param owner
     * @param projectName
     * @param file
     * @param path
     * @return
     */
    @PostMapping("/upload-files")
    public ResponseEntity<FileMetadata> uploadFiles(@RequestParam MultipartFile file, FileMetadata formData) {

    	try {
    		FileMetadata data = fileMetadataService.store(file, formData);
    		return new ResponseEntity<>(data, HttpStatus.CREATED);
		} catch (Exception e) {
			if(e.getMessage().equals("EmtpyFileException")) {
				log.error("empty file Exception");
			}
			e.printStackTrace();
			return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		}
    }
    
    /**
     * 커밋에 추가하지 않을 파일에 대한 DELETE 요청 처리
     * @param fileId
     * @return
     */
    @DeleteMapping("/users/{owner}/{projetName}/upload")
    public ResponseEntity<Object> deleteFiles(@RequestParam("fileId") String fileId) {
    	
    	try {
    		Long id = Long.valueOf(fileId);
        	fileMetadataService.deleteById(id);
        	return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    	} catch (Exception e) {
    		
    		return new ResponseEntity<>("System is broken.",HttpStatus.CONFLICT);
    	}
    }
    
    /**
     * 커밋 내용에 대한 POST 요청 처리
     */
    @PostMapping("/users/{owner}/{projectName}/upload")
    public ResponseEntity<Object> commit(@PathVariable String owner, 
    		@PathVariable String projectName,
    		@RequestParam Map<String, String> paramMap) {
    	
    	String email = memberRepository
    			.findByUsername(owner)
    			.get()
    			.getEmail();
    	paramMap.put("email", email);

    	Long repositoryId = Long.valueOf(paramMap.get("repositoryId"));
    	Long uploadId = Long.valueOf(paramMap.get("uploadId"));
    	List<FileMetadata> list = fileMetadataService.findByRespositoryIdAndUploadId(repositoryId, uploadId);
    	
    	ResponseEntity<Object> response = null;
    	try {
    		objectStorageService.store(list, paramMap);
    		response = new ResponseEntity<Object>(null, HttpStatus.CREATED);
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    		response = new ResponseEntity<Object>(null,HttpStatus.BAD_REQUEST);
    	}
    	
    	return response;
    	
    }
    
    
}
