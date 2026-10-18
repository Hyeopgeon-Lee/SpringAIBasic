# SpringAIBasic — Spring Boot 4 AI 모델 실습

Java 17 / Spring Boot 4.1.1에서 **학습된 AI 모델 파일을 읽어 실제 추론(inference)을 실행**하는 콘솔 실습 예제입니다.

- **AI 모델 분류 실습(신규)**: 붓꽃(Iris) 품종을 구분하는 학습된 `iris-logreg.onnx` 사용
- **OCR 실습(유지)**: Tesseract / Tess4J로 이미지의 한글 문자 인식
- **DB 실습(유지)**: MyBatis / Oracle CRUD
- **KOMORAN 형태소 분석은 제거했습니다.**
- Spring AI(ChatClient/LLM) 프레임워크를 사용하는 프로젝트가 아닙니다. 여기서 AI는 머신러닝 **ONNX 모델 추론**을 뜻합니다.

## 1. 왜 Iris 모델인가요?

학생들은 API 키, 외부 AI 서버, GPU, 대용량 모델 다운로드 없이 다음을 익힐 수 있습니다.

1. **학습(Training)**: 붓꽃 데이터를 이용해 다중 클래스 로지스틱 회귀 모델을 학습합니다.
2. **모델 파일(Artifact)**: 학습 결과(가중치와 절편, 계산 그래프)를 `.onnx`로 저장합니다.
3. **추론(Inference)**: Spring Boot 서비스가 ONNX Runtime으로 모델 파일을 로드합니다.
4. **입력 특징(Feature)**: 길이·너비 네 숫자를 `float[1][4]` 텐서로 전달합니다.
5. **예측 결과(Prediction)**: 품종 세 가지의 확률을 출력하고 가장 높은 확률을 가진 품종을 선택합니다.

모델은 직접 학습된 **실제 머신러닝 모델**이며 단순 if/else 규칙으로 품종을 선택하지 않습니다.

## 2. 환경 및 프로젝트 구조

| 항목 | 설명 |
|---|---|
| Java | JDK 17 이상 |
| Spring Boot | 4.1.1 |
| ONNX Runtime | 1.23.0, CPU 추론 |
| MyBatis | Spring Boot Starter 4.1.0 |
| Oracle | `ojdbc11` (DB 실습 전용) |
| Tess4J | 5.2.1 (OCR 실습 전용) |
| AI 모델 파일 | `src/main/resources/models/iris-logreg.onnx` |
| 테스트용 DB | H2 (Oracle 설치 불필요) |

```text
src/main/java/kopo/poly/
├─ SpringAiBasicApplication.java        # CommandLineRunner 실습 모드
├─ service/impl/IrisInferenceService.java # ONNX 파일 로딩·추론
├─ service/impl/OcrService.java         # 이미지 문자 인식
├─ service/impl/StudentService.java     # CRUD
├─ dto/                                # OCR·학생 정보 DTO
└─ mapper/                             # MyBatis 인터페이스

src/main/resources/
├─ models/iris-logreg.onnx             # 사전 학습된 실제 AI 모델
├─ image/sample01.jpg                 # OCR 테스트 이미지
├─ mapper/StudentMapper.xml           # 학생 데이터 CRUD SQL
└─ application.properties              # 설정 및 예제 입력값

scripts/train_iris_model.py            # 모델 재학습 및 ONNX 내보내기
src/test/java/kopo/poly/
├─ IrisInferenceServiceTests.java      # 실제 ONNX 모델 추론 테스트
└─ SpringAiBasicApplicationTests.java  # H2 데이터베이스 CRUD 테스트
```

## 3. 빠른 실습 — AI 모델 추론

**Windows (PowerShell)**

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=iris"
```

**macOS / Linux**

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--demo.mode=iris
```

기본 입력값은 다음과 같습니다.

| 특징(feature) | 값 | 의미 |
|---|---:|---|
| sepal-length | 5.1 | 꽃받침 길이(cm) |
| sepal-width | 3.5 | 꽃받침 너비(cm) |
| petal-length | 1.4 | 꽃잎 길이(cm) |
| petal-width | 0.2 | 꽃잎 너비(cm) |

출력은 `setosa`, `versicolor`, `virginica`의 확률과 최종 예측 품종입니다. 모델의 높은 확률은 정답이 보장된다는 의미가 아닙니다.

### 입력값을 바꾸는 실습

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=iris --iris.sepal-length=6.9 --iris.sepal-width=3.1 --iris.petal-length=5.4 --iris.petal-width=2.1"
```

명령의 수치를 자유롭게 바꾸어 세 품종의 예측 확률이 어떻게 바뀌는지 살펴보세요.

**실습 문항:** 꽃잎 길이 `petal-length`만 1.4 → 4.5 → 5.4로 바꾸면 품종별 확률이 어떻게 달라질까요? 나머지 수치는 동일하게 유지하여 변화 원인을 설명해 보세요.

## 4. 코드 흐름

```java
// Spring에서 @Service로 관리하는 IrisInferenceService
// 모델은 앱 기동 시 classpath:models/iris-logreg.onnx에서 로드합니다.

var result = irisInferenceService.predict(5.1f, 3.5f, 1.4f, 0.2f);
System.out.println(result.species());       // 예상: setosa
System.out.println(result.confidence());    // 가장 높은 예측 확률
System.out.println(result.probabilities()); // 세 품종의 확률
```

`IrisInferenceService`에서 수행하는 과정:

1. `ClassPathResource`를 이용해 ONNX 모델 바이트를 읽습니다.
2. `OrtEnvironment`와 `OrtSession`으로 모델을 메모리에 로드합니다.
3. 네 개의 입력값을 `OnnxTensor`로 변환합니다.
4. `session.run(...)`을 호출합니다.
5. `float[1][3]` 확률을 품종별로 매핑하고 가장 높은 값을 찾습니다.
6. 텐서와 실행 결과는 `try-with-resources`로 해제하고, 세션은 종료 시 닫습니다.

이 과정에는 Python이 필요하지 않습니다. Java 애플리케이션만으로 추론합니다.

## 5. 모델을 다시 학습하기 (심화)

제공된 `iris-logreg.onnx` 모델은 다음과 같이 학습했습니다.

- 데이터: scikit-learn `load_iris()` (총 150개)
- 알고리즘: 다중 클래스 로지스틱 회귀
- 학습/검증 분할: 80% / 20%, stratified, `random_state=42`
- 학습한 분할과 모델로 계산한 검증 정확도: 약 **96.7%** (30개 검증 샘플 중 29개 정답)
- 모델 출력: 확률이 합계 약 1이 되는 `[1,3]` 텐서

다른 데이터 분할, 모델, scikit-learn 버전에서는 정확도가 달라질 수 있습니다.

Python으로 학습부터 직접 따라 하려면:

```bash
python -m pip install scikit-learn numpy onnx
python scripts/train_iris_model.py
```

스크립트가 동일한 경로에 ONNX 모델을 다시 저장합니다. 변경한 파일은 Git에 반영하기 전에 테스트하세요. **Python은 재학습 때만 필요**합니다.

## 6. OCR 실습 (별도 예제)

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=ocr"
```

`src/main/resources/image/sample01.jpg`에서 한글 텍스트를 인식합니다. 별도로 설치하거나 내려받은 `kor.traineddata`를 포함하는 디렉터리를 `OCR_TESSDATA_PATH` 환경변수로 지정하세요.

예: Windows에서는 `C:/model/tessdata/kor.traineddata`가 존재한다면 `OCR_TESSDATA_PATH=C:/model/tessdata`로 설정합니다. 기존의 고정 경로 사용을 제거했습니다.

**OCR은 이미지에서 글자를 추출하는 예제이고, Iris는 학습된 모델을 사용하는 분류 예제입니다. 서로 다른 실습입니다.**

## 7. MyBatis / Oracle 학생 CRUD (별도 예제)

```sql
CREATE TABLE STUDENT (
    USER_ID   VARCHAR2(100) PRIMARY KEY,
    USER_NAME VARCHAR2(100),
    EMAIL     VARCHAR2(200),
    ADDR      VARCHAR2(200)
);
```

Windows PowerShell 환경변수 설정 및 실행:

```powershell
$env:DB_URL = "jdbc:oracle:thin:@127.0.0.1:1521/xe"
$env:DB_USERNAME = "poly"
$env:DB_PASSWORD = "본인의_DB_비밀번호"
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=student"
```

MyBatis의 `IStudentMapper` 메서드는 `StudentMapper.xml`의 동일한 `id`에 매핑됩니다. `#{userId}`와 같은 바인딩 방식으로 쿼리 매개변수를 전달합니다. 실습에서는 `demo_...` 고유 학생 ID로만 새 데이터를 만들고 수정·삭제합니다. 운영 DB가 아닌 실습 DB를 사용하세요.

## 8. 테스트

```bash
# Windows
./mvnw.cmd clean verify

# macOS / Linux
./mvnw clean verify
```

- `IrisInferenceServiceTests`: 실제 모델 파일을 ONNX Runtime으로 읽고 품종 세 개의 추론 결과, 확률 합계, 잘못된 입력을 테스트합니다.
- `SpringAiBasicApplicationTests`: H2 DB를 사용해 MyBatis XML의 INSERT/SELECT/UPDATE/DELETE를 검증합니다.
- GitHub Actions가 JDK 17로 `verify`를 수행합니다.

> 추론에 CPU용 ONNX Runtime의 네이티브 라이브러리를 사용합니다. 지원 플랫폼과 아키텍처를 확인하세요. 모델 파일은 작지만 처음 Maven 의존성을 설치할 때 ONNX Runtime 라이브러리 다운로드는 별도로 필요합니다.

## 9. 학생 과제

1. 꽃받침 길이는 고정하고 꽃잎 길이를 바꿔 품종별 확률 그래프를 작성합니다.
2. `sepal-length` / `petal-width` 순서를 바꿨을 때 결과가 왜 틀리는지 설명합니다.
3. `float[1][4]` 입력과 `float[1][3]` 출력 형태를 설명합니다.
4. `train_iris_model.py`의 학습·검증 비율을 변경해 정확도를 비교합니다.
5. 과적합, 과소적합, 학습 데이터와 추론 데이터의 차이를 조사합니다.

## 참고

- [ONNX Runtime Java 공식 문서](https://onnxruntime.ai/docs/get-started/with-java.html)
- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/reference/)
- [scikit-learn Iris 데이터셋](https://scikit-learn.org/stable/auto_examples/datasets/plot_iris_dataset.html)
- [Tess4J](https://github.com/nguyenq/tess4j)

한국폴리텍대학 서울강서캠퍼스 빅데이터소프트웨어공학과  
문의: hglee67@kopo.ac.kr
