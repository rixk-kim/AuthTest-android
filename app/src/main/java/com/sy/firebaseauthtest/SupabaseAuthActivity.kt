package com.sy.firebaseauthtest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sy.firebaseauthtest.viewmodel.SupabaseAuthState
import com.sy.firebaseauthtest.viewmodel.SupabaseAuthViewModel
import com.sy.firebaseauthtest.viewmodel.SupabaseClient
import io.github.jan.supabase.auth.auth

class SupabaseAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        if (currentUser != null) {
            goToHome()
            return
        }

        setContent {
            MaterialTheme {
                SupabaseAuthScreen(onLoginSuccess = { goToHome() })
            }
        }
    }

    private fun goToHome() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                putExtra("auth_type", "supabase")
            }
        )
        finish()
    }
}

@Composable
fun SupabaseAuthScreen(
    viewModel: SupabaseAuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    val supabaseAuthState by viewModel.supabaseAuthState.collectAsState()

    LaunchedEffect(supabaseAuthState) {
        if (supabaseAuthState is SupabaseAuthState.NavigateToHome) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Supabase Auth", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.signUp(email, password) },
                modifier = Modifier.weight(1f),
                enabled = supabaseAuthState !is SupabaseAuthState.Loading
            ) { Text("회원가입") }

            Button(
                onClick = { viewModel.signIn(email, password) },
                modifier = Modifier.weight(1f),
                enabled = supabaseAuthState !is SupabaseAuthState.Loading
            ) { Text("로그인") }
        }

        // Firebase랑 동일하게 텍스트 버튼으로
        TextButton(
            onClick = { showPasswordResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("비밀번호를 잊으셨나요?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (supabaseAuthState) {
            is SupabaseAuthState.Loading -> CircularProgressIndicator()
            is SupabaseAuthState.Success -> Text(
                text = (supabaseAuthState as SupabaseAuthState.Success).message,
                color = MaterialTheme.colorScheme.primary
            )
            is SupabaseAuthState.Error -> Text(
                text = (supabaseAuthState as SupabaseAuthState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            else -> {}
        }
    }

    // Firebase의 PasswordResetDialog랑 동일한 구조
    if (showPasswordResetDialog) {
        SupabasePasswordResetDialog(
            onDismiss = { showPasswordResetDialog = false },
            onConfirm = { resetEmail ->
                viewModel.sendPasswordResetEmail(resetEmail)
                showPasswordResetDialog = false
            }
        )
    }
}