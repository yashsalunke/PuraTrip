package com.ysdigi.puratrip.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.R
import com.ysdigi.puratrip.home.HomeScreenActivity
import com.ysdigi.puratrip.models.User
import com.ysdigi.puratrip.ui.theme.PuraTripTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToHome()
        }

        setContent {
            PuraTripTheme {
                LoginNavigation(oneTapClient)
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeScreenActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginNavigation(oneTapClient: SignInClient) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    val intent = Intent(context, HomeScreenActivity::class.java)
                    context.startActivity(intent)
                    (context as? ComponentActivity)?.finish()
                },
                onGoToRegister = { navController.navigate("register") },
                oneTapClient = oneTapClient
            )
        }
        composable("register") {
            RegistrationScreen(
                onRegisterSuccess = {
                    val intent = Intent(context, HomeScreenActivity::class.java)
                    context.startActivity(intent)
                    (context as? ComponentActivity)?.finish()
                },
                onGoToLogin = { navController.navigate("login") }
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    oneTapClient: SignInClient
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }
    val auth = Firebase.auth
    val db = Firebase.firestore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverClientId = stringResource(id = R.string.default_web_client_id)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                scope.launch {
                    try {
                        auth.signInWithCredential(firebaseCredential).await()
                        val user = auth.currentUser
                        if (user != null) {
                            val userDoc = db.collection("users").document(user.uid).get().await()
                            if (!userDoc.exists()) {
                                val userMap = hashMapOf(
                                    "name" to user.displayName,
                                    "email" to user.email
                                )
                                db.collection("users").document(user.uid).set(userMap).await()
                            }
                        }
                        onLoginSuccess()
                    } catch (e: Exception) {
                        Log.w("LoginActivity", "signInWithCredential", e)
                    }
                }
            }
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign in failed", e)
        }
    }

    fun validateEmail() {
        isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword() {
        isPasswordValid = password.isNotBlank()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                validateEmail()
            },
            label = { Text("Email Address") },
            modifier = Modifier.padding(16.dp),
            isError = !isEmailValid,
            supportingText = {
                if (!isEmailValid) {
                    Text("Invalid email format")
                }
            }
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                validatePassword()
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(16.dp),
            isError = !isPasswordValid,
            supportingText = {
                if (!isPasswordValid) {
                    Text("Password cannot be empty")
                }
            }
        )
        Button(
            onClick = {
                validateEmail()
                validatePassword()
                if (isEmailValid && isPasswordValid) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier.padding(16.dp),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text("Login")
        }
        GoogleSignInButton(
            onClick = {
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(serverClientId)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()

                oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        launcher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        Log.e("LoginActivity", "Google Sign-in failed", e)
                    }
            }
        )
        Button(
            onClick = onGoToRegister,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Go to Register")
        }
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google sign-in",
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign in with Google", color = Color.Black)
        }
    }
}

@Composable
fun RegistrationScreen(onRegisterSuccess: () -> Unit, onGoToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isNameValid by remember { mutableStateOf(true) }
    var isEmailValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }
    val auth = Firebase.auth
    val db = Firebase.firestore
    val context = LocalContext.current

    fun validateName() {
        isNameValid = name.isNotBlank()
    }

    fun validateEmail() {
        isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword() {
        isPasswordValid = password.length >= 6
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                validateName()
            },
            label = { Text("Name") },
            modifier = Modifier.padding(16.dp),
            isError = !isNameValid,
            supportingText = {
                if (!isNameValid) {
                    Text("Name cannot be empty")
                }
            }
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                validateEmail()
            },
            label = { Text("Email Address") },
            modifier = Modifier.padding(16.dp),
            isError = !isEmailValid,
            supportingText = {
                if (!isEmailValid) {
                    Text("Invalid email format")
                }
            }
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                validatePassword()
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(16.dp),
            isError = !isPasswordValid,
            supportingText = {
                if (!isPasswordValid) {
                    Text("Password must be at least 6 characters long")
                }
            }
        )
        Button(
            onClick = {
                validateName()
                validateEmail()
                validatePassword()
                if (isNameValid && isEmailValid && isPasswordValid) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { send ->
                                        if (send.isSuccessful) {
                                            Log.d("LoginActivity", "Verification email sent successfully.")
                                        } else {
                                            Log.e("LoginActivity", "Failed to send verification email.", send.exception)
                                        }
                                    }
                                val userMap = User(
                                    name = name,
                                    email = email,
                                    verified = user?.isEmailVerified ?: false
                                )
                                if (user != null) {
                                    db.collection("users").document(user.uid).set(userMap)
                                        .addOnSuccessListener { onRegisterSuccess() }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier.padding(16.dp),
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
        ) {
            Text("Register")
        }
        Button(
            onClick = onGoToLogin,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Go to Login")
        }
    }
}
