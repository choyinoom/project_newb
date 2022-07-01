package com.project.myApplication.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.myApplication.domain.Project;
import com.project.myApplication.service.MemberService;
import com.project.myApplication.service.ProjectService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MemberController {

    private final MemberService memberService;
	private final ProjectService projectService;
    
    @Autowired
    public MemberController(MemberService memberService, ProjectService projectService) {
    	this.memberService = memberService;
    	this.projectService = projectService;
    }
       
    @GetMapping({ "/", "/home" })
    public String home() {
         return "home";
    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request) {
         return "login";
    }
    
    @GetMapping("/hello")
    public String hello(HttpServletRequest request) {
    	return "hello";
    }

    @RequestMapping(
    		value = "/users/{username}", 
    		params = {"q", "type", "sort"}, 
    		method= RequestMethod.GET)
    public String MainQuery(
    		@PathVariable String username,
    		@RequestParam(defaultValue="") String q,
    		@RequestParam(defaultValue="") String type,
    		@RequestParam(defaultValue="") String sort,
    		Model model) {
    	log.debug("q:{}, type:{}, sort:{}", q, type, sort);
    	List<Project> repositories = projectService.findByQuery(username, q, type, sort);
    	
        model.addAttribute("repos", repositories);
    	model.addAttribute("type", type);
    	model.addAttribute("sort", sort);
    	model.addAttribute("owner", username);
    	return "fragments/projectList";
    }
    
    @GetMapping("/users/{username}")
    public String Main(@PathVariable String username, Model model) {
    	
    	List<Project> repositories = projectService.findByOwner(username);
    	Collections.sort(repositories);
    	model.addAttribute("owner", username);
        model.addAttribute("repos", repositories);
        model.addAttribute("type", "");
        model.addAttribute("sort", "");
         
         return "web/repositories";
    }
    
}
