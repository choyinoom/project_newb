package com.project.myApplication.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import com.project.myApplication.domain.member.Member;

public class JpaMemberRepository implements MemberRepository {

	private final EntityManager em;
	
	public JpaMemberRepository(EntityManager em) {
		this.em = em;
	}
	
	public Member save(Member member) {
		em.persist(member);
		return member;
	}
	
	public Optional<Member> findById(Long id) {
		Member member = em.find(Member.class, id);
		return Optional.ofNullable(member);
	}
	
	public List<Member> findAll() {
		return em.createQuery("select m from MEMBERS m", Member.class)
				.getResultList();
	}
	
	public Optional<Member> findByUsername(String username) {
		List<Member> result = em.createQuery("select m from MEMBERS m where m.username = :username", Member.class)
				.setParameter("username", username)
				.getResultList();
		return result.stream().findAny();
	}
	
}
