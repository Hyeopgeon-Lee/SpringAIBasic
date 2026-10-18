# SpringAIBasic — Spring Boot 4 실습

**Java 17 + Spring Boot 4.1.1 + MyBatis 4.1.0** 기반의 콘솔 실습 저장소입니다. Java와 Spring의 의존성 주입, 한국어 자연어 처리(NLP), 이미지 문자 인식(OCR), SQL 매퍼 기반 CRUD를 단계적으로 익힙니다.

> **프로젝트 이름과 실제 기능의 차이:** 현재 코드는 Spring AI(ChatClient, LLM, RAG)를 이용하지 않습니다. 이 프로젝트의 AI 예제는 **KOMORAN 형태소 분석**과 **Tesseract OCR**입니다. 생성형 AI를 실습하려면 Spring AI 관련 의존성과 예제를 별도로 추가해야 합니다.

## 1. 학습 목표

1. `@SpringBootApplication`, `@Service`, 인터페이스, 생성자 주입의 역할을 설명합니다.
2. DTO를 만들어 서비스 메서드에 데이터를 전달하고 결과를 받습니다.
3. KOMORAN으로 형태소 품사를 분석하고 명사를 추출하여 빈도를 계산합니다.
4. Tesseract로 이미지의 한글을 인식하고 NLP 입력값으로 활용합니다.
5. MyBatis Mapper 인터페이스와 XML의 `id`가 어떻게 연결되는지 설명합니다.
6. `INSERT`, `SELECT`, `UPDATE`, `DELETE`를 실행하고 결과를 확인합니다.

## 2. 개발 환경 및 기술 스택

| 항목 | 버전·설명 |
| --- | --- |
| JDK | Java 17 이상 |
| Spring Boot | 4.1.1 |
| Maven | 3.9.6 (저장소 Maven Wrapper 제공) |
| MyBatis Spring Boot Starter | 4.1.0 |
| Oracle JDBC | `ojdbc11` (Spring Boot BOM에서 버전 관리) |
| KOMORAN | 3.3.4 (JitPack) |
| Tess4J | 5.2.1 |
| Lombok | DTO getter/setter 및 생성자·로깅 |
| 테스트 DB | H2 (테스트에서만 사용, Oracle 모드) |

**Spring Boot 4 호환성:** 기존 프로젝트의 Spring Boot 3.2.5 / MyBatis 3.0.3을 Spring Boot 4.1.1 / MyBatis 4.1.0으로 변경했습니다. MyBatis 3.x Starter를 Boot 4와 혼용하지 마세요. 기존 Oracle DB와 SQL Mapper는 유지됩니다.

## 3. 디렉터리 구조

```text
src/
├─ main/
│  ├─ java/kopo/poly/
│  │  ├─ SpringAiBasicApplication.java    # 콘솔 실습 선택 및 실행
│  │  ├─ dto/                             # StudentDTO, NlpDTO, OcrDTO
│  │  ├─ mapper/IStudentMapper.java       # Mapper 메서드 선언
│  │  ├─ service/                         # Service 인터페이스
│  │  └─ service/impl/                    # 실제 처리 로직
│  └─ resources/
│     ├─ application.properties           # 환경변수·MyBatis 설정
│     ├─ image/sample01.jpg               # OCR 실습 이미지
│     └─ mapper/StudentMapper.xml         # SQL 매핑
└─ test/
   ├─ java/kopo/poly/SpringAiBasicApplicationTests.java
   └─ resources/schema.sql                # H2 테스트 테이블
```

## 4. 기본 실행 — 데이터베이스 수정 없음

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

기본 `demo.mode=none` 이므로 학생 정보 추가·삭제나 OCR 작업을 자동으로 실행하지 않습니다. **모드는 한 번에 하나씩 지정합니다.**

```bash
# Windows — 형태소 분석
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=nlp"

# macOS / Linux — 형태소 분석
./mvnw spring-boot:run -Dspring-boot.run.arguments=--demo.mode=nlp
```

실행 가능 모드:

| 모드 | 기능 | 준비사항 |
| --- | --- | --- |
| `none` | Spring 애플리케이션 기동만 확인 | JDK |
| `nlp` | 예제 문장의 형태소 분석 및 명사 빈도 계산 | JDK |
| `ocr` | `sample01.jpg`에서 한글 텍스트 추출 | 한글 Tesseract 학습 데이터 |
| `ocr-nlp` | OCR 인식 텍스트를 KOMORAN으로 분석 | Tesseract 학습 데이터 |
| `student` | Oracle 학생 데이터 등록 → 수정 → 삭제 | Oracle DB와 STUDENT 테이블 |

모드는 환경변수 `DEMO_MODE`로 설정하거나 명령 인자 `--demo.mode=...`로 덮어쓸 수 있습니다.

### NLP 실습 흐름

```java
NlpDTO tagged = nlpService.getPlainText("빅데이터를 공부합니다.");
NlpDTO result = nlpService.getNouns("빅데이터를 공부합니다.");
List<String> nouns = result.getNouns();
```

- `getPlainText()`: 형태소 분리 후 품사 태깅 결과를 문자열로 반환합니다.
- `getNouns()`: 일반 명사(NNG), 고유 명사(NNP) 등을 추출하여 `List<String>`으로 반환합니다.
- `Map.merge(noun, 1, Integer::sum)`: 같은 명사가 다시 나올 때 빈도를 1 증가시킵니다.
- `comparingByValue(reverseOrder())`: 추출한 명사를 사용 빈도 내림차순으로 출력합니다.

**확인 질문:** `List`가 중복 단어를 보관하지만 `Map`의 키는 중복되지 않는 이유는 무엇일까요?

### OCR 실습 흐름

```java
OcrDTO dto = new OcrDTO();
dto.setFilePath("image");
dto.setFileName("sample01.jpg");
String text = ocrService.getReadforImageText(dto).getResult();
```

1. `ClassPathResource`로 `resources/image/sample01.jpg`를 읽습니다.
2. `ImageIO.read(InputStream)`으로 `BufferedImage`를 만듭니다. `getFile()`과 달리 실행 JAR 내부의 리소스에서도 동작합니다.
3. Tesseract에서 `kor` 언어를 지정하여 문자 인식을 수행합니다.
4. 반환한 문자열을 KOMORAN에 전달하면 OCR → NLP 실습으로 이어집니다.

**OCR 준비:** Tesseract의 `kor.traineddata`를 모델 경로에 내려받으세요. 기본 경로는 Windows의 `C:/model/tessdata/kor.traineddata`입니다. 다른 OS에서는 `OCR_TESSDATA_PATH` 환경변수를 `kor.traineddata`가 실제로 들어 있는 디렉터리로 설정하세요. 예를 들어 Linux의 경우 `/usr/share/tesseract-ocr/5/tessdata`일 수 있습니다. 정확한 위치는 설치 환경에 따라 다릅니다. **OCR/NLP 라이브러리를 구동하는 데 OpenAI API 키는 필요하지 않습니다.**

## 5. Oracle DB와 MyBatis CRUD

학생 CRUD 모드는 Oracle 접속 및 아래 테이블 생성이 필요합니다.

```sql
CREATE TABLE STUDENT (
    USER_ID   VARCHAR2(100) PRIMARY KEY,
    USER_NAME VARCHAR2(100),
    EMAIL     VARCHAR2(200),
    ADDR      VARCHAR2(200)
);
```

애플리케이션은 다음 환경변수로 DB 정보를 읽습니다. **비밀번호를 GitHub에 커밋하지 마세요.**

| 환경변수 | 기본값 | 의미 |
| --- | --- | --- |
| `DB_URL` | `jdbc:oracle:thin:@127.0.0.1:1521/xe` | Oracle JDBC URL |
| `DB_USERNAME` | `poly` | 계정 이름 |
| `DB_PASSWORD` | 비어 있음 | 계정 비밀번호 |
| `DEMO_MODE` | `none` | 실행 예제 선택 |
| `OCR_TESSDATA_PATH` | `C:/model/tessdata` | Tesseract 언어 모델 폴더 |

Windows PowerShell 예시:

```powershell
$env:DB_URL = "jdbc:oracle:thin:@127.0.0.1:1521/xe"
$env:DB_USERNAME = "poly"
$env:DB_PASSWORD = "본인의_DB_비밀번호"
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=student"
```

매번 실행할 때 `demo_<랜덤문자>`라는 고유 학생 ID를 만들어 실습 데이터를 추가·수정·삭제합니다. **다른 학생의 기존 레코드를 대상으로 수정하거나 삭제하지 않습니다.** 다만 DB 전체 조회 로그에는 실제 데이터가 포함될 수 있으므로 운영 DB 대신 실습 DB에서 실행하세요.

### Java 메서드와 MyBatis XML의 연결

```java
// IStudentMapper.java
void insertStudent(StudentDTO pDTO) throws Exception;
```

```xml
<!-- StudentMapper.xml -->
<insert id="insertStudent" parameterType="StudentDTO">
    INSERT INTO STUDENT (USER_ID, USER_NAME, EMAIL, ADDR)
    VALUES (#{userId}, #{userName}, #{email}, #{addr})
</insert>
```

- Java 인터페이스의 `insertStudent`와 XML의 `id="insertStudent"`가 일치해야 합니다.
- `#{userId}`는 `StudentDTO.getUserId()`의 값으로 바인딩합니다. 문자열을 SQL에 그대로 붙이는 방식보다 안전합니다.
- `namespace="kopo.poly.mapper.IStudentMapper"`는 Mapper 인터페이스의 전체 이름입니다.
- `map-underscore-to-camel-case=true`를 통해 DB의 `USER_NAME`을 Java의 `userName`에 매핑합니다.

**확인 질문:** SQL의 `#{userId}`와 `${userId}`는 어떻게 다르며, 사용자 입력을 SQL 문자열에 직접 결합하면 어떤 문제가 생길까요?

## 6. 테스트 및 오류 확인

```bash
# Windows
mvnw.cmd clean verify

# macOS / Linux
./mvnw clean verify
```

`SpringAiBasicApplicationTests`는 H2 메모리 DB에 `STUDENT` 테이블을 만든 후, **실제 MyBatis XML 매퍼의 INSERT → SELECT → UPDATE → DELETE 동작**을 확인합니다. 별도의 Oracle 설치 없이 실행할 수 있습니다. GitHub Actions에서도 JDK 17로 `verify`를 실행하도록 설정했습니다.

**참고:** H2 테스트가 성공하더라도 실습 PC의 Oracle 접속 정보, 서버 권한, Tesseract 네이티브 라이브러리·학습 데이터까지 자동 검증되는 것은 아닙니다. 해당 기능은 각 실습 환경에서 별도 확인해야 합니다.

## 7. 학생 실습 과제

1. `nlp` 모드의 문장을 자유롭게 바꾸고 형태소와 명사 빈도 변화 비교하기.
2. OCR 이미지에서 한글 문장을 바꿔 `ocr-nlp`를 실행하고 인식 결과 분석하기.
3. `StudentDTO`에 `phone` 속성을 추가하고, DB 테이블·Mapper XML·서비스를 함께 변경하기.
4. Mapper XML의 `ORDER BY USER_NAME DESC`를 변경하여 정렬 순서가 바뀌는지 확인하기.
5. 예외가 발생했을 때 DB 실습 데이터가 남지 않도록 트랜잭션을 개선해 보기.

## 8. 참고 자료

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/reference/)
- [Spring Boot 4 마이그레이션 가이드](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [MyBatis Spring Boot Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [KOMORAN](https://github.com/shineware/KOMORAN)
- [Tess4J](https://github.com/nguyenq/tess4j)

## 문의

한국폴리텍대학 서울강서캠퍼스 **빅데이터소프트웨어공학과**  
이협건 교수 · hglee67@kopo.ac.kr
