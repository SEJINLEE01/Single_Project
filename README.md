# 비콘 기반 출석 관리 시스템

## 📋 프로젝트 개요
엑셈' 기업탐방 당시, 개발자들이 주도적으로 사내 불편 사항을 개선해 나가는 문화를 보고 영감을 받았습니다.   
이에 센터 내에서 가장 비효율적이었던 기존 '출석 시스템'을 직접 개선해 보고자 프로젝트를 발의했습니다.  
Android 앱과 서버 연동을 통한 실시간 데이터 처리 및 웹 대시보드 시각화 구현 했습니다.  

<img width="800" height="400" alt="image" src="https://github.com/user-attachments/assets/cfb1d629-6e33-4437-b88a-33bd6556a4cc" />

## 🛠 기술 스택
- **백엔드**: FastAPI, SQLite
- **모바일**: Android (Kotlin, Android Studio)
- **인프라**: ngrok (도메인 고정)
- **스케줄링**: APScheduler

## 📅 개발 타임라인

### Week 1: 기본 인프라 구축 (4/23 - 4/29)

**4월 23일 - 프로토타입**
- 버튼 기반 수동 스캔 방식 구현

**4월 27일 - 서버 & 앱 기본 구조**
- FastAPI + SQLite 서버 구축
- Android 앱 기본 구조 (회원가입, 로그인, 스캔 화면)
- JSON 형식으로 클라이언트-서버 간 데이터 송수신 구현

**4월 28일 - 아키텍처 개선**
- **문제**: Main 액티비티에서만 서버 연결 가능
- **해결**: Singleton 패턴으로 서버 인스턴스 전역 관리
- 자동로그인 기능 추가 (SharedPreferences 활용)
- ngrok으로 서버 도메인 고정 (테스트 편의성 향상)

**4월 29일 - 자동 스캔 도입 및 문제 해결**
- **기능**: 로그인 시 비콘 자동 스캔 시작
- **문제**: 출석 직후 즉시 퇴실 처리되는 현상
- **해결**: 
  - 출석 후 일정 시간 동안 재스캔 방지 로직
  - 스캔 완료 시 자동으로 스캔 중지
  - 설정 시간 경과 시 자동으로 스캔 재개
- 로그 전송 로직 함수화로 코드 중복 제거

### Week 2: 상태 관리 고도화 (5/2 - 5/8)

**5월 2일 - 상태 관리 아키텍처 변경**
- **기존**: 시간 기반으로 출석/퇴실 구분
- **개선**: DB에 사용자 상태 플래그 추가 (1: 출석, 0: 퇴실)
- **효과**: 서버에서 상태 관리, 앱에서 시간 관리로 역할 분리

**5월 3일 - 관리자 대시보드**
- 29개 좌석 실시간 출석 현황 표시
- 관리자 모드 ON/OFF 기능

**5월 6일 - 관리자 기능 확장**
- 회원 정보 조회 (모달 방식)
- 좌석 변경 및 스왑 기능
- 회원 삭제 기능

**5월 7일 - 설정 중앙화**
- 좌석번호 유효성 검증 (1-29 범위) 버그 수정
- 조기 퇴실 시 확인 다이얼로그 추가
- **핵심 개선**: 입실/퇴실 시간, 총 좌석 수를 서버에서 관리
  - 앱 재배포 없이 서버 설정만 변경 가능

**5월 8일 - 동적 관리 시스템**
- 관리자 권한으로 좌석 배치 동적 변경
- 일일 출석 상태 자동 리셋 (APScheduler)

### Week 3: 출결 로직 완성 (5/10 - 5/12)

**5월 10일 - 출결 분류 시스템**
행위별 데이터 테이블 추가 및 자동 집계:
- **출석**: 정상 입실 → 정상 퇴실
- **조퇴**: 정상 입실 → 조기 퇴실
- **지각**: 늦은 입실 → 정상 퇴실
- **결석**: 당일 미출석 (스케줄러로 자정에 자동 판정)

**5월 11-12일 - 완성도 향상**
- 버그 수정 및 안정화
- 학생 정보 조회 페이지
- 로그 추적 페이지 추가

## 🎬 실행 영상

<table>
  <tr>
    <td align="center">
      <b>앱 실행 영상 1</b><br>
      <video src="https://github.com/user-attachments/assets/45996728-799b-4cdf-af05-edaadeb69186" controls width="250"></video>
    </td>
    <td align="center">
      <b>앱 실행 영상 2</b><br>
      <video src="https://github.com/user-attachments/assets/746279c5-4056-4842-8f20-fd892279e8a3" controls width="250"></video>
    </td>
    <td align="center">
      <b>앱 실행 영상 3</b><br>
      <video src="https://github.com/user-attachments/assets/cd7eb9f5-3772-4c12-94ab-0c9ed4e19954" controls width="250"></video>
    </td>
    <td align="center">
      <b>앱 실행 영상 4</b><br>
      <video src="https://github.com/user-attachments/assets/50e4b010-7f79-4a0c-bbc4-4d4b73af03ca" controls width="250"></video>
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>웹 대시보드 1</b><br>
      <video src="https://github.com/user-attachments/assets/61e2c704-1004-4eb6-9c5c-cf3e669419c7" controls width="250"></video>
    </td>
    <td align="center">
      <b>웹 대시보드 2</b><br>
      <video src="https://github.com/user-attachments/assets/22151f7a-1951-4360-8105-b248e85f7529" controls width="250"></video>
    </td>
    <td align="center">
      <b>웹 대시보드 3</b><br>
      <video src="https://github.com/user-attachments/assets/c0271fa2-01ce-491f-a10c-ce96c0a33365" controls width="250"></video>
    </td>
    <td></td>
  </tr>
</table>
<table>
  <tr>
    <td align="center"><b>📱 지각 처리 시 앱 화면</b></td>
    <td align="center"><b>🗄️ 실제 학생 출결관리 DB 테이블</b></td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/4aa7b8bf-9df2-414d-9dbf-390ed6eae192" width="200" alt="지각 앱 화면">
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/4ad6bc51-5b3e-46c9-99b2-940991de358b" width="700" alt="학생 정보 DB">
    </td>
  </tr>
</table>


## 🎯 주요 기능

### 아키텍쳐
<img width="2430" height="1728" alt="Gemini_Generated_Image_t1j0mvt1j0mvt1j0" src="https://github.com/user-attachments/assets/14aaf3a2-ffac-4525-8a8f-caaf97a10ebf" />

### 사용자 기능
- 비콘 자동 스캔을 통한 무인 출결 체크
- 자동 로그인 (SharedPreferences)
- 실시간 출석 상태 확인

### 관리자 기능
- 29석 규모 좌석 대시보드
- 회원 관리 (조회, 삭제, 좌석 배정)
- 동적 좌석 배치 변경
- 출결 통계 (출석/조퇴/지각/결석)
- 사용자별 로그 추적

### 시스템 기능
- 일일 출석 상태 자동 리셋
- 서버 중심 설정 관리 (앱 업데이트 불필요)
- 중복 스캔 방지 로직

## 🔧 주요 기술적 개선

1. **Singleton 패턴**: 서버 인스턴스를 앱 전역에서 접근 가능하도록 설계
2. **상태 관리 분리**: 서버(상태) + 클라이언트(시간) 역할 분담
3. **설정 중앙화**: 하드코딩 제거, 서버 API로 설정값 제공
4. **스케줄링**: APScheduler로 자동화된 일일 집계 및 리셋
5. **사용자 경험**: 자동 스캔 on/off로 배터리 효율 개선
6. **비콘 통신**: Bluetooth LE 기반 자동 감지

## 💡 개발 과정에서 배운 점

- **문제 해결**: 자동 스캔으로 인한 중복 처리 문제를 시간 제어로 해결
- **아키텍처 설계**: 초기 시간 기반 로직을 상태 기반으로 리팩토링하며 확장성 확보
- **유지보수성**: 서버 중심 설정 관리로 앱 재배포 최소화
- **데이터 모델링**: 출석/조퇴/지각/결석을 체계적으로 분류하는 로직 설계
