package com.project.myApplication.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project.myApplication.domain.Project;
import com.project.myApplication.repository.ProjectRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MemberController {

    @Autowired
    private ProjectRepository projectRepository;

    @PostConstruct 
    public void init() {
    	projectRepository.save(new Project("choyi", "first-project", "this is awesome", "public"));
    	projectRepository.save(new Project("choyi", "second-project", "this is magnificent", "public"));
    	projectRepository.save(new Project("choyi", "third-project", "this is good", "public"));
    	log.debug("Test data is ready={}",projectRepository.getSequence());
    }
    
    @GetMapping({ "/", "/home" })
    public String home() {
         return "home";
    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request) {
         return "login";
    }

    @GetMapping("/users/{username}")
    public String Main(@PathVariable String username, Model model) {
         Map<String, Project> repositoriesMap = projectRepository.findByOwner(username);
         
         if (repositoriesMap != null) {
        	 Collection<Project> projects = repositoriesMap.values();
        	 log.debug("status={}",projects);
        	 model.addAttribute("repos", projects);
         }
  
         model.addAttribute("owner", username);
         
         return "web/repositories";
    }
}
