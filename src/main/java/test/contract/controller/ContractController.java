package test.contract.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import test.contract.domain.Member;
import test.contract.domain.MemberRole;
import test.contract.domain.VentureListInfo;
import test.contract.domain.VentureListInfoForm;
import test.contract.repository.VentureListInfoRepository;
import test.contract.service.ContractService;
import test.contract.service.MemberService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    private final MemberService memberService; // MemberService 주입

    private final VentureListInfoRepository ventureListInfoRepository;

    /*@PostMapping("/members")
    public ResponseEntity<Member> registerMember(@RequestBody Member member) {
        Member savedMember = memberService.saveMember(member);
        return ResponseEntity.ok(savedMember);
    }*/

   /* @PostMapping("/register-member") //멤버 객체 등록하기
    public ResponseEntity<String> registerMember(@RequestBody Member member) {
        memberService.saveMember(member);
        return new ResponseEntity<>("Member registered successfully", HttpStatus.CREATED);
    }*/

    /**
     * 회원 등록 및 venture 정보 저장
     * 회원가입 시 memberRole이 VENTURE이면 VentureListInfo도 함께 저장
     */
    @PostMapping("/register-member")
    public ResponseEntity<String> registerMember(@RequestBody Member member) {
        try {
            // 1. Member 저장 및 역할에 따른 추가 처리
            memberService.registerMemberWithVenture(member);

            return new ResponseEntity<>("Member and Venture info registered successfully", HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error registering member: ", e);
            return new ResponseEntity<>("Error registering member", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/register-venture")
    public ResponseEntity<String> registerVenture(@RequestBody VentureListInfoForm request) {
        Long memberId = request.getMemberId(); // 클라이언트에서 전달된 memberId

        // ventureListInfo에서 memberId 조회
        Optional<VentureListInfo> ventureInfoOptional = ventureListInfoRepository.findById(memberId);

        if (ventureInfoOptional.isPresent()) {
            VentureListInfo ventureListInfo = ventureInfoOptional.get();

            // 이미 존재하는 경우, 해당 정보 업데이트
            ventureListInfo.setName(request.getCompanyName());
            ventureListInfo.setVentureNumber(request.getBusinessLicense());
            ventureListInfo.setAddress(request.getAddress());

            ventureListInfoRepository.save(ventureListInfo); // 업데이트된 정보 저장

            return ResponseEntity.ok("기업 정보가 업데이트되었습니다.");
        } else {
            // memberId에 해당하는 Venture 정보가 없는 경우, 새로운 정보 추가
            VentureListInfo newVentureInfo = new VentureListInfo();
            newVentureInfo.setId(memberId); // memberId 설정

            newVentureInfo.setName(request.getCompanyName());
            newVentureInfo.setVentureNumber(request.getBusinessLicense());
            newVentureInfo.setAddress(request.getAddress());


            ventureListInfoRepository.save(newVentureInfo); // 새로운 정보 저장

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("기업 정보가 등록되었습니다.");
        }
    }


    @PostMapping("/save-investor-part")  //투자자가 먼저 정보 입력하고, 이를 기반으로 초안 생성하여 저장
    public ResponseEntity<String> saveInvestorPart(@RequestBody Map<String, String> investorData) {
        try {
            // 초안 생성 로직 (투자자 정보 저장)
            contractService.saveDraft(investorData);
            return new ResponseEntity<>("투자자 정보 일부 저장됨. 초안 생성됨.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error saving investor part", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/complete-contract") // 기업이 나머지 정보 입력하여 최종 계약서 완성
    public ResponseEntity<String> completeContract(@RequestBody Map<String, String> companyData) {
        try {
            // companyData에서 memberId 추출
            String memberIdStr = companyData.get("memberId");
            if (memberIdStr == null) {
                log.error("Missing memberId in request data");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            Long memberId;
            try {
                memberId = Long.parseLong(memberIdStr); // String을 Long으로 변환
            } catch (NumberFormatException e) {
                log.error("Invalid memberId format: {}", memberIdStr);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // MemberService를 통해 memberId로 Member 객체 조회
            Member member = memberService.getMemberById(memberId);
            if (member == null) {
                log.error("Member not found for ID: {}", memberId);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // 계약서 암호 설정: memberRole에 따라 다른 암호 설정
            String userPassword;
            if (member.getMemberRole() == MemberRole.MEMBER) {
                // 투자자일 경우 생년월일로 암호 설정
                log.info("MemberRole : MEMBER");
                userPassword = member.getBirthdate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else if (member.getMemberRole() == MemberRole.VENTURE) {
                // 기업일 경우 사업자 등록번호로 암호 설정
                log.info("MemberRole : MEMBER");
                userPassword = companyData.get("ventureNumber");
                if (userPassword == null) {
                    log.error("Missing ventureNumber in request data for VENTURE role");
                    return new ResponseEntity<>("기업 사용자는 ventureNumber가 필요합니다.", HttpStatus.BAD_REQUEST);
                }
            } else {
                log.error("Invalid role for Member ID: {}", memberId);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // ownerPassword는 companyData에서 추출
            String ownerPassword = companyData.get("ownerPassword");
            if (ownerPassword == null) {
                log.error("Missing ownerPassword in request data");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // 계약서 완성 로직 (기업 정보 입력 후 최종본 생성 및 암호화)
            contractService.completeContract(companyData, userPassword, ownerPassword);
            return new ResponseEntity<>("암호화된 계약서 완성본 생성됨.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error completing contract", e);
            return new ResponseEntity<>("Error completing contract", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




   /* @PostMapping("/generate-contract")
    public ResponseEntity<byte[]> generateContract(@RequestBody Map<String, String> fieldData) {
        log.info("Received request to generate and encrypt contract with data: {}", fieldData);

        try {
            // fieldData에서 memberId 추출
            //String memberId = fieldData.get("memberId");
            Long memberId = Long.parseLong(fieldData.get("memberId")); // String을 Long으로 변환
            if (memberId == null) {
                log.error("Missing memberId in request data");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // MemberService를 통해 memberId로 Member 객체 조회
            Member member = memberService.getMemberById(memberId);
            if (member == null) {
                log.error("Member not found for ID: {}", memberId);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // 생년월일을 userPassword로 사용 (yyyyMMdd 형식으로 변환)
            String userPassword = member.getBirthdate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // ownerPassword는 fieldData에서 추출
            String ownerPassword = fieldData.get("ownerPassword");

            if (ownerPassword == null || userPassword == null) {
                log.error("Missing ownerPassword or userPassword");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // 계약서 생성 및 암호화된 파일 경로 가져오기
            String[] filePaths = contractService.generateAndProtectContract(fieldData, ownerPassword, userPassword);
            String protectedFilePath = filePaths[1]; // 암호화된 파일 경로
            log.info("Encrypted contract generated and saved to: {}", protectedFilePath);

            // 생성된 암호화된 파일을 읽기
            File file = new File(protectedFilePath);
            if (!file.exists()) {
                log.error("File does not exist: " + protectedFilePath);
                throw new IOException("File not found at path: " + protectedFilePath);
            }

            byte[] pdfContent = Files.readAllBytes(file.toPath());

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "encrypted_contract.pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error occurred while generating and encrypting contract with data: {}", fieldData, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/
}
