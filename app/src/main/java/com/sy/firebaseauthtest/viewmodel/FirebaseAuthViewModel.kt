package com.sy.firebaseauthtest.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID
import com.google.firebase.auth.EmailAuthProvider


class FirebaseAuthViewModel: ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signUp(email: String, password: String) {

        //유효성 검사
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //회원 가입 성공 이후 이메일 인증 발송
                    sendEmailVerification()
                } else {
                    _authState.value = AuthState.Error(
                        task.exception?.message ?: "회원가입 실패"
                    )
                }
            }
    }

    //이메일 인증 발송
    private fun sendEmailVerification() {
        val user = auth.currentUser

        Timber.tag("EmailVerification").d("email verification sent")
        Timber.tag("EmailVerification").d("user email: ${user?.email}")
        Timber.tag("EmailVerification").d("user UID: ${user?.uid}")

        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.tag("EmailVerification").d("email verification send success")
                _authState.value = AuthState.Success("회원 가입 성공 \n이메일 인증 메일을 보냈습니다")
            } else {
                Timber.tag("EmailVerification").e("이메일 발송 실패 ${task.exception}")
                _authState.value = AuthState.Error("이메일 발송 실패: ${task.exception?.message}")
            }
        }
    }

    //이메일 인증 재발송
    fun resendEmailVerification() {
        val user = auth.currentUser

        if (user == null) {
            _authState.value = AuthState.Error("로그인이 필요합니다.")
            return
        }

        if(user.isEmailVerified) {
            _authState.value = AuthState.Success("이미 이메일 인증이 완료되었습니다.")
            return
        }

        _authState.value = AuthState.Loading

        Timber.tag("EmailVerification").d("Email Verfication resent")

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.tag("EmailVerification").d("email verification resend success")
                    _authState.value = AuthState.Success("인증 이메일을 재발송 하였습니다.\n이메일을 확인하세요.")
                } else {
                    Timber.tag("EmailVerification").e("이메일 재발송 실패 ${task.exception}")
                    _authState.value = AuthState.Error(getErrorMessage(task.exception))
                }
            }
    }

    // 이메일 인증 상태 새로고침
    fun reloadUser(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser

        Timber.tag("EmailVerification").d("=== 사용자 정보 새로고침 ===")

        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isVerified = user.isEmailVerified
                Timber.tag("EmailVerification").d( "인증 상태: $isVerified")
                onComplete(isVerified)
            } else {
                Timber.tag("EmailVerification").d("새로고침 실패 ${task.exception}")
                onComplete(false)
            }
        }
    }

    fun signIn(email: String, password: String) {

        //유효성 검사
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            return
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    //이메일 인증 체크
                    if (user?.isEmailVerified == false) {
                        Timber.tag("EmailVerification").d("이메일 미인증 사용자 로그인 차단")
                        _authState.value = AuthState.Error("이메일 인증이 필요합니다.\n\n${user.email}로 발송된 인증 메일을 확인 하세요.")
                        auth.signOut()
                    } else {
                        Timber.tag("EmailVerification").d("이메일 인증 완료")
                        _authState.value = AuthState.Success("로그인 성공!")
                    }
                } else {
                    _authState.value = AuthState.Error(
                        getErrorMessage(task.exception)
                    )
                }
            }
    }

    //이메일 주소 유효성 검사
    private fun validateInput(email: String, password: String) : String? {
        when {
            email.isBlank() -> return "이메일을 입력하세요"
            !email.contains("@") -> return "올바른 이메일 형식이 아닙니다"
            !email.contains(".") -> return "올바른 이메일 형식이 아닙니다"
        }

        return validatePassword(password)
    }

    //비밀번호 유효성 검사
    private fun validatePassword(password: String) : String? {
        if(password.isBlank()) {
            return "비밀번호를 입력하세요."
        }

        if(password.length < 8) {
            return "비밀번호는 최소 8자 이상입니다."
        }

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        val missing = mutableListOf<String>()
        if (!hasUpperCase) missing.add("대문자")
        if (!hasLowerCase) missing.add("소문자")
        if (!hasDigit) missing.add("숫자")
        if (!hasSpecialChar) missing.add("특수문자")

        return if (missing.isNotEmpty()) {
            "비밀번호에 ${missing.joinToString(", ")}를 포함하세요."
        } else {
            null
        }
    }

    //firebase 에러 메세지 한글화
    private fun getErrorMessage(exception: Exception?): String {
        return when {
            exception?.message?.contains("already in use") == true ->
                "이미 사용 중인 이메일입니다"
            exception?.message?.contains("invalid-email") == true ->
                "올바른 이메일 형식이 아닙니다"
            exception?.message?.contains("weak-password") == true ->
                "비밀번호가 너무 약합니다"
            exception?.message?.contains("user-not-found") == true ->
                "존재하지 않는 계정입니다"
            exception?.message?.contains("wrong-password") == true ->
                "비밀번호가 틀렸습니다"
            exception?.message?.contains("network") == true ->
                "네트워크 연결을 확인하세요"
            exception?.message?.contains("too-many-requests") == true ->
                "너무 많은 시도가 있었습니다. 잠시 후 다시 시도하세요"
            else -> exception?.message ?: "알 수 없는 오류가 발생했습니다"
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("이메일을 입력하세요")
            return
        }

        if (!email.contains("@")) {
            _authState.value = AuthState.Error("올바른 이메일 형식이 아닙니다")
            return
        }

        _authState.value = AuthState.Loading

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success(
                        "비밀번호 재설정 이메일을 전송했습니다.\n이메일을 확인하세요."
                    )
                } else {
                    _authState.value = AuthState.Error(
                        getErrorMessage(task.exception)
                    )
                }
            }
    }

    fun updateEmail(newEmail: String, password: String) {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.Error("로그인이 필요합니다.")
            return
        }

        if (newEmail.isBlank()) {
            _authState.value = AuthState.Error("이메일을 입력하세요.")
            return
        }

        if (!newEmail.contains("@")) {
            _authState.value = AuthState.Error("올바른 이메일 형식이 아닙니다,")
        }

        _authState.value = AuthState.Loading

        Timber.tag("EmailVerification").d("Email Change Update")
        Timber.tag("EmailVerification").d("old email: ${user.email}")
        Timber.tag("EmailVerification").d("new email: $newEmail")

        //재인증
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    Timber.d("Email Update Success")

                    //이메일 변경 및 인증 메일 발송
                    user.verifyBeforeUpdateEmail(newEmail)
                        .addOnCompleteListener { updateTask ->
                            if(updateTask.isSuccessful) {
                                Timber.d("인증 메일 발송 성공")
                                _authState.value = AuthState.Success(
                                    "인증 이메일을 발송 하였습니다.\n $newEmail 에서 이메일을 확인 하고 인증을 완료하세요."
                                )
                            } else {
                                Timber.e("이메일 변경 실패 ${updateTask.exception}")
                                _authState.value = AuthState.Error(
                                    getErrorMessage(updateTask.exception)
                                )
                            }
                        }
                } else {
                    Timber.e("재인증 실패 ${reauthTask.exception}")
                    _authState.value = AuthState.Error(
                        "비밀번호가 틀렸습니다."
                    )
                }
            }
    }


    //google 계정 연동 로그인 이메일 주소 비밀번호 입력 필요 없음
    suspend fun signInWithGoogle(context: Context) {
        try {
            _authState.value = AuthState.Loading

            val credentialManager = CredentialManager.create(context)

            //Nonce 생성 (보안용)
            val ranNonce = UUID.randomUUID().toString()
            val bytes = ranNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            //google id option setting
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("689792089134-rdv9n0l7jc1ib9enlaq6e9msolku7gl5.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            val credential = result.credential

            //Google Id Token 추출
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)

            val googleIdToken = googleIdTokenCredential.idToken

            //Firebase 인증
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            auth.signInWithCredential(firebaseCredential).await()

            _authState.value = AuthState.Success("Google 로그인 성공!")
        } catch (e: GetCredentialException) {
            Timber.e("Credential error $e")
            _authState.value = AuthState.Error("Google 로그인 취소 또는 실패")
        } catch (e: Exception) {
            Timber.e("Sign in error $e")
            _authState.value = AuthState.Error(getErrorMessage(e))
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun getCurrentUser() = auth.currentUser

}

//로그인 상태 클래스
sealed class AuthState {
    object Idle: AuthState()
    object Loading: AuthState()
    data class Success(val message: String): AuthState()
    data class Error(val message: String): AuthState()
}