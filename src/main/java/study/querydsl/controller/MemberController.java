package study.querydsl.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
public class MemberController {

	private final MemberJpaRepository memberJpaRepository;
	private final MemberRepository memberRepository;

	@GetMapping("/api/v1/members")
	public List<MemberTeamDto> selectMemberV1(MemberSearchCondition condition) {
		return memberJpaRepository.search(condition);
	}
	// localhost:8080/api/v1/members?teamName=teamB&ageGoe=35&ageLoe=40&username=member37
	// where
	//            m1_0.username=?
	//            and t1_0.name=?
	//            and m1_0.age>=?
	//            and m1_0.age<=?

	@GetMapping("/api/v2/members")
	public Page<MemberTeamDto> selectMemberV2(MemberSearchCondition condition, Pageable pageable) {
		return memberRepository.searchComplex(condition, pageable);
	}
	// localhost:8080/api/v2/members?page=1&size=5
}
