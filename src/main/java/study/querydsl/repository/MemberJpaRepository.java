package study.querydsl.repository;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QTeam;

@Repository
public class MemberJpaRepository {

	private final EntityManager em; // 동시성 문제가 있지않냐 의문을 가질수있지만 자바에서 프록시를 생성하고 바인딩을 해주기 때문에 문제없다.
	private final JPAQueryFactory queryFactory;

	public MemberJpaRepository(EntityManager em) {
		this.em = em;
		this.queryFactory = new JPAQueryFactory(em);
	}

	// 빈등록하고 바로 주입하는 방법
//	public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//		this.em = em;
//		this.queryFactory = queryFactory;
//	}

	public void save(Member member) {
		em.persist(member);
	}

	public Optional<Member> findById(Long id) {
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
	}

	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class)
				.getResultList();
	}

	public List<Member> findAll_QueryDsl() {
		return queryFactory
				.selectFrom(member)
				.fetch();
	}

	public List<Member> findByUsername(String name) {
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
				.setParameter("username", name)
				.getResultList();
	}

	public List<Member> findByUsername_QueryDsl(String name) {
		return queryFactory
				.selectFrom(member)
				.where(member.username.eq(name))
				.fetch();
	}

	public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
		BooleanBuilder builder = new BooleanBuilder();
		if (hasText(condition.getUsername())) {
			builder.and(member.username.eq(condition.getUsername()));
		}
		if (hasText(condition.getTeamName())) {
			builder.and(team.name.eq(condition.getTeamName()));
		}
		if (condition.getAgeGoe() != null) {
			builder.and(member.age.goe(condition.getAgeGoe()));
		}
		if (condition.getAgeLoe() != null) {
			builder.and(member.age.loe(condition.getAgeLoe()));
		}

		return queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"),
						member.username,
						member.age,
						team.id.as("temaId"),
						team.name.as("teamName")
				))
				.from(member)
				.leftJoin(member.team, team)
				.where(builder)
				.fetch();
	}

}
