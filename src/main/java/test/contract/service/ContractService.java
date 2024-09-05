package test.contract.service;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ContractService {

    // Logger 추가
    private static final Logger logger = Logger.getLogger(ContractService.class.getName());

    public String fillContractTemplate(Map<String, String> fieldData) throws IOException {
        String templatePath = "D:/Admin/ProtectedFileStore/test4.pdf";
        String outputFilePath = "D:/Admin/ProtectedFileStore/filled_contract.pdf";

        logger.info("Loading PDF template from: " + templatePath);

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(templatePath), new PdfWriter(new FileOutputStream(outputFilePath)))) {

            if (pdfDoc == null) {
                throw new IOException("Failed to load the PDF document from the path: " + templatePath);
            }
            logger.info("PDF document loaded successfully.");

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            if (form == null) {
                throw new IllegalArgumentException("No form found in the PDF.");
            }
            logger.info("Form fields loaded successfully.");

            Map<String, PdfFormField> fields = form.getFormFields();

            if (fields.isEmpty()) {
                throw new IllegalArgumentException("No fields found in the PDF.");
            }
            logger.info("Form fields available: " + fields.keySet());

            // 폰트 파일 경로 (나눔명조 폰트 사용)
            String fontPath = "D:/NanumMyeongjo.ttf";

            // 폰트 로드 (내장 포함)
            PdfFont font = PdfFontFactory.createFont(fontPath, "Identity-H", true);

            // 필드 이름 매핑 (JSON 키 -> PDF 필드 이름)
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("investorName1", "dhFormfield-5193825025");
            fieldMapping.put("ventureName1", "dhFormfield-5193704257");
            fieldMapping.put("price", "dhFormfield-5193831145");
            fieldMapping.put("year", "dhFormfield-5193832166");
            fieldMapping.put("month", "dhFormfield-5193832173");
            fieldMapping.put("date", "dhFormfield-5193832174");
            fieldMapping.put("investorAddress", "dhFormfield-5193832179");
            fieldMapping.put("investorBusinessName", "dhFormfield-5193832180");
            fieldMapping.put("investorName2", "dhFormfield-5193834284");
            fieldMapping.put("ventureAddress", "dhFormfield-5193834335");
            fieldMapping.put("ventureBusinessName", "dhFormfield-5193834717");
            fieldMapping.put("ventureName2", "dhFormfield-5193834787");
            //나머지 필드 더 추가

            // JSON 필드 데이터에 대해 처리
            for (Map.Entry<String, String> entry : fieldData.entrySet()) {
                String jsonKey = entry.getKey();
                String fieldValue = entry.getValue();

                // 매핑된 PDF 필드 이름 가져오기
                String pdfFieldName = fieldMapping.get(jsonKey);

                if (pdfFieldName != null) {
                    PdfFormField pdfField = fields.get(pdfFieldName);
                    if (pdfField != null) {
                        // 필드에 값 설정, 폰트 설정, 폰트 크기 설정
                        pdfField.setValue(fieldValue)
                                .setFont(font)      // 폰트 설정
                                .setFontSize(8);   // 폰트 크기 설정
                        logger.info("Field '" + pdfFieldName + "' set to: " + fieldValue);
                    } else {
                        logger.warning("Field '" + pdfFieldName + "' not found in the PDF.");
                    }
                } else {
                    logger.warning("No mapping found for JSON key '" + jsonKey + "'");
                }
            }

            // 필드를 일반 텍스트로 변환
            form.flattenFields();
            logger.info("Fields have been flattened.");

            // 결과 파일 저장
            logger.info("Contract saved to: " + outputFilePath);
            return outputFilePath;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while processing the PDF: " + e.getMessage(), e);
            throw e;
        }
    }
}