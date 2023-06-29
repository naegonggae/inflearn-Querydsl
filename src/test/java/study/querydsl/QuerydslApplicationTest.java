package study.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
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

//		QMember m = new QMember("m"); //"m" 변수명 즉, 별칭 설정 근데 중요하지는 않다고함
//		QMember m = QMember.member; // 이미 생성된걸 사용해도됨 QMember.member 내부에 있음
		// 여기서 더 줄일 수 있음 -> static import 사용해서 완전히 주석처리
		// 대신 같은 테이블을 join 해야할때는 별칭을 따로 선언해줘야한다.

		// 강점 : Jpql 에 비해 컴파일시 오류를 잡아준다.
		Member findMember = jpaQueryFactory
				.select(member) // 요렇게 보라색 글씨로 나옴 이렇게 쓰는걸 권장
				.from(member)
				.where(member.username.eq("member1")) // 파라미터 바인딩을 자동으로 해줌 eq= 같다는의미
				.fetchOne(); // 아마 단건조회
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void search() {
		Member findMember = jpaQueryFactory
				.selectFrom(member) // select from 을 줄일 수 있다.
				.where(member.username.eq("member1").and(member.age.eq(10))) // .and .or 로 계속 이어나갈 수 있음
				.fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("member1");
		assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	void searchAndParam() {
		Member findMember = jpaQueryFactory
				.selectFrom(member) // select from 을 줄일 수 있다.
				.where(
						member.username.eq("member1"),
						member.age.eq(10) // 요 방법이 동적쿼리에서 빛을 발함
				) // and 는 요렇게 도 가능 / 외에 여러 연산자가 있음
				.fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("member1");
		assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	void resultFetch() {
//		List<Member> fetch = jpaQueryFactory
//				.selectFrom(member)
//				.fetch(); // 리스트 조회
//
//		Member fetchOne = jpaQueryFactory
//				.selectFrom(member)
//				.fetchOne();// 단건조회, 없으면 null 두개면 예외발생
//
//		Member fetchFirst = jpaQueryFactory
//				.selectFrom(member)
////				.limit(1).fetchOne() / == fetchFirst
//				.fetchFirst();

		QueryResults<Member> results = jpaQueryFactory // 페이징에서 사용 / 성능이 중요시 되는 곳에서는 이렇게 하지말고 쿼리 두개를 따로 날려야함
				.selectFrom(member)
				.fetchResults(); // .fetch() 대체 사용 권장 / count 쿼리 + member 찾아오는 쿼리 = 총 2개나감
		System.out.println("===");
		results.getTotal();
		List<Member> content = results.getResults();

		long count = jpaQueryFactory // 토탈카운트
				.selectFrom(member)
				.fetchCount();// member select 하는 쿼리를 카운트 쿼리로 바꾸는데 이거도 사용안하네...

	}
}
