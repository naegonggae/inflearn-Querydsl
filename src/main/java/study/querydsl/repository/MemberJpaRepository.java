package study.querydsl.repository;

import static study.querydsl.entity.QMember.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

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

}
