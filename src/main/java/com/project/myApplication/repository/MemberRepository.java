package com.project.myApplication.repository;

import java.util.List;
import java.util.Optional;

import com.project.myApplication.domain.Member;

public interface MemberRepository {

	Member save(Member member);
	Optional<Member> findById(Long id);
	Optional<Member> findByUsername(String name);
	List<Member> findAll();
}
