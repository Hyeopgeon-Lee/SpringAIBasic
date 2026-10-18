# SpringAIBasic — Spring Boot 4 + 한국어 AI 감성분석

**Java 17 / Spring Boot 4.1.1**로 기존에 학습한 **koSent 긍정·부정 분류 모델**을 호출하는 교육용 프로젝트입니다.

- AI 감성분석: [koSent](https://github.com/Hyeopgeon-Lee/koSent) **Keras TextCNN 모델**에 HTTP/JSON 요청을 보냅니다.
- OCR: Tesseract/Tess4J로 이미지 안의 한글을 읽는 독립 실습입니다.
- DB: MyBatis/Oracle로 학생 데이터를 추가·조회·수정·삭제하는 독립 실습입니다.
- **붓꽃(Iris) ONNX 모델과 KOMORAN 형태소 분석 코드는 제거했습니다.**

> **중요:** 이 Spring Boot 저장소는 Keras 모델을 직접 실행하지 않습니다. 별도 실행 중인 **koSent FastAPI**가 Python/TensorFlow로 모델을 로드하고 한국어 전처리 및 추론을 수행합니다. Java는 Spring `RestClient`로 API를 호출하여 실제 모델의 결과를 출력합니다. 따라서 AI 실습을 하려면 koSent 서버를 먼저 실행해야 합니다. 모델을 ONNX로 변환했다고 주장하지 않습니다.

## 1. 교육 목표

1. 텍스트를 수치로 변환하는 전처리와 **TextVectorization**의 역할 이해
2. 학습(Training), 검증(Evaluation), 추론(Inference)의 구분
3. Python의 Keras 모델과 Java 기반 Spring Boot의 서비스 분리 구조 이해
4. Spring Boot 4의 **RestClient**, HTTP POST, JSON 요청과 응답 처리
5. 모델의 `score`, 긍정/부정 `label` 및 분류 임계값(threshold) 해석
6. 외부 모델 서버의 오류, 연결 실패, 입력 유효성 검사 처리

## 2. 모델: koSent 저장소에 이미 존재

[koSent 프로젝트](https://github.com/Hyeopgeon-Lee/koSent)에는 다음 학습 결과가 있습니다.

| 파일 | 용도 |
|---|---|
| `model/mySentimentModel.keras` | 약 48.6MB의 학습된 TensorFlow/Keras TextCNN 모델 |
| `model/eval_meta.json` | 학습·검증 메타 및 권장 임계값 |
| `serve.py` | FastAPI 웹 API 서버 |
| `data/ratings.txt` | 한국어 리뷰 학습 데이터 |
| `ko_sent_model.py` | 모델 재학습 소스코드 |

모델은 `TextVectorization`과 커스텀 한국어 정규화 함수를 포함합니다. Spring Boot에서 모델 파일만 그대로 복사해서 Java 라이브러리로 실행할 수는 없습니다. **이번 실습에서는 기존 Python API가 원래의 전처리·토크나이저 및 학습 가중치를 모두 재사용하도록 구성**했습니다.

모델 평가 자료 `koSent/model/eval_sent.json` (40,242개 검증 샘플):

| 지표 | 값 |
|---|---:|
| ROC-AUC | 0.8961 |
| PR-AUC | 0.8978 |
| 임계값 0.35일 때 정확도 | 79.20% |
| 임계값 0.35일 때 F1 | 0.8123 |
| 임계값 0.50일 때 정확도 | 79.75% |
| 임계값 0.50일 때 F1 | 0.7830 |

**예측 `score`는 긍정 클래스의 모델 출력값이지 무조건 맞을 확률이나 모델의 전체 정확도가 아닙니다.** `score >= threshold`이면 `pos`, 그렇지 않으면 `neg`입니다. 이 임계값은 koSent의 `eval_meta.json`에서 읽고 `THRESHOLD` 환경변수로 변경할 수 있습니다.

## 3. 시스템 구조

```text
학생이 한국어 문장 입력
        │
        ▼
Spring Boot 4 / Java 17
 └─ SpringAiBasicApplication
     └─ KoSentClient (@Service, Spring RestClient)
           │ HTTP POST /predict
           │ {"text":"정말 재미있었어요"}
           ▼
koSent FastAPI / Python 3.10
 ├─ TensorFlow / Keras TextVectorization
 ├─ model/mySentimentModel.keras (학습된 TextCNN)
 └─ {"label":"pos","score":...}
           │
           ▼
Spring Boot 콘솔에 긍정/부정 및 긍정 점수 출력
```

**Spring Boot는 API 호출자(클라이언트)**이며, koSent는 모델을 운영하는 API 서버입니다. 모델 다운로드나 학습을 반복하지 않습니다.

## 4. 실습 준비 및 실행 (서버 2개 프로세스)

### 단계 A — koSent API 먼저 실행

Python **3.10**을 설치한 후, 별도 터미널에서 실행합니다.

```bash
git clone https://github.com/Hyeopgeon-Lee/koSent.git
cd koSent

# Python 3.10의 가상 환경에서 수행하세요.
python -m pip install -r requirements.txt
python -X utf8 -m uvicorn serve:app --host 127.0.0.1 --port 8000
```

다른 Windows 가상환경 명령 예:

```powershell
py -3.10 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python -X utf8 -m uvicorn serve:app --host 127.0.0.1 --port 8000
```

실행 여부 확인: [http://127.0.0.1:8000/health](http://127.0.0.1:8000/health) (로컬에서만 접근 가능). `"ok": true`가 나와야 모델 로딩이 완료된 상태입니다.

API 단독 검증 (별도 터미널):

```bash
curl -X POST http://127.0.0.1:8000/predict -H "Content-Type: application/json" -d '{"text":"정말 재미있는 영화네요"}'
```

응답 구조: `{"label":"pos","score":0~1 사이 실수,"score_str":"..."}`  
위의 score 값은 설명을 위한 형식이며, **실제 점수는 모델이 계산**합니다.

### 단계 B — Spring Boot 4 클라이언트 실행

별도 터미널에서 Spring Boot 저장소를 실행합니다.

```bash
git clone https://github.com/Hyeopgeon-Lee/SpringAIBasic.git
cd SpringAIBasic
```

**Windows PowerShell**

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=sentiment"
```

**macOS / Linux**

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--demo.mode=sentiment
```

기본 입력 문장은 `정말 재미있고 감동적인 영화였습니다`입니다. 원하는 문장으로 변경할 수도 있습니다.

```powershell
./mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--demo.mode=sentiment --sentiment.text=스토리가 지루하고 연기도 아쉬웠어요'
```

세 문장을 한 번에 분석하는 배치 예제:

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo.mode=sentiment-batch"
```

### API 서버 주소를 변경할 때

기본 주소는 `http://127.0.0.1:8000`입니다. 다른 호스트 또는 포트를 사용하려면 `KOSENT_API_URL` 환경변수를 설정합니다.

```powershell
$env:KOSENT_API_URL = "http://127.0.0.1:8000"
```

일반 학습 PC에서는 같은 컴퓨터에서 두 프로그램을 실행하도록 권장합니다. 외부에 API를 공개할 경우 접근 제어, TLS, 네트워크 제한이 별도로 필요합니다.

## 5. 코드 핵심 설명

**KoSentClient.java**

```java
// JSON 요청을 작성하고, 모델 서버에서 응답을 역직렬화합니다.
Prediction result = client.post()
    .uri("/predict")
    .contentType(MediaType.APPLICATION_JSON)
    .body(Map.of("text", "정말 감동적인 영화였습니다"))
    .retrieve()
    .body(Prediction.class);
```

- `@Service`는 Spring이 서비스 객체를 관리한다는 의미입니다.
- `RestClient.Builder`는 Spring Boot 4의 `spring-boot-starter-restclient`가 제공합니다.
- `.post()`는 HTTP POST 요청이고, `/predict`는 기존 koSent의 실제 API입니다.
- `.body(...)`는 JSON 요청 객체를 만들고, `.body(Prediction.class)`는 JSON 응답을 Java record로 변환합니다.
- `score`는 긍정 점수이며, 임계값 기준 판단은 **koSent 서버**가 수행합니다.
- 빈 문장과 5,000자 초과 입력은 Java 클라이언트에서 먼저 거부합니다.

**데이터 전달 DTO**

```java
public record Prediction(
    String label,
    Double score,
    String score_str,
    Integer index
) {}
```

단건 응답에는 `index`가 없으므로 `null`이며, 배치 응답에는 입력 순서에 대응하는 정수 `index`가 제공됩니다.

## 6. 실제 실습에서 주의할 부분

- Java에서 `koSent/model/mySentimentModel.keras`를 직접 여는 코드는 없습니다. **이것은 Python API 연동 방식**입니다.
- 저장소의 원래 `custom_standardize`와 `TextVectorization` 동작을 유지하므로 Java 자체 전처리에서 생길 수 있는 불일치를 피합니다.
- `0.35`는 koSent 모델이 평가에서 선택한 F1 임계값이며, 점수를 무조건 0.5 이상일 때 긍정으로 판단하면 원래의 API와 결과가 달라질 수 있습니다.
- 원본 koSent 실행에는 Python 3.10과 TensorFlow 의존성 설치가 필요합니다.
- 단순 `SpringAIBasic` 빌드·테스트에는 koSent 서버가 필요하지 않습니다.

## 7. 테스트 및 검증

```bash
./mvnw clean verify
```

Windows는 `./mvnw.cmd clean verify`를 실행하세요.

- `KoSentClientTests`: 실제 HTTP 요청/JSON 응답을 **로컬 테스트 서버로 모의**하여 긍정·부정 응답, 배치 순서, 입력값 검증을 테스트합니다.
- `SpringAiBasicApplicationTests`: H2 메모리 DB에서 MyBatis SQL CRUD를 검증합니다.
- GitHub Actions가 JDK 17 환경에서 `mvn verify`를 실행합니다.

**구분해야 할 점:** Java의 HTTP API 테스트가 통과하는 것과 실제 Python Keras 모델을 실행해서 긍정·부정 결과가 정상적으로 출력되는 것은 별개의 검증입니다. 실제 모델을 이용한 전체 시스템 검증은 양쪽 프로그램을 실행한 뒤 수행해야 합니다.

## 8. 확장 과제

1. 같은 문장의 긍정 점수와 `label`을 저장하고, 문장별 차이를 설명합니다.
2. `THRESHOLD`를 0.35에서 0.5로 변경하여 결과를 비교합니다.
3. 비슷한 의미의 존댓말과 반말 문장을 분석해 모델의 표현 민감도를 확인합니다.
4. 긍정·부정 단어가 동시에 있는 문장을 넣고 모델의 오분류 원인을 조사합니다.
5. Java의 HTTP 예외 처리 및 koSent 서버 장애 시 사용자에게 적절한 오류를 보여주는 기능을 구현합니다.

## 9. 함께 제공되는 기존 실습

| 모드 | 기능 | 준비사항 |
|---|---|---|
| `none` | 기본 애플리케이션 기동 | Java 17 |
| `sentiment` | 한국어 1문장 긍정·부정 예측 | koSent FastAPI 실행 |
| `sentiment-batch` | 한국어 3문장 배치 예측 | koSent FastAPI 실행 |
| `ocr` | `sample01.jpg` 문자 인식 | Tesseract `kor.traineddata` |
| `student` | MyBatis Oracle CRUD | Oracle DB 및 STUDENT 테이블 |

학생 CRUD / OCR 실습 설정은 기존 README 구성과 같습니다. 보안상 DB 비밀번호는 `DB_PASSWORD` 환경변수로 설정하고 운영 DB 대신 실습 DB를 사용합니다.

## 참고 문서

- [koSent 학습 모델 및 FastAPI 저장소](https://github.com/Hyeopgeon-Lee/koSent)
- [Spring Boot 4 공식 REST 클라이언트 문서](https://docs.spring.io/spring-boot/reference/io/rest-client.html)
- [FastAPI](https://fastapi.tiangolo.com/)
- [TensorFlow/Keras](https://www.tensorflow.org/guide/keras)

한국폴리텍대학 서울강서캠퍼스 **빅데이터소프트웨어공학과**  
문의: hglee67@kopo.ac.kr
