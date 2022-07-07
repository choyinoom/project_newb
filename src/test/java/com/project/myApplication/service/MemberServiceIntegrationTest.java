package com.project.myApplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.project.myApplication.domain.member.Member;
import com.project.myApplication.repository.MemberRepository;

@SpringBootTest
@Transactional  
public class MemberServiceIntegrationTest {

	@Autowired MemberService memberService;
	@Autowired MemberRepository memberRepository;
	
	@Test
	public void 회원가입() throws Exception {
		
		//Given
		Member member = Member.builder()
            .username("delinesa")
            .password("1234")
            .email("delinesa@gmail.com")
            .build();
		
		//When
		Long saveId = memberService.join(member);
		
		//Then
		Member findMember = memberRepository.findById(saveId).get();
		assertEquals(member.getUsername(), findMember.getUsername());
	}
	
	@Test
	public void 중복_회원_예외() throws Exception {
		
		//Given
		Member member1 = Member.builder()
            .username("delinesa")
            .password("1234")
            .email("delinesa@gmail.com")
            .build();
		
            Member member2 = Member.builder()
            .username("delinesa")
            .password("4321")
            .email("delinesa@naver.com")
            .build();
		
		//When
		memberService.join(member1);
		IllegalStateException e = assertThrows(IllegalStateException.class, 
				() -> memberService.join(member2));
		//Then
		assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
	}
}
