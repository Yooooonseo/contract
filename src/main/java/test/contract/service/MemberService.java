package test.contract.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import test.contract.domain.Member;
import test.contract.domain.MemberRole;
import test.contract.domain.VentureListInfo;
import test.contract.repository.MemberRepository;
import test.contract.repository.VentureListInfoRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final VentureListInfoRepository ventureListInfoRepository;

    /**
     * 회원 가입 시 memberRole이 VENTURE일 경우 VentureListInfo도 함께 생성 및 저장
     */
    public void registerMemberWithVenture(Member member) {
        // 1. Member 테이블에 회원 정보 저장
        Member savedMember = memberRepository.save(member);

        // 2. 만약 회원의 역할이 VENTURE라면 VentureListInfo도 생성 및 저장
        if (member.getMemberRole() == MemberRole.VENTURE) {
            VentureListInfo ventureListInfo = new VentureListInfo();
            ventureListInfo.setMember(savedMember); // Member와 연결
            ventureListInfoRepository.save(ventureListInfo);
            log.info("Venture information saved for member ID: {}", savedMember.getId());
        }
    }

    // ID로 Member 조회하는 메서드
    public Member getMemberById(Long memberId) {

        return memberRepository.findById(memberId)
                .orElse(null); // 존재하지 않으면 null 반환
    }

    public Member saveMember(Member member) {
        //return memberRepository.save(member);
        Member savedMember = memberRepository.save(member);

        // memberRole이 VENTURE인 경우, VentureListInfo 생성 및 연결
        if (member.getMemberRole() == MemberRole.VENTURE) {
            // 새로운 VentureListInfo 객체 생성
            VentureListInfo ventureListInfo = new VentureListInfo();
            ventureListInfo.setMember(savedMember);  // member 필드에 저장된 Member 설정
            ventureListInfoRepository.save(ventureListInfo);  // VentureListInfo 저장
        }
        return savedMember;
    }
}
