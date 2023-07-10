package study.querydsl.repository;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
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
						team.id.as("teamId"),
						team.name.as("teamName")
				))
				.from(member)
				.leftJoin(member.team, team)
				.where(builder)
				.fetch();
	}

	public List<MemberTeamDto> search(MemberSearchCondition condition) {
		return queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"),
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")
				))
				.from(member)
				.leftJoin(member.team, team)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoeEq(condition.getAgeGoe()),
						ageLoeEq(condition.getAgeLoe())
				)
				.fetch();
	}

	// Member 로 뽑아내는 메서드인데 위에서 사용한 메서드를 동일하게 재사용가능하다.
	// 사용전략 : 이 방법을 기본으로 사용하고 상황에 따라서 빌더 방식사용하자
	// 조립도 가능하다 4개를 다 묶어서 파라미터로 condition 만 넘기게 할 수도 있음
	// 실무에서는 isValid() 해서 같은 조건을 많이 사용하는 경우가 많음 그래서 파라미터로 condition 하나만 넘기고 깔끔하게 사용하는게 김영한이 자주쓴다함
	public List<Member> searchMember(MemberSearchCondition condition) {
		return queryFactory
				.selectFrom(member)
				.leftJoin(member.team, team)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						betweenAge(condition.getAgeLoe(), condition.getAgeGoe()) // 이렇게 조립가능. 근데 이거는 null 에 대한 대비를 더 해야 쓸수있음
				)
				.fetch();
	}

	// Predicate 보다 BooleanExpression 으로 선언해야 재활용이 가능하다.
//	private Predicate usernameEq(String username) {
//		return hasText(username) ? member.username.eq(username) : null;
//	}
	private BooleanExpression betweenAge(int ageLoe, int ageGoe) {
		return ageLoeEq(ageLoe).and(ageGoeEq(ageGoe));
	}

	private BooleanExpression usernameEq(String username) {
		return hasText(username) ? member.username.eq(username) : null;
	}

	private BooleanExpression teamNameEq(String teamName) {
		return hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private BooleanExpression ageGoeEq(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	private BooleanExpression ageLoeEq(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}

}
