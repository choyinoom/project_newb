package com.project.myApplication.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
    @GetMapping("/users/{owner}/{projectName}")
    public String repository(@PathVariable String owner, @PathVariable String projectName, Model model) {
        
    	String viewName = "";
    	try {
    		Project project = projectService.findByOwnerAndName(owner, projectName);
    		log.debug("this is fetch result from repository= {}", project);
    		model.addAttribute(project);
        	
    		List<FileMap> entry = objectStorageService.getEntry(project.getId(), null);
    		model.addAttribute("entry", entry);
        	
        	viewName = viewName + "web/project";
    	} catch (NoSuchElementException e) {
    		viewName = viewName + "error/404";
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
    @GetMapping("/users/{owner}/{projectName}/tree/**")
    public String tree(
    		@PathVariable String owner, 
    		@PathVariable String projectName,
    		HttpServletRequest request,
    		Model model) {
    	
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	model.addAttribute(project);

    	String subPath = getObjectURL(request);
    	List<FileMap> entry = objectStorageService.getEntry(project.getId(), subPath);
    	
    	model.addAttribute("entry", entry);
    	model.addAttribute("subPath", subPath);
    	
    	return "web/tree";
    }
    
    
    /**
     * 파일을 한 줄씩 읽어서 blob view 반환
     */
    @GetMapping("/users/{owner}/{projectName}/blob/**")
    public String blob(@PathVariable String owner, @PathVariable String projectName, HttpServletRequest request, Model model) {
    	
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	model.addAttribute(project);

    	String subPath = getObjectURL(request);

    	List<String> content = objectStorageService.getBlob(project.getId(), subPath);
    	
    	model.addAttribute("project", new Project(owner, projectName));
    	model.addAttribute("content", content);
    	model.addAttribute("subPath", subPath);
    	
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
    
    @GetMapping("/users/{owner}/{projectName}/find")
    public String findFiles(@PathVariable String owner, @PathVariable String projectName, Model model) {
    	
    	Project project = projectService.findByOwnerAndName(owner, projectName);
    	Long repositoryId = project.getId();
    	List<String> fileList = new ArrayList<>();
    	fileList = objectStorageService.findFiles(repositoryId);
    	log.debug("this is file list {}", fileList);
    	model.addAttribute("fileList", fileList);
    	model.addAttribute(project);
    	return "web/finder";
    }
  
    
    private String getObjectURL(HttpServletRequest request) {
    	String 	orgRequest = request.getRequestURI();
    	log.debug("this is requestURI = {}", orgRequest);
    	int fifthSlash = StringUtils.ordinalIndexOf(orgRequest, "/", 5);
    	String processed = orgRequest.substring(fifthSlash);
    	return processed;
    }
    
}
