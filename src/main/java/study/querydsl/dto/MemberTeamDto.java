package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {

	private Long MemberId;
	private String username;
	private int age;
	private Long teamId;
	private String teamName;

	@QueryProjection // dto 가 queryDsl 에 의존한다 순수하지 못하다
	public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
		MemberId = memberId;
		this.username = username;
		this.age = age;
		this.teamId = teamId;
		this.teamName = teamName;
	}
}
