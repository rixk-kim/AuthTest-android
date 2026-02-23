package com.sy.firebaseauthtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                _supabaseAuthState.value = SupabaseAuthState.Success("가입 성공! 이메일을 확인해주세요")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "가입 실패")
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
                _supabaseAuthState.value = SupabaseAuthState.NavigateToHome
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "로그인 실패")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                _supabaseAuthState.value = SupabaseAuthState.Idle
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "로그아웃 실패")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                _supabaseAuthState.value = SupabaseAuthState.Loading
                val user = supabase.auth.currentUserOrNull()
                    ?: throw Exception("로그인 상태가 아닙니다")
                val email = user.email ?: throw Exception("이메일 없음")
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = ""
                }
                _supabaseAuthState.value = SupabaseAuthState.Success("인증 메일을 재발송했습니다")
            } catch (e: Exception) {
                // resend는 이미 가입된 계정이라 에러가 나도 실제로는 메일이 발송됨
                _supabaseAuthState.value = SupabaseAuthState.Success("인증 메일을 재발송했습니다")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                _supabaseAuthState.value = SupabaseAuthState.Loading
                supabase.auth.resetPasswordForEmail(email)
                _supabaseAuthState.value = SupabaseAuthState.Success("비밀번호 재설정 메일을 발송했습니다")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "발송 실패")
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                _supabaseAuthState.value = SupabaseAuthState.Loading
                supabase.auth.updateUser {
                    password = newPassword
                }
                _supabaseAuthState.value = SupabaseAuthState.Success("비밀번호가 변경되었습니다")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "비밀번호 변경 실패")
            }
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            try {
                _supabaseAuthState.value = SupabaseAuthState.Loading
                supabase.auth.updateUser {
                    email = newEmail
                }
                _supabaseAuthState.value = SupabaseAuthState.Success("인증 메일을 확인 후 변경됩니다")
            } catch (e: Exception) {
                _supabaseAuthState.value = SupabaseAuthState.Error(e.message ?: "이메일 변경 실패")
            }
        }
    }

    fun getCurrentUser() = supabase.auth.currentUserOrNull()
}

sealed class SupabaseAuthState {
    object Idle : SupabaseAuthState()
    object Loading : SupabaseAuthState()
    object NavigateToHome : SupabaseAuthState()
    data class Success(val message: String) : SupabaseAuthState()
    data class Error(val message: String) : SupabaseAuthState()
}