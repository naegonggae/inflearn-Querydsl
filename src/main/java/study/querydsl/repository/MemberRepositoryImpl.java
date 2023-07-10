package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

// 이름을 MemberRepository + Impl 해줘야한다.
public class MemberRepositoryImpl implements MemberRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public MemberRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	@Override
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

	@Override
	public Page<MemberTeamDto> searchComplex(MemberSearchCondition condition, Pageable pageable) {
		// 데이터 조회 쿼리 (페이징 적용)
		List<MemberTeamDto> content = queryFactory
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
				.offset(pageable.getOffset()) // 몇개를 넘기고 가져올건가
				.limit(pageable.getPageSize()) // 몇개씩 가져올건가
				.fetch();

		// count 쿼리 (조건에 부합하는 로우의 총 개수를 얻는 것이기 때문에 페이징 미적용)
		Long total = queryFactory
				.select(member.count()) // SQL 상으로는 count(member.id)와 동일
				.from(member)
				.leftJoin(member.team, team) // 카운트할때는 조인이 필요없는경우도 있다. 이 방법을 사용하면 카운트쿼리를 최적화 할수있다.
				// 카운트쿼리를 하고 없으면 content 조회를 안하거나 그런 최적화도 가능 / 잘쓰면 성능을 아주 최적화할수있음
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoeEq(condition.getAgeGoe()),
						ageLoeEq(condition.getAgeLoe())
				)
				.fetchOne();

		return new PageImpl<>(content, pageable, total); // PageImpl 이 Page 의 구현체임

	}

	@Override
	public Page<MemberTeamDto> searchSimple(MemberSearchCondition condition, Pageable pageable) {
		// 심플은 이제 없어 무조건 카운트랑 콘텐트랑 나눠서 해야해
		return null;
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
