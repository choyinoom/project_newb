package com.project.myApplication.domain.member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicInsert;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "MEMBERS")
@Getter
@DynamicInsert
public class Member {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String username;
	private String password;
	
	private String email;
	private Boolean enabled;

	@Builder
	public Member(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email = email;
	}
	

}
