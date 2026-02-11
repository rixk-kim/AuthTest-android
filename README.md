# AuthTest-android

Firebase 및 Supabase 인증 학습 프로젝트 (Android)

### Firebase Auth
- [x] 이메일/비밀번호 회원가입
- [x] 이메일/비밀번호 로그인  
- [x] Google 소셜 로그인 (Credential Manager API)
- [x] 이메일 인증
- [x] 비밀번호 재설정
- [x] 이메일 주소 변경
- [x] 자동 로그인
- [x] 입력 유효성 검사

### Supabase Auth
- [ ] 준비 중

## 🛠 기술 스택

- Kotlin
- Jetpack Compose
- Firebase Authentication
- MVVM Architecture
- Coroutines & StateFlow
- Material Design 3

## 📂 프로젝트 구조
```
app/src/main/java/com/sy/firebaseauthtest/
├── MainActivity.kt
├── FirebaseAuthActivity.kt
├── HomeActivity.kt
└── viewmodel/
    └── FirebaseAuthViewModel.kt
```

## 시작하기

### 1. Firebase 설정

1. [Firebase Console](https://console.firebase.google.com/)에서 프로젝트 생성
2. Android 앱 추가
3. 패키지명: `com.sy.firebaseauthtest`
4. `google-services.json` 다운로드
5. `app/` 폴더에 배치

### 2. SHA-1 인증서 등록 (Google Sign-In용)
```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

출력된 SHA-1을 Firebase Console에 등록

### 3. 빌드 & 실행
```bash
./gradlew installDebug
```

또는 Android Studio에서 Run

## 학습 내용

- Firebase Authentication 통합
- Jetpack Compose UI 구현
- MVVM 패턴 적용
- Coroutines를 활용한 비동기 처리
- StateFlow를 이용한 상태 관리
