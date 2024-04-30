package kopo.poly.service.impl;

import kopo.poly.dto.OcrDTO;
import kopo.poly.service.IOcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OcrService implements IOcrService {

    /**
     * 이미지 파일로부터 문자 읽어 오기
     *
     * @param pDTO 이미지 파일 정보
     * @return pDTO 이미지로부터 읽은 문자열
     */
    @Override
    public OcrDTO getReadforImageText(OcrDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getReadforImageText start!");

        // resources 폴더 밑에 존재하는 파일을 활용하기 위해서는 반드시 ClassPathResource 객체 사용함
        ClassPathResource resource = new ClassPathResource(pDTO.getFilePath() + "/" + pDTO.getFileName());

        // OCR 기술 사용을 위한 Tesseract 플랫폼 객체 생성
        ITesseract instance = new Tesseract();

        // OCR 분석에 필요한 기준 데이터(이미 각 나라의 언어별로 학습시킨 데이터 위치 폴더)
        // 저장 경로는 물리경로를 사용함(전체 경로)
        instance.setDatapath(IOcrService.modelFile); // IOcrService 인터페이스 파일에 정의한 상수

        // 한국어 학습 데이터 선택(기본 값은 영어)
        instance.setLanguage("kor"); //한국어 설정
//        instance.setLanguage("eng"); //영어 설정

        // 이미지 파일로부터 텍스트 읽기
        String result = instance.doOCR(resource.getFile());

        // 읽은 글자를 DTO에 저장하기
        pDTO.setResult(result);

        log.info(this.getClass().getName() + ".getReadforImageText End!");

        return pDTO;
    }
}
