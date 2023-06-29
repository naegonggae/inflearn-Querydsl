package study.querydsl;

import static org.assertj.core.api.Assertions.*;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslApplicationTest {

	@Autowired
	EntityManager em;
	JPAQueryFactory jpaQueryFactory; // 이렇게 하면 동시성문제가 걸리지 않나? 괜찮다고함 멀티쓰레드 환경에서 사용가능하도록 설계되었다고 함

	@BeforeEach
	public void before() {
		jpaQueryFactory = new JPAQueryFactory(em); // 여기만 실행하면 다 쓰이도록

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
	}

	@Test
	void startJpql() {
		// member1 를 찾아라
		Member findMember = em.createQuery("select m from Member m where m.username = :username",
						Member.class)
				.setParameter("username", "member1")
				.getSingleResult();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void startQueryDsl() {
		// 동시에 쓰도록 설정가능
//		JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em); // 엔티티 매니저로 데이터를 찾는거 설정한거임

		QMember m = new QMember("m"); //"m" 변수명 즉, 별칭 설정 근데 중요하지는 않다고함

		// 강점 : Jpql 에 비해 컴파일시 오류를 잡아준다.
		Member findMember = jpaQueryFactory
				.select(m)
				.from(m)
				.where(m.username.eq("member1")) // 파라미터 바인딩을 자동으로 해줌 eq= 같다는의미
				.fetchOne(); // 아마 단건조회
		assertThat(findMember.getUsername()).isEqualTo("member1");

	}
}
