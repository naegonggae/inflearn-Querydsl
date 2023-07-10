package study.querydsl.controller;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

	private final InitMemberService initMemberService;

	@PostConstruct // transactional 과 같이 라이프사이클이 안돌아간다. 그래서 분리해서 먹여야함
	public void init() {
		initMemberService.init();
	}

	@Component
	static class InitMemberService {

		@PersistenceContext
		EntityManager em;

		@Transactional
		public void init() {
			Team teamA = new Team("teamA");
			Team teamB = new Team("teamB");
			em.persist(teamA);
			em.persist(teamB);

			for (int i = 0; i<100; i++) {
				Team selectTeam = i % 2 == 0 ? teamA : teamB;
				em.persist(new Member("member"+i, i, selectTeam));
			}
		}

	}

}
