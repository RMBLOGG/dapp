package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DappRepository
import com.example.ui.components.DappButton
import com.example.ui.components.DappTextField
import com.example.ui.components.DappWordmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: DappRepository,
    onAuthSuccess: (isNewUser: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("alex@dapp.network") }
    var password by remember { mutableStateOf("dapp1234") }
    var username by remember { mutableStateOf("alex_dev") }
    var displayName by remember { mutableStateOf("Alex Morgan") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteVoid)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Brand Logo & Wordmark
            DappWordmark(fontSize = 44)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRegisterMode) "Buat akun baru untuk mulai chat" else "Masuk ke jaringan terenkripsi Dapp",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    color = SlateText
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ErrorRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed)
                        )
                    }
                }
            }

            // Form Fields
            if (isRegisterMode) {
                DappTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Username (contoh: alex_dev)",
                    leadingIcon = Icons.Outlined.AlternateEmail,
                    testTag = "input_username"
                )
                Spacer(modifier = Modifier.height(14.dp))

                DappTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    placeholder = "Nama Tampilan (contoh: Alex Morgan)",
                    leadingIcon = Icons.Outlined.PersonOutline,
                    testTag = "input_display_name"
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            DappTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Alamat Email",
                leadingIcon = Icons.Outlined.MailOutline,
                testTag = "input_email"
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text("Password (minimal 6 karakter)", color = SlateText)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SlateText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = SlateText
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PanelColor,
                    unfocusedContainerColor = PanelColor,
                    focusedBorderColor = SignalBlue,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = FogWhite,
                    unfocusedTextColor = FogWhite,
                    cursorColor = PulseTeal
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    color = FogWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            DappButton(
                text = if (isRegisterMode) "Daftar Akun Dapp" else "Masuk ke Dapp",
                onClick = {
                    errorMessage = null
                    isLoading = true
                    coroutineScope.launch {
                        if (isRegisterMode) {
                            val res = repository.signUp(email, password, username, displayName)
                            isLoading = false
                            if (res.isSuccess) {
                                onAuthSuccess(true)
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Gagal mendaftar"
                            }
                        } else {
                            val res = repository.signIn(email, password)
                            isLoading = false
                            if (res.isSuccess) {
                                onAuthSuccess(false)
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Gagal masuk"
                            }
                        }
                    }
                },
                icon = if (isRegisterMode) Icons.Outlined.PersonAdd else Icons.Outlined.Login,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                testTag = "submit_auth_button"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Switch Mode Link
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegisterMode) "Sudah punya akun? " else "Belum punya akun? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SlateText)
                )
                Text(
                    text = if (isRegisterMode) "Masuk" else "Daftar Baru",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SignalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Quick Demo Switcher Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PanelColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.VpnKey,
                            contentDescription = null,
                            tint = PulseTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Akses Instan Demo (Supabase Offline & Realtime)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PulseTeal
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Masuk langsung dengan profil Alex Morgan terverifikasi",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateText)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
