package com.example.feelingsapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.feelingsapp.BuildConfig
import com.example.feelingsapp.R
import com.example.feelingsapp.data.FirebaseConfig
import com.example.feelingsapp.data.SecurityUtils
import com.example.feelingsapp.data.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLogin: (User) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    isFirebaseConfigured: Boolean = false,
    allowDemoMode: Boolean = BuildConfig.DEBUG,
    onGoogleIdToken: suspend (String) -> Result<User> = {
        Result.failure(IllegalStateException("Firebase is not configured"))
    },
    onEmailSignIn: suspend (String, String) -> Result<User> = { _, _ ->
        Result.failure(IllegalStateException("Firebase is not configured"))
    },
    onEmailSignUp: suspend (String, String, String) -> Result<User> = { _, _, _ ->
        Result.failure(IllegalStateException("Firebase is not configured"))
    },
    onSendPasswordReset: suspend (String) -> Result<Unit> = {
        Result.failure(IllegalStateException("Firebase is not configured"))
    }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showChooser by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authNotice by remember { mutableStateOf<String?>(null) }

    var isSignUpMode by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val webClientId = remember(isFirebaseConfigured) {
        if (isFirebaseConfigured) FirebaseConfig.getWebClientId(context) else null
    }
    val useRealFirebaseGoogle = isFirebaseConfigured && webClientId != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Image(
                painter = painterResource(
                    id = if (isDarkTheme) R.drawable.logo_moody_dark else R.drawable.logo_moody_light
                ),
                contentDescription = "moody",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome to your space of calm. Take a moment to check in with yourself.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mode toggle (Existing Account / Create Account)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(4.dp)
            ) {
                FilterChip(
                    selected = !isSignUpMode,
                    enabled = !isSigningIn,
                    onClick = {
                        isSignUpMode = false
                        authError = null
                        authNotice = null
                    },
                    label = {
                        Text(
                            "Log In",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isSignUpMode,
                    enabled = !isSigningIn,
                    onClick = {
                        isSignUpMode = true
                        authError = null
                        authNotice = null
                    },
                    label = {
                        Text(
                            "Create Account",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isSignUpMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    enabled = !isSigningIn,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                singleLine = true,
                enabled = !isSigningIn,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        if (isSignUpMode) {
                            "Password (${SecurityUtils.MIN_SIGNUP_PASSWORD_LENGTH}+ characters)"
                        } else {
                            "Password"
                        }
                    )
                },
                singleLine = true,
                enabled = !isSigningIn,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Hide" else "Show", fontSize = 12.sp)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isSignUpMode && isFirebaseConfigured) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(
                        enabled = !isSigningIn,
                        onClick = {
                            authError = null
                            authNotice = null
                            val trimmed = email.trim()
                            if (trimmed.isEmpty() || !trimmed.contains("@")) {
                                authError = "Enter your email address first to reset your password."
                                return@TextButton
                            }
                            isSigningIn = true
                            coroutineScope.launch {
                                val res = onSendPasswordReset(trimmed)
                                isSigningIn = false
                                if (res.isSuccess) {
                                    authNotice = "If an account exists for $trimmed, a password reset email has been sent."
                                } else {
                                    authError = res.exceptionOrNull()?.localizedMessage
                                        ?: "Could not send password reset email."
                                }
                            }
                        }
                    ) {
                        Text("Forgot password?", fontSize = 12.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Button(
                enabled = !isSigningIn,
                onClick = {
                    authError = null
                    authNotice = null
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                        authError = "Please enter a valid email address."
                        return@Button
                    }
                    if (isSignUpMode) {
                        val passwordError = SecurityUtils.validateSignUpPassword(password)
                        if (passwordError != null) {
                            authError = passwordError
                            return@Button
                        }
                    } else if (password.length < SecurityUtils.MIN_SIGNIN_PASSWORD_LENGTH) {
                        authError =
                            "Password must be at least ${SecurityUtils.MIN_SIGNIN_PASSWORD_LENGTH} characters."
                        return@Button
                    }

                    if (!isFirebaseConfigured) {
                        if (!allowDemoMode) {
                            authError =
                                "Cloud authentication is unavailable right now. Please reinstall or try again later."
                            return@Button
                        }
                        val fallbackName = displayName.trim().ifEmpty {
                            trimmedEmail.substringBefore("@").ifEmpty { "moody User" }
                        }
                        onLogin(
                            User(
                                name = fallbackName,
                                email = trimmedEmail,
                                avatar = "",
                                uid = "demo-${trimmedEmail.lowercase()}",
                                isDemo = true,
                                authProvider = "demo"
                            )
                        )
                        return@Button
                    }

                    isSigningIn = true
                    coroutineScope.launch {
                        val result = if (isSignUpMode) {
                            onEmailSignUp(trimmedEmail, password, displayName.trim())
                        } else {
                            onEmailSignIn(trimmedEmail, password)
                        }
                        isSigningIn = false
                        if (result.isFailure) {
                            authError = result.exceptionOrNull()?.localizedMessage
                                ?: "Authentication failed. Please try again."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (isSigningIn) {
                        "Please wait..."
                    } else if (isSignUpMode) {
                        "Create Account"
                    } else {
                        "Sign In"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "or",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                enabled = !isSigningIn,
                onClick = {
                    authError = null
                    authNotice = null
                    if (useRealFirebaseGoogle) {
                        isSigningIn = true
                        coroutineScope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setServerClientId(webClientId)
                                    .setFilterByAuthorizedAccounts(false)
                                    .setNonce(SecurityUtils.generateHashedNonce())
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val response = credentialManager.getCredential(
                                    context = context,
                                    request = request
                                )
                                val credential = response.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.data)
                                    val authResult = onGoogleIdToken(googleIdTokenCredential.idToken)
                                    if (authResult.isFailure) {
                                        authError = authResult.exceptionOrNull()?.localizedMessage
                                            ?: "Firebase sign-in failed."
                                    }
                                } else {
                                    authError = "Unsupported credential returned."
                                }
                            } catch (_: GetCredentialCancellationException) {
                                // User dismissed the account picker
                            } catch (_: NoCredentialException) {
                                authError = "No Google account found on this device."
                            } catch (_: GetCredentialException) {
                                authError = "Google sign-in failed. Please try again."
                            } catch (_: Exception) {
                                authError = "Unable to sign in right now. Please try again."
                            } finally {
                                isSigningIn = false
                            }
                        }
                    } else if (allowDemoMode) {
                        showChooser = true
                    } else {
                        authError =
                            "Google sign-in is unavailable right now. Please reinstall or try again later."
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (authNotice != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = authNotice ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = authError ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            if (!isFirebaseConfigured && allowDemoMode) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Demo Mode — add android/app/google-services.json to enable cloud auth & Firestore sync",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showChooser && allowDemoMode) {
        Dialog(onDismissRequest = { showChooser = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose an account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "to continue to moody",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    AccountItem(
                        name = "Demo User",
                        email = "demo@moody.local",
                        onClick = {
                            showChooser = false
                            onLogin(User("Demo User", "demo@moody.local", "", uid = "demo-user", isDemo = true))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AccountItem(
                        name = "Developer User",
                        email = "developer@feelings.app",
                        onClick = {
                            showChooser = false
                            onLogin(User("Developer User", "developer@feelings.app", "", uid = "demo-dev", isDemo = true))
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            showChooser = false
                            onLogin(User("Guest User", "guest@feelings.app", "", uid = "demo-guest", isDemo = true))
                        }
                    ) {
                        Text("Use guest account", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Debug demo accounts store logs only on this device.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    name: String,
    email: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Text(
                    text = name.take(1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
