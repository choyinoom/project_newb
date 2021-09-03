package com.project.myApplication.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping("/users/{username}")
    public String Main(@PathVariable String username, Model model) {
         List<Project> repositories = projectService.findByOwner(username);
         
         if (repositories.size() > 0) {
        	 model.addAttribute("repos", repositories);
         }
  
         model.addAttribute("owner", username);
         
         return "web/repositories";
    }
}
