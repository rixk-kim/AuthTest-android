package com.sy.firebaseauthtest.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sy.firebaseauthtest.data.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber


class TodoViewModel: ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos : StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadTodos()
    }

    //Todo 목록 조회 (실시간)
    private fun loadTodos() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("todos")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Timber.e("Error loading todos $exception")
                    _errorMessage.value = exception.message
                    return@addSnapshotListener
                }

                val todoList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Todo::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _todos.value = todoList
                Timber.d("Loaded ${todoList.size} todos")
            }
    }

    //todo 추가
    fun addTodo(title: String) {
        if (title.isBlank()){
            _errorMessage.value = "할 일을 입력하세요."
            return
        }

        val userId = auth.currentUser?.uid ?: return

        val todo = hashMapOf(
            "title" to title,
            "done" to false,
            "userId" to userId,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        _isLoading.value = true

        firestore.collection("todos")
            .add(todo)
            .addOnSuccessListener {
                Timber.d("Todo added: $title")
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                Timber.e("Error adding todo $title $e")
                _errorMessage.value = "추가 실패: ${e.message}"
                _isLoading.value = false
            }
    }

    //todo 완료 토글
    fun toggleTodo(todo: Todo) {
        firestore.collection("todos")
            .document(todo.id)
            .update("done", !todo.done)
            .addOnSuccessListener {
                Timber.d("Todo toggled: ${todo.id}")
            }
            .addOnFailureListener { e ->
                Timber.e("Error toggling todo $e")
                _errorMessage.value = "업데이트 실패:${e.message}"
            }
    }

    //todo 삭제
    fun deleteTodo(todo: Todo) {
        firestore.collection("todos")
            .document(todo.id)
            .delete()
            .addOnSuccessListener {
                Timber.d("Todo deleted: ${todo.id}")
            }
            .addOnFailureListener { e ->
                Timber.e("Error deleting todo $e")
                _errorMessage.value = "삭제 실패:${e.message}"
            }
    }

    //에러 메시지 초기화
    fun clearError() {
        _errorMessage.value = null
    }
}

