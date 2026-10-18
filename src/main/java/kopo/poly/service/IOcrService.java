package kopo.poly.service;

import kopo.poly.dto.OcrDTO;

public interface IOcrService {

    /**
     * Read text from an image in src/main/resources.
     * Tesseract language data location is configured via ocr.tessdata.path.
     *
     * @param pDTO classpath folder and image file name
     * @return DTO containing OCR result text
     */
    OcrDTO getReadforImageText(OcrDTO pDTO) throws Exception;
}
