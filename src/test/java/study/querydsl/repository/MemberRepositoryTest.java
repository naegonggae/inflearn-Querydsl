package study.querydsl.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

	@Autowired
	EntityManager em;
	@Autowired MemberRepository memberRepository;

	@Test
	public void basicTest() throws Exception {
		Member member = new Member("member1", 10);
		memberRepository.save(member);

		Member findMember = memberRepository.findById(member.getId()).get();
		assertThat(findMember).isEqualTo(member);

		List<Member> result1 = memberRepository.findAll();
		assertThat(result1).containsExactly(member);

		List<Member> result2 = memberRepository.findByUsername("member1");
		assertThat(result2).containsExactly(member);

	}

	@Test
	void searchTest() {
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);

		MemberSearchCondition condition = new MemberSearchCondition();
		condition.setAgeGoe(35);
		condition.setAgeLoe(40);
		condition.setTeamName("teamB");
		// 조건이 하나도 없을경우 데이터를 다끌어온다...
		// 그래서 limit 를 걸거나, paging 을 하거나 기본조건을 해놔야한다.
		// 데이터가 별로 없다면 상관없지만...

		List<MemberTeamDto> result = memberRepository.search(condition); // 여기 메서드만 바꿔가면서 테스트중
		assertThat(result).extracting("username").containsExactly("member4");
	}

	@Test
	void searchComplex() {
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);

		MemberSearchCondition condition = new MemberSearchCondition();
		PageRequest pageRequest = PageRequest.of(0, 3);

		Page<MemberTeamDto> result = memberRepository.searchComplex(condition, pageRequest); // 여기 메서드만 바꿔가면서 테스트중

		assertThat(result.getSize()).isEqualTo(3);
		assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
	}

}