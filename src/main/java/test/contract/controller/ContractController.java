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
import test.contract.service.ContractService;
import test.contract.service.MemberService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    private final MemberService memberService; // MemberService 주입

    /*@PostMapping("/members")
    public ResponseEntity<Member> registerMember(@RequestBody Member member) {
        Member savedMember = memberService.saveMember(member);
        return ResponseEntity.ok(savedMember);
    }*/

    @PostMapping("/register-member") //멤버 객체 등록하기
    public ResponseEntity<String> registerMember(@RequestBody Member member) {
        memberService.saveMember(member);
        return new ResponseEntity<>("Member registered successfully", HttpStatus.CREATED);
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

    @PostMapping("/complete-contract") //기업이 나머지 정보 입력하여 최종 계약서 완성
    public ResponseEntity<String> completeContract(@RequestBody Map<String, String> companyData) {
        try {
            /*// fieldData에서 memberId 추출
            //String memberId = fieldData.get("memberId");
            Long memberId = Long.parseLong(companyData.get("memberId")); // String을 Long으로 변환
            if (memberId == null) {
                log.error("Missing memberId in request data");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }*/

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

            // 생년월일을 userPassword로 사용 (yyyyMMdd 형식으로 변환)
            String userPassword = member.getBirthdate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // ownerPassword는 fieldData에서 추출
            String ownerPassword = companyData.get("ownerPassword");

            if (ownerPassword == null || userPassword == null) {
                log.error("Missing ownerPassword or userPassword");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // 계약서 완성 로직 (기업 정보 입력 후 최종본 생성 및 암호화)
            contractService.completeContract(companyData, userPassword, ownerPassword); // 패스워드를 함께 전달
            return new ResponseEntity<>("암호화된 계약서 완성본 생성됨.", HttpStatus.OK);
        } catch (Exception e) {
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
