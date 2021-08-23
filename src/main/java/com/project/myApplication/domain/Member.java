package com.project.myApplication.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Member {

	private Long id;
	private String email;
	private String username;
	private String password;
}
