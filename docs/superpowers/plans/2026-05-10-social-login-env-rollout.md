# 운영 소셜 로그인 환경 보강 계획

## 목표

운영 배포 시 Google, Kakao 앱 로그인용 OIDC audience와 SayNow token secret이 EC2 서비스 환경파일에 주입되도록 보강한다.

## 범위

- AWS Parameter Store `/saynow/prod`의 Google, Kakao audience 값을 앱 로그인 기준으로 갱신한다.
- 배포 워크플로가 인증 관련 SSM 파라미터를 `/opt/saynow/.env`에 포함하도록 수정한다.
- 소셜 로그인 구현 로직 자체는 바꾸지 않는다.
- 사용자가 전달한 key 원문은 리포지토리 파일에 기록하지 않는다.

## 성공 기준

1. Parameter Store의 Google audience에 web client id와 Android client id가 모두 포함된다.
2. Parameter Store의 Kakao audience에 REST API key와 Native App Key가 모두 포함된다.
3. Parameter Store의 `SAYNOW_AUTH_TOKEN_SECRET`이 존재하고 기본 개발값이 아니다.
4. 배포 워크플로의 필수 환경변수 목록에 소셜 로그인 설정 3개가 포함된다.
5. 인증 관련 테스트와 워크플로 문법 검사를 통과한다.
