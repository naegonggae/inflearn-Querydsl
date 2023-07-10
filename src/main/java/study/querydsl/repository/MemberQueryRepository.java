package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

// 뭔가 특화된 기능의 메서드는 이렇게 클래스로 만들어서 인젝션 받아 사용하면 된다. 너무 custom 리포지토리로 다 하려하지 않아도 된다.
// 공통적인 조회인경우 MemberRepository 에 넣고 그게 아니고 특정 화면이나 특정 상황에 걸리는 메서드라면 이렇게 만드는게 찾기도 편하고 깔끔하다.
@Repository
public class MemberQueryRepository {

	private final JPAQueryFactory queryFactory;

	public MemberQueryRepository(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
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
