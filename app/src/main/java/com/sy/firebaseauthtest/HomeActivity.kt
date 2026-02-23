package com.sy.firebaseauthtest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.sy.firebaseauthtest.data.SupabaseTodo
import com.sy.firebaseauthtest.data.Todo
import com.sy.firebaseauthtest.viewmodel.AuthState
import com.sy.firebaseauthtest.viewmodel.FirebaseAuthViewModel
import com.sy.firebaseauthtest.viewmodel.SupabaseAuthState
import com.sy.firebaseauthtest.viewmodel.SupabaseAuthViewModel
import com.sy.firebaseauthtest.viewmodel.SupabaseTodoViewModel
import com.sy.firebaseauthtest.viewmodel.TodoViewModel

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 어떤 Auth로 로그인했는지 구분
        val authType = intent.getStringExtra("auth_type") ?: "firebase"
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                HomeScreen(
                    authType = authType,
                    onLogout = {
                        if (authType == "firebase") {
                            auth.signOut()
                            startActivity(Intent(this, FirebaseAuthActivity::class.java))
                        } else {
                            startActivity(Intent(this, SupabaseAuthActivity::class.java))
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authType: String = "firebase",
    onLogout: () -> Unit
) {
    val auth = Firebase.auth
    val user = auth.currentUser
    var isEmailVerified by remember { mutableStateOf(user?.isEmailVerified ?: false) }

    val firebaseViewModel: FirebaseAuthViewModel = viewModel()
    val firebaseAuthState by firebaseViewModel.authState.collectAsState()

    val supabaseViewModel: SupabaseAuthViewModel = viewModel()

    val supabaseAuthState by supabaseViewModel.supabaseAuthState.collectAsState()
    var isFirstLaunch by remember { mutableStateOf(true) }

    //supabase todo viewmodel
    // ViewModel 추가
    val supabaseTodoViewModel: SupabaseTodoViewModel = viewModel()
    val supabaseTodos by supabaseTodoViewModel.todos.collectAsState()
    val supabaseTodoLoading by supabaseTodoViewModel.isLoading.collectAsState()
    val supabaseTodoError by supabaseTodoViewModel.errorMessage.collectAsState()


    var showEmailChangeDialog by remember { mutableStateOf(false) }
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var newTodoTitle by remember { mutableStateOf("") }

    val todoViewModel: TodoViewModel = viewModel()
    val todos by todoViewModel.todos.collectAsState()
    val isLoading by todoViewModel.isLoading.collectAsState()
    val errorMessage by todoViewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("홈", "Todo")

    LaunchedEffect(supabaseAuthState) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }
        if (authType == "supabase" && supabaseAuthState is SupabaseAuthState.Idle) {
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    // ✅ authType에 따라 타이틀 변경
                    title = {
                        Text(if (authType == "firebase") "Firebase Auth Test" else "Supabase Auth Test")
                    },
                    actions = {
                        TextButton(onClick = {
                            // ✅ Supabase는 로그아웃 먼저 처리
                            if (authType == "supabase") {
                                supabaseViewModel.signOut()
                            }
                            onLogout()
                        }) {
                            Text("로그아웃")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showAddTodoDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Todo")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> {
                    // ✅ authType에 따라 홈탭 분기
                    if (authType == "firebase") {
                        HomeTab(
                            user = user,
                            isEmailVerified = isEmailVerified,
                            authViewModel = firebaseViewModel,
                            authState = firebaseAuthState,
                            onReloadUser = { verified -> isEmailVerified = verified },
                            onShowEmailChange = { showEmailChangeDialog = true }
                        )
                    } else {
                        SupabaseHomeTab(viewModel = supabaseViewModel)
                    }
                }
                1 -> {
                    if (authType == "firebase") {
                        TodoTab(
                            todos = todos,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onToggleTodo = { todoViewModel.toggleTodo(it) },
                            onDeleteTodo = { todoViewModel.deleteTodo(it) },
                            onClearError = { todoViewModel.clearError() }
                        )
                    } else {
                        SupabaseTodoTab(
                            todos = supabaseTodos,
                            isLoading = supabaseTodoLoading,
                            errorMessage = supabaseTodoError,
                            onToggleTodo = { supabaseTodoViewModel.toggleTodo(it) },
                            onDeleteTodo = { supabaseTodoViewModel.deleteTodo(it) },
                            onClearError = { supabaseTodoViewModel.clearError() }
                        )
                    }
                }
            }
        }
    }

    // Firebase 이메일 변경 다이얼로그
    if (showEmailChangeDialog && authType == "firebase") {
        EmailChangeDialog(
            currentEmail = user?.email ?: "",
            onDismiss = { showEmailChangeDialog = false },
            onConfirm = { newEmail, password ->
                firebaseViewModel.updateEmail(newEmail, password)
                showEmailChangeDialog = false
            }
        )
    }

    // Todo 추가 다이얼로그
    if (showAddTodoDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTodoDialog = false
                newTodoTitle = ""
            },
            title = { Text("할 일 추가") },
            text = {
                OutlinedTextField(
                    value = newTodoTitle,
                    onValueChange = { newTodoTitle = it },
                    label = { Text("할 일") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (authType == "firebase") {
                            todoViewModel.addTodo(newTodoTitle)
                        } else {
                            supabaseTodoViewModel.addTodo(newTodoTitle)
                        }
                        showAddTodoDialog = false
                        newTodoTitle = ""
                    },
                    enabled = newTodoTitle.isNotBlank()
                ) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTodoDialog = false
                    newTodoTitle = ""
                }) { Text("취소") }
            }
        )
    }
}

// ── 기존 Firebase 홈탭 (변경 없음) ────────────────────────────

@Composable
fun HomeTab(
    user: com.google.firebase.auth.FirebaseUser?,
    isEmailVerified: Boolean,
    authViewModel: FirebaseAuthViewModel,
    authState: AuthState,
    onReloadUser: (Boolean) -> Unit,
    onShowEmailChange: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "환영합니다!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { authViewModel.reloadUser { verified -> onReloadUser(verified) } }
                        ) { Text("인증 확인") }
                        Button(
                            onClick = { authViewModel.resendEmailVerification() }
                        ) { Text("이메일 재발송") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (authState) {
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Success -> Text(
                text = (authState as AuthState.Success).message,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            is AuthState.Error -> Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (user != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        onClick = onShowEmailChange,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("이메일 주소 변경") }
                }
            }
        }
    }
}

// ── Supabase 홈탭 (신규 추가) ─────────────────────────────────

@Composable
fun SupabaseHomeTab(viewModel: SupabaseAuthViewModel) {
    val currentUser = viewModel.getCurrentUser()
    val authState by viewModel.supabaseAuthState.collectAsState()
    var showEmailChangeDialog by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
//    var showPasswordResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("환영합니다!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        when (authState) {
            is SupabaseAuthState.Loading -> CircularProgressIndicator()
            is SupabaseAuthState.Success -> Text(
                (authState as SupabaseAuthState.Success).message,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            is SupabaseAuthState.Error -> Text(
                (authState as SupabaseAuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentUser != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "로그인 정보",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("이메일: ${currentUser.email}")
                    Text("UID: ${currentUser.id}")
                    Text("이메일 인증: ${if (currentUser.emailConfirmedAt != null) "✓ 완료" else "✗ 미완료"}")

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentUser.emailConfirmedAt == null) {
                        OutlinedButton(
                            onClick = { viewModel.resendVerificationEmail() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("인증 메일 재발송") }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { showEmailChangeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("이메일 변경") }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showPasswordChangeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("비밀번호 변경") }

                    Spacer(modifier = Modifier.height(8.dp))

//                    OutlinedButton(
//                        onClick = { showPasswordResetDialog = true },
//                        modifier = Modifier.fillMaxWidth()
//                    ) { Text("비밀번호 재설정 메일 발송") }
                }
            }
        }
    }

    if (showEmailChangeDialog) {
        SupabaseEmailChangeDialog(
            currentEmail = currentUser?.email ?: "",
            onDismiss = { showEmailChangeDialog = false },
            onConfirm = { newEmail ->
                viewModel.updateEmail(newEmail)
                showEmailChangeDialog = false
            }
        )
    }

    if (showPasswordChangeDialog) {
        SupabasePasswordChangeDialog(
            onDismiss = { showPasswordChangeDialog = false },
            onConfirm = { newPassword ->
                viewModel.updatePassword(newPassword)
                showPasswordChangeDialog = false
            }
        )
    }

//    if (showPasswordResetDialog) {
//        SupabasePasswordResetDialog(
//            onDismiss = { showPasswordResetDialog = false },
//            onConfirm = { email ->
//                viewModel.sendPasswordResetEmail(email)
//                showPasswordResetDialog = false
//            }
//        )
//    }
}

// ── 기존 Todo 탭 / 아이템 / Firebase EmailChangeDialog (변경 없음) ──

@Composable
fun TodoTab(
    todos: List<Todo>,
    isLoading: Boolean,
    errorMessage: String?,
    onToggleTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onClearError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) { Text("닫기") }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        if (todos.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "할 일이 없습니다\n+ 버튼을 눌러 추가하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todos, key = { it.id }) { todo ->
                    TodoItem(
                        todo = todo,
                        onToggle = { onToggleTodo(todo) },
                        onDelete = { onDeleteTodo(todo) }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoItem(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.done, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

//supabase todo tab

@Composable
fun SupabaseTodoTab(
    todos: List<SupabaseTodo>,
    isLoading: Boolean,
    errorMessage: String?,
    onToggleTodo: (SupabaseTodo) -> Unit,
    onDeleteTodo: (SupabaseTodo) -> Unit,
    onClearError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) { Text("닫기") }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        if (todos.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "할 일이 없습니다\n+ 버튼을 눌러 추가하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todos, key = { it.id }) { todo ->
                    SupabaseTodoItem(
                        todo = todo,
                        onToggle = { onToggleTodo(todo) },
                        onDelete = { onDeleteTodo(todo) }
                    )
                }
            }
        }
    }
}

@Composable
fun SupabaseTodoItem(
    todo: SupabaseTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.done, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
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
            ) { Text("변경") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ── Supabase 다이얼로그들 (신규 추가) ─────────────────────────

@Composable
fun SupabaseEmailChangeDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이메일 변경") },
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
                Text(
                    "⚠️ 새 이메일로 인증 메일이 발송됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newEmail) },
                enabled = newEmail.isNotBlank()
            ) { Text("변경") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
fun SupabasePasswordChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("비밀번호 변경") },
        text = {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("새 비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newPassword) },
                enabled = newPassword.isNotBlank()
            ) { Text("변경") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
fun SupabasePasswordResetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("비밀번호 재설정") },
        text = {
            Column {
                Text("가입하신 이메일로 재설정 링크를 보내드립니다.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("이메일") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank()
            ) { Text("전송") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}