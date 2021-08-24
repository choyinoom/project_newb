package com.project.myApplication.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.myApplication.domain.MyFile;
import com.project.myApplication.domain.Project;
import com.project.myApplication.repository.ProjectRepository;
import com.project.myApplication.service.ObjectStorageService;
import com.project.myApplication.service.ProjectService;
import com.project.myApplication.util.FileMap;
import com.project.myApplication.util.FileUtil;

import com.project.myApplication.PropertiesConfig;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

@Slf4j
@Controller
public class ProjectController {


	private final ProjectService projectService;
	private final ObjectStorageService objectStorageService;
	private final PropertiesConfig propertiesConfig;
	
	@Autowired
	public ProjectController(ProjectService projectService, ObjectStorageService objectStorageService, PropertiesConfig propertiesConfig) {
		this.projectService = projectService;
		this.objectStorageService = objectStorageService;
		this.propertiesConfig = propertiesConfig;
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
    		String path = getProjectRootURL(owner, projectName);
        	List<FileMap> entry = FileUtil.getInstance().listDirectory(path);	
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

    	String root = getProjectRootURL(owner, projectName);
    	String subPath = getObjectURL(request);
    	List<FileMap> entry = FileUtil.getInstance().listDirectory(root + "/" + subPath);
    	
    	model.addAttribute("entry", entry);
    	model.addAttribute("subPath", subPath);
    	
    	return "web/tree";
    }
    
    
    /**
     * 파일을 한 줄씩 읽어서 blob view 반환
     */
    @GetMapping("/users/{owner}/{projectName}/blob/**")
    public String blob(@PathVariable String owner, @PathVariable String projectName, HttpServletRequest request, Model model) {
    	
    	String root = getProjectRootURL(owner, projectName);
    	String subPath = getObjectURL(request);

    	List<String> content = FileUtil.getInstance().readFile(root + "/" + subPath);
    	
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
            redirectAttributes.addAttribute("projectName", project.getName());
            redirectAttributes.addAttribute("owner",project.getOwner());
            redirectAttributes.addAttribute("status", true);
            return "redirect:/users/{owner}/{projectName}";
    	} catch (IllegalStateException e) {
    		redirectAttributes.addAttribute("status", false);
    		return "redirect:/new";
    	}
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
    	model.addAttribute("upload-id", System.currentTimeMillis());
    	model.addAttribute(owner);
    	model.addAttribute(projectName);
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
    @PostMapping("/users/{owner}/{projectName}/upload")
    @ResponseBody
    public String uploadFiles(
    		@PathVariable String owner, 
    		@PathVariable String projectName, 
    		@RequestParam MultipartFile file,
    		@RequestParam("full_path") String path) {
    	

    	try {

    		log.debug("Uploaded file Name={}, {}, {}, {}" ,
					file.getOriginalFilename(),
					file.getSize(),
					file.getBytes(),
					path);
    		Map<String, Object> map = new HashMap<>();
    		map.put("owner", owner);
    		map.put("projectName", projectName);
    		map.put("path", path);
    		map.put("file", file);
    		objectStorageService.save(map);
    		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return "ok";
    }
    
    @DeleteMapping("/users/{owner}/{projetName}/upload")
    @ResponseBody
    public String deleteFiles(
    		@PathVariable String owner,
    		@PathVariable String projectName,
    		@RequestParam("file_id") String id) {
    	log.debug("Please remove this file from server ={}", id);
    	return "ok";
    }
    
    private String getProjectRootURL(String owner, String projectName) {
    	final String prefix = propertiesConfig.getStorageRoot();
    	return String.format(prefix+"%s/%s/", owner, projectName);
    }
    
    private String getObjectURL(HttpServletRequest request) {
    	String 	orgRequest = request.getRequestURI();
    	log.debug("this is requestURI = {}", orgRequest);
    	int fifthSlash = StringUtils.ordinalIndexOf(orgRequest, "/", 5);
    	String processed = orgRequest.substring(fifthSlash+1);
    	return processed;
    }
    	
    
    @GetMapping("/test")
    public ModelAndView test(HttpServletRequest request, HttpServletResponse respsonse) {
    	ModelAndView model = new ModelAndView();
    	model.addObject("this", "test");
    	model.addObject("entry", "entry");
    	return model;
    }
}
