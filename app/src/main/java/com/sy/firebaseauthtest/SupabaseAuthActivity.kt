package com.sy.firebaseauthtest

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

class SupabaseAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SupabaseAuthScreen()
            }
        }
    }
}

@Composable
fun SupabaseAuthScreen(viewModel: SupabaseAuthViewModel = viewModel()) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val supabaseAuthState by viewModel.supabaseAuthState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Supabase Auth",
            style = MaterialTheme.typography.headlineMedium
        )

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

        Button(
            onClick = { viewModel.signUp(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = supabaseAuthState !is SupabaseAuthState.Loading
        ) {
            Text(text = "회원가입")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.signIn(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = supabaseAuthState !is SupabaseAuthState.Loading
        ) {
            Text(text = "로그인")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "로그아웃")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (supabaseAuthState) {
            is SupabaseAuthState.Loading -> {
                CircularProgressIndicator()
            }
            is SupabaseAuthState.Success -> {
                Text(
                    text = (supabaseAuthState as SupabaseAuthState.Success).message,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is SupabaseAuthState.Error -> {
                Text(
                    text = (supabaseAuthState as SupabaseAuthState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentUser = viewModel.getCurrentUser()
        if (currentUser != null) {
            Text("로그인 중: ${currentUser.email}")
            Text("UID: ${currentUser.id}")
        } else {
            Text("로그인 안 됨")
        }
    }
}