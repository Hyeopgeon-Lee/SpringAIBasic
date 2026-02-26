# SpringAIBasic

Spring Boot 기반 AI/DB 기초 실습 프로젝트입니다.

- OCR(Tesseract + Tess4J)
- 형태소 분석(Komoran)
- MyBatis + Oracle DB CRUD

현재 메인 실행 흐름은 `CommandLineRunner` 기반 콘솔 프로그램이며, 기본 상태에서는 학생 정보 CRUD(MyBatis/Oracle) 예제가 실행됩니다. OCR/NLP 예제 코드는 포함되어 있지만 `SpringAiBasicApplication`에서 주석 처리되어 있습니다.

## 기술 스택

- Java 17
- Spring Boot 3.2.5
- MyBatis Spring Boot Starter 3.0.3
- Oracle JDBC (`ojdbc11`)
- KOMORAN 3.3.4
- Tess4J 5.2.1
- Lombok

## 프로젝트 구조

```text
src/main/java/kopo/poly
├─ SpringAiBasicApplication.java      # CommandLineRunner 실행 진입점
├─ dto/                               # OcrDTO, NlpDTO, StudentDTO
├─ mapper/                            # MyBatis Mapper 인터페이스
└─ service/
   ├─ INlpService, IOcrService, IStudentService
   └─ impl/                           # OcrService, NlpService, StudentService

src/main/resources
├─ application.properties             # DB/MyBatis 설정
├─ image/sample01.jpg                 # OCR 예제 이미지
└─ mapper/StudentMapper.xml           # MyBatis SQL 매핑
```

## 현재 실행 동작(기본)

`src/main/java/kopo/poly/SpringAiBasicApplication.java`의 `run()`에서 아래 순서가 실행됩니다.

1. 학생 1건 등록
2. 동일 학생 정보 수정
3. 동일 학생 정보 삭제
4. 각 단계 후 `STUDENT` 테이블 전체 조회 결과를 로그로 출력

## 사전 준비

### 1. JDK / Maven

- JDK 17 설치
- Maven Wrapper 사용 가능 (`mvnw`, `mvnw.cmd`)

### 2. Oracle DB 준비

기본 설정값(`src/main/resources/application.properties`)

```properties
spring.datasource.url=jdbc:oracle:thin:@127.0.0.1:1521/xe
spring.datasource.username=poly
spring.datasource.password=1234
```

환경에 맞게 값 수정 후 아래 테이블을 생성하세요.

```sql
CREATE TABLE STUDENT (
    USER_ID   VARCHAR2(100) PRIMARY KEY,
    USER_NAME VARCHAR2(100),
    EMAIL     VARCHAR2(200),
    ADDR      VARCHAR2(200)
);
```

### 3. (선택) OCR 실행 준비

OCR 예제를 사용하려면 Tesseract 학습 데이터가 아래 경로에 있어야 합니다.

- `C:/model/tessdata`

이 경로는 `src/main/java/kopo/poly/service/IOcrService.java`의 상수 `modelFile`로 고정되어 있습니다.

또한 `kor.traineddata`(한국어) 파일이 필요합니다. (`OcrService`에서 `instance.setLanguage("kor")` 사용)

## 실행 방법

Windows:

```bash
mvnw.cmd spring-boot:run
```

공통(Maven 설치 시):

```bash
mvn spring-boot:run
```

## 테스트 실행

```bash
mvnw.cmd test
```

기본 테스트는 Spring 컨텍스트 로드 테스트(`contextLoads`) 1건입니다.

## OCR / NLP 예제 실행 방법(선택)

현재 `SpringAiBasicApplication`의 OCR/NLP 관련 코드는 주석 처리되어 있습니다.

- `OcrDTO` 생성
- `ocrService.getReadforImageText()` 호출
- `nlpService.getPlainText()` / `nlpService.getNouns()` 호출
- 명사 빈도 계산 및 정렬 로그 출력

해당 블록의 주석을 해제하면 `src/main/resources/image/sample01.jpg`를 대상으로 OCR + 형태소 분석 예제를 실행할 수 있습니다.

## 참고 사항

- 웹 API 프로젝트가 아니라 콘솔 로그 중심 실습 코드입니다.
- 학생 CRUD SQL은 `src/main/resources/mapper/StudentMapper.xml`에 정의되어 있습니다.
- MyBatis camelCase 매핑이 활성화되어 있어 `USER_ID -> userId` 형태로 자동 매핑됩니다.

## 🙋‍♀️ 문의
- **한국폴리텍대학 서울강서캠퍼스 빅데이터소프트웨어과**
- **이협건 교수** · hglee67@kopo.ac.kr
- 입학 상담 오픈채팅방: <https://open.kakao.com/o/gEd0JIad>