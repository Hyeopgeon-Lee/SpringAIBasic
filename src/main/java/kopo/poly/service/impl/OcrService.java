package kopo.poly.service.impl;

import kopo.poly.dto.OcrDTO;
import kopo.poly.service.IOcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@Slf4j
@Service
public class OcrService implements IOcrService {

    @Value("${ocr.tessdata.path}")
    private String tessdataPath;

    @Override
    public OcrDTO getReadforImageText(OcrDTO pDTO) throws Exception {
        if (pDTO == null || pDTO.getFilePath() == null || pDTO.getFileName() == null) {
            throw new IllegalArgumentException("OCR 파일 경로와 파일명을 지정해야 합니다.");
        }

        // Use an input stream instead of ClassPathResource.getFile():
        // resources packaged inside a runnable JAR are not regular disk files.
        String resourcePath = pDTO.getFilePath() + "/" + pDTO.getFileName();
        ClassPathResource resource = new ClassPathResource(resourcePath);

        BufferedImage image;
        try (InputStream inputStream = resource.getInputStream()) {
            image = ImageIO.read(inputStream);
        }
        if (image == null) {
            throw new IllegalArgumentException("읽을 수 없는 이미지 형식: " + resourcePath);
        }

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("kor");

        String recognizedText = tesseract.doOCR(image);
        pDTO.setResult(recognizedText);
        log.info("OCR 분석 완료: {}", resourcePath);
        return pDTO;
    }
}
