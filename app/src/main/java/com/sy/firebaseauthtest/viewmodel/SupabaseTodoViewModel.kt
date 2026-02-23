package com.sy.firebaseauthtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sy.firebaseauthtest.data.SupabaseTodo
import com.sy.firebaseauthtest.viewmodel.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SupabaseTodoViewModel : ViewModel() {

    private val supabase = SupabaseClient.client

    private val _todos = MutableStateFlow<List<SupabaseTodo>>(emptyList())
    val todos: StateFlow<List<SupabaseTodo>> = _todos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchTodos()
    }

    fun fetchTodos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = supabase.postgrest
                    .from("todos")
                    .select() {
                        filter {
                            eq("user_id", supabase.auth.currentUserOrNull()?.id ?: "")
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<SupabaseTodo>()
                _todos.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTodo(title: String) {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                supabase.postgrest
                    .from("todos")
                    .insert(
                        SupabaseTodo(
                            userId = userId,
                            title = title,
                            done = false
                        )
                    )
                fetchTodos()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun toggleTodo(todo: SupabaseTodo) {
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("todos")
                    .update(todo.copy(done = !todo.done)) {
                        filter {
                            eq("id", todo.id)
                        }
                    }
                fetchTodos()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteTodo(todo: SupabaseTodo) {
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("todos")
                    .delete {
                        filter {
                            eq("id", todo.id)
                        }
                    }
                fetchTodos()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}