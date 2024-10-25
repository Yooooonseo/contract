package test.contract.domain;

import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

import lombok.Data;

@Data
public class VentureListInfoForm {  //VentureRegisterationRequest
    private Long memberId;         // 회원 ID
    private String companyName;    // 회사명
    private String businessLicense; // 사업자 등록번호
    private String address;        // 주소
    //private String phoneNumber;    // 전화번호
    //private String description;     // 설명
}
