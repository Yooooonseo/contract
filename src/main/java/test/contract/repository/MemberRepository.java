package test.contract.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import test.contract.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 필요에 따라 추가적인 쿼리 메서드 정의
}

