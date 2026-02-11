package com.sy.firebaseauthtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.sy.firebaseauthtest.viewmodel.AuthState
import com.sy.firebaseauthtest.viewmodel.FirebaseAuthViewModel

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            MaterialTheme {
                HomeScreen(
                    onLogout = {
                        auth.signOut()
                        // 로그아웃하면 MainActivity로 돌아감
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val auth = Firebase.auth
    val user = auth.currentUser
    var isEmailVerified by remember { mutableStateOf(user?.isEmailVerified ?: false)}
    val viewModel : FirebaseAuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()
    var showEmailChangeDialog by remember { mutableStateOf(false) }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "환영합니다!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 이메일 인증 경고
            // 이메일 인증 하고 로긴 하게 로직을 변경 했기에 아래 코드는 필요 없음
            if (user != null && !isEmailVerified) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ 이메일 인증이 필요합니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${user.email}로 발송된\n인증 이메일을 확인하세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.reloadUser { verified ->
                                        isEmailVerified = verified
                                    }
                                }
                            ) {
                                Text("인증 확인")
                            }

                            Button(
                                onClick = { viewModel.resendEmailVerification() }
                            ) {
                                Text("이메일 재발송")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 상태 메시지
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Success -> {
                    Text(
                        text = (authState as AuthState.Success).message,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                is AuthState.Error -> {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 사용자 정보
            if (user != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "로그인 정보",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("이메일: ${user.email}")
                        Text("UID: ${user.uid}")
                        Text("이메일 인증: ${if (isEmailVerified) "✓ 완료" else "✗ 미완료"}")

                        user.metadata?.let { metadata ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("가입일: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(metadata.creationTimestamp))}")
                            Text("마지막 로그인: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(metadata.lastSignInTimestamp))}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showEmailChangeDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("이메일 주소 변경")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("로그아웃")
            }
        }
    }

    // 이메일 변경 다이얼로그
    if (showEmailChangeDialog) {
        EmailChangeDialog(
            currentEmail = user?.email ?: "",
            onDismiss = { showEmailChangeDialog = false },
            onConfirm = { newEmail, password ->
                viewModel.updateEmail(newEmail, password)
                showEmailChangeDialog = false
            }
        )
    }
}

@Composable
fun EmailChangeDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이메일 주소 변경") },
        text = {
            Column {
                Text("현재 이메일: $currentEmail")

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("새 이메일") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("현재 비밀번호 (확인용)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "⚠️ 새 이메일로 인증 메일이 발송됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newEmail, password) },
                enabled = newEmail.isNotBlank() && password.isNotBlank()
            ) {
                Text("변경")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}