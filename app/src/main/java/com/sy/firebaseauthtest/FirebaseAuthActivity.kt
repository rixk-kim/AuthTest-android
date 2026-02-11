package com.sy.firebaseauthtest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.sy.firebaseauthtest.viewmodel.AuthState
import com.sy.firebaseauthtest.viewmodel.FirebaseAuthViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import timber.log.Timber

class FirebaseAuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && user.isEmailVerified) {
                // 자동 로그인 + 이메일 인증 완료
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                //로그인 안됨 (이메일 인증 안된 사람 포함)
                setContent {
                    MaterialTheme {
                        AuthScreen()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }
}

@Composable
fun AuthScreen(viewModel: FirebaseAuthViewModel = viewModel()) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null)}
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Firebase Auth",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 이메일 입력
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                //실시간 유효성 검사
                emailError = when {
                    it.isBlank() -> null
                    !it.contains("@") -> "이메일에 @가 필요합니다."
                    else -> null
                }
            },
            label = { Text("Email") },
            isError = emailError != null, //에러 상태
            supportingText = { //에러 메세지 표시
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                //실시간 유효성 검사
                passwordError = when {
                        it.isBlank() -> null
                        it.length < 8 -> "비밀번호는 8자 이상이어야 합니다."
                        else -> null
                    }
                },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

//        Column(modifier = Modifier.fillMaxWidth()) {
//            val hasUpperCase = password.any { it.isUpperCase() }
//            val hasLowerCase = password.any { it.isLowerCase() }
//            val hasDigit = password.any { it.isDigit() }
//            val hasSpecialChar = password.any { !it.isLetterOrDigit() }
//            val hasMinLength = password.length >= 6
//
//            Text(
//                text = "비밀번호 조건:",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//
//            PasswordRequirement("최소 6자", hasMinLength)
//            PasswordRequirement("대문자 포함", hasUpperCase)
//            PasswordRequirement("소문자 포함", hasLowerCase)
//            PasswordRequirement("숫자 포함", hasDigit)
//            PasswordRequirement("특수문자 포함", hasSpecialChar)
//        }

        Spacer(modifier = Modifier.height(16.dp))

        // 회원가입 버튼
        Button(
            onClick = { viewModel.signUp(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            Text(text = "회원가입")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 로그인 버튼
        Button(
            onClick = { viewModel.signIn(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            Text(text = "로그인")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Google 로그인 버튼
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    viewModel.signInWithGoogle(context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            Text("🔵 Google로 로그인")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {showPasswordResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("비밀번호를 잊으셨나요?")
        }

//        // 로그아웃 버튼
//        OutlinedButton(
//            onClick = { viewModel.signOut() },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(text = "로그아웃")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))

        // 상태 표시
        when (authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthState.Success -> {
                Text(
                    text = (authState as AuthState.Success).message,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is AuthState.Error -> {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 현재 사용자
        val currentUser = viewModel.getCurrentUser()
        if (currentUser != null) {
            Text("로그인 중: ${currentUser.email}")
            Text("UID: ${currentUser.uid}")
        } else {
            Text("로그인 안 됨")
        }
    }

    //비밀번호 재설정 다이얼로그 추가
    if(showPasswordResetDialog) {
        PasswordResetDialog(
            onDismiss = { showPasswordResetDialog = false },
            onConfirm = { resetEmail ->
                viewModel.sendPasswordResetEmail(resetEmail)
                showPasswordResetDialog = false
            }
        )
    }
}

@Composable
fun PasswordResetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("")}

    AlertDialog (
        onDismissRequest = onDismiss,
        title = { Text("비밀번호 재설정") },
        text = {
           Column {
               Text("가입하신 이메일 주소를 입력하세요.")
               Text("비밀번호 재설정 링크를 보내드립니다.")

               Spacer(modifier = Modifier.height(16.dp))

               OutlinedTextField(
                   value = email,
                   onValueChange = { email = it },
                   label = { Text("Email") },
                   singleLine = true,
                   modifier = Modifier.fillMaxWidth()
               )
           }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank()
            ) {
                Text("전송")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}


@Composable
fun PasswordRequirement(text: String, satisfied: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (satisfied) "✓" else "✗",
            color = if (satisfied)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (satisfied)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}