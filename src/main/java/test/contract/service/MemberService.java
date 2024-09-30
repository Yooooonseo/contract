package test.contract.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import test.contract.domain.Member;
import test.contract.repository.MemberRepository;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    // ID로 Member 조회하는 메서드
    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElse(null); // 존재하지 않으면 null 반환
    }

    public Member saveMember(Member member) {
        return memberRepository.save(member);
    }
}
