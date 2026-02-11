package com.sy.firebaseauthtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sy.firebaseauthtest.viewmodel.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SupabaseAuthViewModel : ViewModel() {

    private val supabase = SupabaseClient.client

    private val _supabaseAuthState = MutableStateFlow<SupabaseAuthState>(SupabaseAuthState.Idle)
    val supabaseAuthState: StateFlow<SupabaseAuthState> = _supabaseAuthState

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            try {
                _supabaseAuthState.value = SupabaseAuthState.Loading

                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                _supabaseAuthState.value = SupabaseAuthState.Success("슈파베이스 가입 성공")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "슈파베이스 가입 실패")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _supabaseAuthState.value = SupabaseAuthState.Loading

                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                _supabaseAuthState.value = SupabaseAuthState.Success("슈파베이스 로그인 성공")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "슈파베이스 로그인 실패")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                _supabaseAuthState.value = SupabaseAuthState.Success("슈파베이스 로그아웃 성공")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "슈파베이스 로그아웃 실패")
            }
        }
    }

    fun getCurrentUser() = supabase.auth.currentUserOrNull()
}

sealed class SupabaseAuthState {
    object Idle: SupabaseAuthState()
    object Loading: SupabaseAuthState()
    data class Success(val message: String) : SupabaseAuthState()
    data class Error(val message: String) : SupabaseAuthState()
}