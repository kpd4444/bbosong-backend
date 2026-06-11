# 세탁 메이트 뽀송이 - AI 의류 관리 서비스

<div align="center">
  <h1>뽀송이 - 옷을 오래도록 뽀송하게</h1>
  <p>🧺 AI 기반 맞춤형 의류 관리 도우미 🧺</p>
</div>

<br/>

<div align="center">
  <!-- 뽀송이 메인 화면 또는 서비스 대표 배너 -->
  <img width="2108" height="1202" alt="뽀송이" src="https://github.com/user-attachments/assets/089f826e-08c0-406c-9582-3cfa1a0106dd" />

</div>

<br/>

<div align="center">
  <a href="https://bbosongi.com">홈페이지</a>
    |  
  <a>Swagger</a>
    |  
  <a href="https://www.figma.com/design/YlKfu09rP60qLw071ovyH5/%EB%BD%80%EC%86%A1%EC%9D%B4?node-id=0-1&t=zTUFQWzmvpM4EKQ7-1">Figma</a>
</div>

---

## ✍️ 프로젝트 개요

- **프로젝트명:** 뽀송이
- **프로젝트 기간:** 2025-09-01 ~ 2026-05-29
- **프로젝트 형태:** WEB 서비스
- **서비스 상태:** 운영 중
- **목표:** AI 이미지 분석과 날씨 정보를 활용하여 사용자에게 맞춤형 의류 관리 방법을 제공하는 서비스 구축
- **주요 타겟 사용자:**
  - 올바른 의류 세탁 및 관리 방법을 알고 싶은 사용자
  - 보유한 의류를 디지털 옷장으로 관리하고 싶은 사용자
  - 날씨에 적합한 세탁 및 건조 방법을 추천받고 싶은 사용자

---

## ✍️ 프로젝트 소개

### 프로젝트 배경

의류는 소재와 형태에 따라 적절한 세탁 및 건조 방법이 다릅니다. 하지만 사용자가 모든 의류의 소재별 관리법을 직접 확인하기는 어렵고, 잘못된 세탁으로 인해 수축·이염·형태 변형 등의 손상이 발생할 수 있습니다.

또한 세탁 여부를 결정할 때는 옷의 소재뿐만 아니라 기온, 습도, 강수확률과 같은 날씨 정보도 함께 고려해야 합니다.

뽀송이는 이러한 문제를 해결하기 위해 의류 사진을 AI로 분석하고, 사용자에게 적합한 세탁 방법과 주의사항을 제공합니다. 분석한 의류는 디지털 옷장에 저장할 수 있으며, 날씨 기반 세탁 추천과 AI 의류 상담 기능도 함께 제공합니다.

### 사용자 니즈

👕 **의류 관리**

- 사진만으로 의류의 종류와 소재를 간편하게 확인하고 싶음
- 옷에 적합한 세탁·건조 방법을 빠르게 알고 싶음
- 잘못된 세탁으로 인한 의류 손상을 예방하고 싶음

🗄️ **옷장 관리**

- 보유한 의류를 한곳에서 관리하고 싶음
- 카테고리와 검색 기능으로 필요한 옷을 쉽게 찾고 싶음
- 자주 확인하는 의류를 즐겨찾기로 관리하고 싶음

🌦️ **세탁 및 건조**

- 오늘 빨래하기 적합한 날씨인지 알고 싶음
- 실내건조와 실외건조 중 적절한 방법을 추천받고 싶음
- 기온·습도·강수확률에 맞는 세탁 정보를 확인하고 싶음

---

## 🚀 프로젝트 목표

1. **AI를 활용한 간편한 의류 분석 및 맞춤형 세탁 가이드 제공**

2. **개인 의류를 효율적으로 관리할 수 있는 디지털 옷장 구축**

3. **날씨 정보를 활용한 실용적인 세탁 및 건조 방법 추천**

4. **AI 상담을 통한 의류 관리 정보 접근성 향상**

---

## ✨ 주요 기능

### 1. AI 기반 의류 이미지 분석

- 사용자가 촬영하거나 보유한 의류 이미지 업로드
- OpenAI Vision을 활용한 비동기 이미지 분석
- 의류 카테고리·이름·소재·색상 자동 추출
- 권장 세탁 방법 및 세탁 시 주의사항 제공
- 분석 작업의 진행 상태 및 결과 조회

<div align="center">
  <!-- 의류 이미지 업로드부터 분석 결과가 표시되는 과정을 보여주는 GIF 또는 이미지 -->
  <img src="AI 의류 분석 시연 이미지 URL" alt="AI 의류 분석 시연" width="80%"/>
</div>

<br/>

### 2. 나만의 디지털 옷장

- AI 분석 결과와 의류 이미지를 개인 옷장에 저장
- 상의·하의·아우터·침구류 등 카테고리별 조회
- 의류 상세 정보 조회 및 삭제
- 의류 이름 검색과 카테고리 필터링
- 자주 확인하는 의류 즐겨찾기
- 최근 등록 의류와 즐겨찾기 의류를 홈에서 제공

<div align="center">
  <!-- 옷장 목록, 카테고리 필터 및 의류 상세 화면 -->
  <img src="디지털 옷장 시연 이미지 URL" alt="디지털 옷장 시연" width="80%"/>
</div>

<br/>

### 3. AI 의류 관리 상담

- 텍스트 또는 이미지를 활용한 AI 의류 상담
- 세탁·건조·보관·얼룩 제거 방법 안내
- 이전 대화 내용을 반영한 연속 상담
- 상담 이미지와 대화 기록 저장
- 전체 상담 기록 조회 및 삭제

<div align="center">
  <!-- AI 채팅과 의류 이미지 상담 과정을 보여주는 GIF 또는 이미지 -->
  <img src="AI 상담 시연 이미지 URL" alt="AI 의류 상담 시연" width="80%"/>
</div>

<br/>

### 4. 날씨 기반 세탁 추천

- 사용자 위치를 기반으로 기상청 단기예보 조회
- 기온·습도·강수확률·하늘 상태 분석
- 실내건조·실외건조·제습 건조 등 맞춤 추천
- 두꺼운 빨래 가능 여부와 적절한 세탁량 안내

<div align="center">
  <!-- 현재 날씨와 세탁 추천 카드가 함께 보이는 화면 -->
  <img src="날씨 기반 세탁 추천 이미지 URL" alt="날씨 기반 세탁 추천" width="80%"/>
</div>

<br/>

### 5. 세탁소 즐겨찾기

- Kakao 지도 기반 세탁소 정보 활용
- 세탁소 이름·주소·전화번호·위치 정보 저장
- 자주 방문하는 세탁소 즐겨찾기 조회 및 삭제

<div align="center">
  <!-- 주변 세탁소 지도와 즐겨찾기 화면 -->
  <img src="세탁소 즐겨찾기 이미지 URL" alt="세탁소 즐겨찾기" width="80%"/>
</div>

<br/>

### 6. 회원가입 및 인증

- 일반 회원가입 및 로그인
- Google·Kakao 소셜 로그인
- JWT Access Token·Refresh Token 기반 인증
- 토큰 재발급 및 로그아웃
- 닉네임·생년월일 수정 및 회원 탈퇴

<div align="center">
  <!-- 로그인, 회원가입 및 소셜 로그인 화면 -->
  <img src="로그인 및 회원가입 이미지 URL" alt="로그인 및 회원가입" width="80%"/>
</div>

---

## 🧑‍💻 팀원 소개

| **이름** | **역할** | **담당 업무** |
|:--------:|:--------:|:-------------|
| <a href="https://github.com/kpd4444"><img src="https://github.com/kpd4444.png" width="70px"/><br/><sub><b>김태민</b></sub></a> | BE· Leader | 백엔드 아키텍처 설계, 회원·JWT/OAuth2 인증, AI 의류 분석 및 상담, 디지털 옷장, 날씨 기반 추천, AWS 인프라·CI/CD 구축 |
| <a href="https://github.com/hxxneei"><img src="https://github.com/hxxneei.png" width="70px"/><br/><sub><b>정지인</b></sub></a> | BE | Kakao 지도 API 연동, 서비스 모니터링, 동시 요청 처리 성능 테스트 |
| <a href="https://github.com/jiin-jung"><img src="https://github.com/jiin-jung.png" width="70px"/><br/><sub><b>나현지</b></sub></a> | FE | 프론트엔드 구현, UI/UX 설계 및 API 연동 |

---

## ⚙️ 기술 스택

<table>
  <thead>
    <tr>
      <th>분류</th>
      <th>기술 스택</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Backend</td>
      <td>
        <img src="https://img.shields.io/badge/Java_21-007396?style=flat&logo=openjdk&logoColor=white"/>
        <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white"/>
        <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat&logo=spring&logoColor=white"/>
        <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white"/>
        <img src="https://img.shields.io/badge/JWT-000000?style=flat&logo=jsonwebtokens&logoColor=white"/>
      </td>
    </tr>
    <tr>
      <td>Database & Storage</td>
      <td>
        <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white"/>
        <img src="https://img.shields.io/badge/AWS_S3-569A31?style=flat&logo=amazons3&logoColor=white"/>
        <img src="https://img.shields.io/badge/CloudFront-8C4FFF?style=flat&logo=amazonaws&logoColor=white"/>
      </td>
    </tr>
    <tr>
      <td>AI & External API</td>
      <td>
        <img src="https://img.shields.io/badge/Spring_AI-6DB33F?style=flat&logo=spring&logoColor=white"/>
        <img src="https://img.shields.io/badge/OpenAI-412991?style=flat&logo=openai&logoColor=white"/>
        <img src="https://img.shields.io/badge/기상청_API-005BAC?style=flat"/>
        <img src="https://img.shields.io/badge/Kakao_API-FFCD00?style=flat&logo=kakao&logoColor=black"/>
      </td>
    </tr>
    <tr>
      <td>Infra</td>
      <td>
        <img src="https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white"/>
        <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=flat&logo=amazonec2&logoColor=white"/>
        <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white"/>
      </td>
    </tr>
    <tr>
      <td>Documentation & Test</td>
      <td>
        <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black"/>
        <img src="https://img.shields.io/badge/JUnit5-25A162?style=flat&logo=junit5&logoColor=white"/>
        <img src="https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white"/>
      </td>
    </tr>
  </tbody>
</table>

---

## 📐 시스템 아키텍처

<div align="center">
  <!--
  포함할 내용:
  Client → Spring Boot API → MySQL
  Spring Boot → OpenAI API
  Spring Boot → 기상청 API
  Spring Boot → AWS S3 → CloudFront
  GitHub Actions → Docker Hub → AWS EC2
  -->
  <img width="1448" height="1086" alt="뽀송이시스템아키텍처" src="https://github.com/user-attachments/assets/0bd28696-573b-4c8b-b8f4-d4a1caa41eb5" />

</div>

---

## 🗄️ ERD

<div align="center">
  <!--
  포함할 테이블:
  Member, LocalAccount, SocialAccount, RefreshToken,
  Clothes, Category, ClothesAnalysisJob,
  ChatRoom, ChatMessage, Store, StoreFavorite
  -->
  <img width="1810" height="1000" alt="뽀송이erd" src="https://github.com/user-attachments/assets/13294c1c-1448-47b4-b889-16b0196dd64b" />

</div>

---

<div align="center">
  <h3>🧺 <strong>오늘도 오래도록, 뽀송하게.</strong> 🧺</h3>
  <p>뽀송이는 AI를 통해 더 쉽고 안전한 의류 관리를 돕습니다.</p>
</div>
