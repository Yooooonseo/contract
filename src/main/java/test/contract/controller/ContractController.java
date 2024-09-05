package test.contract.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import test.contract.service.ContractService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @PostMapping("/generate-contract")
    public ResponseEntity<byte[]> generateContract(@RequestBody Map<String, String> fieldData) {
        log.info("Received request to generate contract with data: {}", fieldData);

        try {
            // 계약서 생성 및 파일 저장 경로 가져오기
            String filePath = contractService.fillContractTemplate(fieldData);
            log.info("Contract generated and saved to: {}", filePath);

            // 생성된 파일을 읽기
            File file = new File(filePath);
            byte[] pdfContent = Files.readAllBytes(file.toPath());

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "filled_contract.pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error occurred while generating contract with data: {}", fieldData, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}