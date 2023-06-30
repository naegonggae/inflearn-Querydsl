package study.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
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
				.where(member.username.eq("member1")
						.and(member.age.eq(10))) // .and .or 로 계속 이어나갈 수 있음
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
		List<Member> fetch = jpaQueryFactory
				.selectFrom(member)
				.fetch(); // 리스트 조회

		Member fetchOne = jpaQueryFactory
				.selectFrom(member)
				.fetchOne();// 단건조회, 없으면 null 두개면 예외발생

		Member fetchFirst = jpaQueryFactory
				.selectFrom(member)
//				.limit(1).fetchOne() / == fetchFirst
				.fetchFirst();

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

	@Test
	void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = jpaQueryFactory
				.selectFrom(member)
				.where(member.age.eq(100))
				.orderBy(member.age.desc(), member.username.asc().nullsLast())
				// 나이는 내림차순 어차피 100이라 상관없고, 이름은 오름차순, 이름없으면 마지막에 null 출력
				.fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);
		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull(); // 마지막에 이름없는거 저장했으니 null 뜰거다
	}

	@Test
	void paging1() {
		List<Member> result = jpaQueryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1) // 앞에 몇개 짤라 먹을거야?
				.limit(2) // 몇개씩 가져올거야?
				.fetch();
		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	void paging2() {
		// 실무에서는 이렇게하면 쿼리두개 나가니까 상황보고 count 쿼리를 따로 해주라한다.
		// 근데 fetchResults 이거 막힌거 보니까 무조건 따로 날리는 쪽으로 가야하나보다
		QueryResults<Member> queryResults = jpaQueryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1) // 앞에 몇개 짤라 먹을거야?
				.limit(2) // 몇개씩 가져올거야?
				.fetchResults();
		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);
	}

	@Test
	void aggregation() {
		List<Tuple> result = jpaQueryFactory // queryDsl 에서 지원하는 튜플이야
				.select( // 튜플로 왜 뽑냐? 형태가 다달라서 / 근데 튜플 스타일을 자주 사용하지 않고 Dto 형태로 뽑아서 쓸거야
						member.count(),
						member.age.sum(),
						member.age.max(),
						member.age.min(),
						member.age.avg()
				)
				.from(member)
				.fetch(); // 어짜피 하나임
		Tuple tuple = result.get(0); // 하나니까 꺼내와
		assertThat(tuple.get(member.count())).isEqualTo(4); // 꺼낼때 그대로 꺼내
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	@Test
	public void group() throws Exception {
		// 팀의 이름과 각 팀의 평균 연령을 구해라
		List<Tuple> result = jpaQueryFactory
				.select(team.name, member.age.avg())
				.from(member)
				.join(member.team, team)
				.groupBy(team.name) // having 도 가능 ex. 1000원 넘는 가격만 groupBy 해라
				.fetch();
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	@Test
	public void join() throws Exception {
		// teamA 에 소속된 member 를 모두 찾아라
		List<Member> result = jpaQueryFactory
				.selectFrom(member)
				.join(member.team, team) // left, right, inner join 다 가능
				.where(team.name.eq("teamA"))
				.fetch();

		assertThat(result)
				.extracting("username")
				.containsExactly("member1", "member2");
	}

	@Test
	public void thetaJoin() throws Exception {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));

		//세타 조인 사람 이름과 팀이름이 같은 사람을 조회
		List<Member> result = jpaQueryFactory
				.select(member)
				.from(member,
						team) // member 와 team 을 다가져와서 막 조인해서 찾는것임 / 보통 DB 가 성능최적화를 하긴할거임 / 막조인
				// 전혀 연관관계 없는 테이블들도 외부 조인 할 수 있는 방법이 생겼다함
				// 실제 쿼리는 크로스 조인함
				.where(member.username.eq(team.name))
				.fetch();
		assertThat(result)
				.extracting("username")
				.containsExactly("teamA", "teamB");
	}

	@Test
	public void join_on_filtering() throws Exception {
		// 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
		// JPQL : select m, t from Member m left join m.team t on t.name = "teamA";
		List<Tuple> result = jpaQueryFactory
				.select(member, team)
				.from(member)
//				.leftJoin(member.team, team).on(team.name.eq("teamA"))
				// left join 하고 on 절 사용하는게 inner join 하고 where 조건 하는거랑 결과가 같음
				// 이럴땐 inner join, where 사용한다고 함 익숙하니까
				.join(member.team, team)
				.where(team.name.eq("teamA"))
				.fetch();
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	@Test
	public void join_on_no_relation() throws Exception {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));

		//연관관계가 없는 엔티티를 외부조인 on 을 사용해서 가능하게 됨 -> 하이버네이트 5.1 부터
		//회원의 이름과 팀이름이 같은 대상을 외부조인해라
		List<Tuple> result = jpaQueryFactory
				.select(member, team)
				.from(member)
				// 전혀 연관관계 없는 테이블들도 외부 조인 할 수 있는 방법이 생겼다함
				.leftJoin(team).on(member.username.eq(
						team.name)) // 그냥 쌩팀으로 left join 해버림 / id 매칭이 안되니까 on 절의 이름으로만 매칭이됨
				// .leftJoin(member.team, team) 원래는 이런식으로 들어가서 on 절에서 id 값이 들어가서 join 하는 대상이 id 로 매칭이 됨 team.id == member.team.id
				.fetch();
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Test
	void fetchJoinNo() {
		em.flush();
		em.clear();

		Member findMember = jpaQueryFactory
				.select(member)
				.from(member)
				.where(member.username.eq("member1"))
				.fetchOne();
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치조인 미적용").isFalse();
	}

	@Test
	void fetchJoinUse() {
		em.flush();
		em.clear();

		Member findMember = jpaQueryFactory
				.select(member)
				.from(member)
				.join(member.team, team).fetchJoin()
				.where(member.username.eq("member1"))
				.fetchOne();
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치조인 적용").isTrue();
	}

	@Test
	void subQuery() {
		// 나이가 가장 많은 회원 조회
		QMember memberSub = new QMember("memberSub");

		List<Member> result = jpaQueryFactory
				.select(member)
				.from(member)
				.where(member.age.eq(
						JPAExpressions // 내부에 쿼리 작성
								.select(memberSub.age.max()) // member 와 다르게 지정해줘야함
								.from(memberSub)
				))
				.fetch();
		assertThat(result).extracting("age")
				.containsExactly(40);
	}

	@Test
	void subQueryGoe() {
		// 나이가 평균이상인 회원 조회
		QMember memberSub = new QMember("memberSub");

		List<Member> result = jpaQueryFactory
				.select(member)
				.from(member)
				.where(member.age.goe( // 크거나 같다
						JPAExpressions // 내부에 쿼리 작성
								.select(memberSub.age.avg()) // member 와 다르게 지정해줘야함
								.from(memberSub)
				))
				.fetch();
		assertThat(result).extracting("age")
				.containsExactly(30, 40);
	}

	@Test
	void subQueryIn() {
		// 나이가 평균이상인 회원 조회
		QMember memberSub = new QMember("memberSub");

		List<Member> result = jpaQueryFactory
				.select(member)
				.from(member)
				.where(member.age.in( // 크거나 같다
						JPAExpressions // 내부에 쿼리 작성
								.select(memberSub.age) // member 와 다르게 지정해줘야함
								.from(memberSub)
								.where(memberSub.age.gt(10)) // 보다 크다
				))
				.fetch();
		assertThat(result).extracting("age")
				.containsExactly(20, 30, 40);
	}

	@Test
	void selectSubQuery() {
		QMember memberSub = new QMember("memberSub");

		List<Tuple> result = jpaQueryFactory
				.select(member.username, // select 내부 쿼리
						JPAExpressions
								.select(memberSub.age.avg())
								.from(memberSub))
				.from(member)
				.fetch();
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
		// from 절은 서브쿼리 지원안함
		//1. 서브쿼리를 join 으로 변경
		//2. 쿼리 두번으로 나눠서 실행
		//3. 네이티브 SQL 사용

		//너무 뷰에 의존해서 SQL 문을 짜려하다보니 쿼리가 복잡해진다.
		//데이터 가져오는데 집중하고 뷰 형식 맞추는건 프론트에서 하는걸로 / 쿼리로 어떻게든 다 풀려고 하지마
		//한방 쿼리가 무조건 좋나? 트래픽을 신경쓰는 곳이라면 이미 캐시등의 대책을 마련했다...
	}

	@Test
	void basicCase() {
		List<String> result = jpaQueryFactory
				.select(member.age
						.when(10).then("열살")
						.when(20).then("스무살")
						.otherwise("기타"))
				.from(member)
				.fetch();
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	void complexCase() {
		List<String> result = jpaQueryFactory
				.select(new CaseBuilder()
						.when(member.age.between(0, 20)).then("0~20살")
						.when(member.age.between(21, 40)).then("21~40살")
						.otherwise("기타"))
				.from(member)
				.fetch();
		for (String s : result) {
			System.out.println("s = " + s);
		}
		// DB 에서는 row 데이터를 필터링하고 그룹핑하고 이런 작업(데이터를 줄이는 일)을 수행하지 이런 조건식은 권장하지 않는다
		// 그래서 실제로 예시는 나이를 다 가져오고 애플리케이션 레벨이나 프레젠테이션 레벨에서 조건에따라 표현을 달리한다.
	}
}
