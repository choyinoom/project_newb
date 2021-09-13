package com.project.myApplication.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.myApplication.domain.Project;
import com.project.myApplication.service.ObjectStorageService;
import com.project.myApplication.service.ProjectService;
import com.project.myApplication.util.FileMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class ProjectController {


	private final ProjectService projectService;
	private final ObjectStorageService objectStorageService;
	
	@Autowired
	public ProjectController(ProjectService projectService, ObjectStorageService objectStorageService) {
		this.projectService = projectService;
		this.objectStorageService = objectStorageService;
	}
	
    
    /**
     * 레파지토리 엔트리 페이지 view 반환
     * @param owner
     * @param projectName
     * @param model
     * @return
     */
    @GetMapping({"/users/{owner}/{projectName}", "/users/{owner}/{projectName}/tree/{entry}"})
    public String repository(
    		@PathVariable String owner,
    		@PathVariable String projectName, 
    		@PathVariable Optional<String> entry, Model model) {
        
    	String viewName = "";
    	try {
    		Project project = projectService.findByOwnerAndName(owner, projectName);
    		model.addAttribute(project);
        	
    		Long repositoryId = project.getId();
    		String branch = entry.orElse("main");
    		List<FileMap> list = objectStorageService.getEntry(repositoryId, branch, "/");
    		if(list.size() > 0) {
    			model = setTableHeader(repositoryId, branch, model);    	
    		}
    		String location = String.format("/users/%s/%s/tree/%s", owner, projectName, branch);
    		model.addAttribute("branch", branch);
    		model.addAttribute("location",location);
    		model.addAttribute("entry", list);
    		viewName += "web/project";
    	} catch (NoSuchElementException e) {
    		viewName += "error/404";
    	} catch (Exception e) {
    		log.error("unknown error", e);
    	}

        return viewName;
    }
    

    /**
     * 레파지토리의 tree view 반환
     * @param owner
     * @param projectName
     * @param request 요청이 들어온 URL을 그대로 반환하기 위해 필요
     * @param model
     */
    @GetMapping("/users/{owner}/{projectName}/tree/{branch}/{entry}/**")
    public String tree(
    		@PathVariable String owner, 
    		@PathVariable String projectName,
    		@PathVariable String branch,
    		HttpServletRequest request,
    		Model model) {
    	log.debug("tree에 들어왔다!");
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	model.addAttribute(project);

    	String subPath = getObjectURL(request);
    	List<FileMap> entry = objectStorageService.getEntry(project.getId(), branch, subPath);
    	
    	model.addAttribute("branch", branch);
    	model.addAttribute("entry", entry);
    	model.addAttribute("location", decode(request.getRequestURI()));
    	model.addAttribute("detail", true);
    	model = setTableHeader(project.getId(), branch, model);
    	
    	return "web/tree";
    }
    
    
    /**
     * 파일을 한 줄씩 읽어서 blob view 반환
     */
    @GetMapping("/users/{owner}/{projectName}/blob/{branch}/**")
    public String blob(
    		@PathVariable String owner, 
    		@PathVariable String projectName, 
    		@PathVariable String branch,
    		HttpServletRequest request, Model model) {
    	
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	model.addAttribute(project);

    	String subPath = getObjectURL(request);

    	List<String> content = objectStorageService.getBlob(project.getId(), branch, subPath);
    	
    	model.addAttribute("branch", branch);
    	model.addAttribute("content", content);
    	model.addAttribute("location", decode(request.getRequestURI()));
    	model.addAttribute("detail", true);
    	
    	return "web/blob";
    }
    
    
    /**
     * 새 레파지토리 생성을 위한 view 반환
     * @return "web/new"
     */
    @GetMapping("/new")
    public ModelAndView createForm() {
        ModelAndView mv = new ModelAndView("web/new");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        mv.addObject("username", username);
        mv.addObject("status", "ready");
        return mv;
    } 

    
    /**
     * 새 레파지토리를 생성하기 위한 post 요청 처리
     * @param project
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/new")
    public String create(Project project, RedirectAttributes redirectAttributes) {
        
    	try {
    		Long id = projectService.add(project);
    		objectStorageService.init(id);
            redirectAttributes.addAttribute("projectName", project.getName());
            redirectAttributes.addAttribute("owner",project.getOwner());
            redirectAttributes.addAttribute("status", true);
            return "redirect:/users/{owner}/{projectName}";
    	} catch (IllegalStateException e) {
    		redirectAttributes.addAttribute("status", false);
    		return "redirect:/new";
    	}
    }
    
    @GetMapping("/users/{owner}/{projectName}/find/{branch}")
    public String findFiles(
    		@PathVariable String owner, 
    		@PathVariable String projectName, 
    		@PathVariable String branch,
    		Model model) {
    	
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	Long repositoryId = project.getId();
    	List<String> fileList = new ArrayList<>();
    	fileList = objectStorageService.findFiles(repositoryId, branch);
    	model.addAttribute("fileList", fileList);
    	model.addAttribute(branch);
    	model.addAttribute(project);
    	return "web/finder";
    }
  
    /**
     * 커밋 히스토리를 시간 순으로 나열
     * @param owner
     * @param projectName
     * @param model
     * @return
     */
    @GetMapping({"/users/{owner}/{projectName}/commits", "/users/{owner}/{projectName}/commits/{branch}"}) 
    public String commits(
    		@PathVariable String owner, 
    		@PathVariable String projectName, 
    		@PathVariable Optional<String> branch, Model model){
    	
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	Long repositoryId = project.getId();
    	String commit = branch.orElse("main");
    	List<Map<String, String>> commitHistory = objectStorageService.getCommitHistory(repositoryId, commit);
    	Collections.reverse(commitHistory);
    	
    	model.addAttribute(project);
    	model.addAttribute("commitHistory", commitHistory);
    	log.debug("commit history =  {}", commitHistory);
    	return "web/commits";
    }
    
    private Model setTableHeader(Long repositoryId, String branch, Model model) {
    	List<Map<String, String>> commitHistory = objectStorageService.getCommitHistory(repositoryId, branch);
		int count = commitHistory.size();
		Map<String, String> lastCommit = commitHistory.get(count-1);
		model.addAttribute("commit", lastCommit);
		model.addAttribute("count", count);
    	return model;
    }
    
    
    private String getObjectURL(HttpServletRequest request) {
    	String 	orgRequest = request.getRequestURI();
    	log.debug("this is requestURI = {}", orgRequest);
    	int fifthSlash = StringUtils.ordinalIndexOf(orgRequest, "/", 6);
    	String processed = orgRequest.substring(fifthSlash);
    	log.debug("this is processedURI = {}", processed);
    	return decode(processed);
    }
    
    private String decode(String uri) {
    	return URLDecoder.decode(uri,StandardCharsets.UTF_8);
    }
    
}
