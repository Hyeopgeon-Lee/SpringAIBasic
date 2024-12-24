# 🌱 Spring Boot Frameworks 기반 AI 기초 프로그래밍 실습

> **Java 기반 AI 프로그래밍 실습 코드**  
> 공유되는 프로그래밍 코드는 한국폴리텍대학 서울강서캠퍼스 빅데이터과 수업에서 사용된 코드입니다.

---

### 📚 **작성자**
- **한국폴리텍대학 서울강서캠퍼스 빅데이터과**  
- **이협건 교수**  
- ✉️ [hglee67@kopo.ac.kr](mailto:hglee67@kopo.ac.kr)  
- 🔗 [빅데이터학과 입학 상담 오픈채팅방](https://open.kakao.com/o/gEd0JIad)

---

## 🚀 주요 실습 내용

1. **Tesseract를 이용하여 이미지의 객체 인식하기**
2. **Komoran을 이용하여 형태소 분석 및 명사 추출하기**
3. **MyBatis를 이용한 JDBC 프로그래밍**

---

## 🛠️ 주요 적용 프레임워크

- **Spring Boot Frameworks**
- **Tesseract**
- **Komoran**
- **MyBatis**
- **Oracle Database**

---

## 📩 문의 및 입학 상담

- 📧 **이메일**: [hglee67@kopo.ac.kr](mailto:hglee67@kopo.ac.kr)  
- 💬 **입학 상담 오픈채팅방**: [바로가기](https://open.kakao.com/o/gEd0JIad)

---

## 💡 **우리 학과 소개**
- 한국폴리텍대학 서울강서캠퍼스 빅데이터과는 **클라우드 컴퓨팅, 인공지능, 빅데이터 기술**을 활용하여 소프트웨어 개발자를 양성하는 학과입니다.  
- 학과에 대한 더 자세한 정보는 [학과 홈페이지](https://www.kopo.ac.kr/kangseo/content.do?menu=1547)를 참고하세요.

---

## 📦 **설치 및 실행 방법**

### 1. 레포지토리 클론
- 아래 명령어를 실행하여 레포지토리를 클론합니다.

```bash
git clone https://github.com/Hyeopgeon-Lee/SpringAIBasic.git
cd SpringAIBasic
```

### 2. Oracle Database 설정
- application.yml 또는 application.properties 파일에서 Oracle Database 설정 정보를 업데이트합니다.

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:xe
    username: your_username
    password: your_password
```

### 3. 의존성 설치
- 아래 명령어를 실행하여 Maven 의존성을 설치합니다:
  
```bash
mvn clean install
```

### 4. 애플리케이션 실행
아래 명령어를 실행하여 애플리케이션을 시작합니다.

```bash
mvn spring-boot:run
```
