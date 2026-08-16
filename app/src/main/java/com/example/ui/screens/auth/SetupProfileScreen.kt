package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.DappRepository
import com.example.ui.components.DappAvatar
import com.example.ui.components.DappButton
import com.example.ui.components.DappTextField
import com.example.ui.components.DappWordmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SetupProfileScreen(
    repository: DappRepository,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "Alex Morgan") }
    var username by remember { mutableStateOf(currentUser?.username ?: "alex_dev") }
    var statusMessage by remember { mutableStateOf(currentUser?.statusMessage ?: "Building next-gen decentralized apps ⚡") }
    var selectedAvatarUrl by remember { mutableStateOf(currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&q=80") }
    var isLoading by remember { mutableStateOf(false) }

    val presetAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&q=80",
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&q=80",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&q=80",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&q=80"
    )

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            DappWordmark(fontSize = 32)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Setup Profil Dapp Anda",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = FogWhite
                )
            )

            Text(
                text = "Foto profil akan diunggah ke bucket Supabase 'avatars'",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    color = SlateText
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Avatar with Pulse Ring
            DappAvatar(
                avatarUrl = selectedAvatarUrl,
                displayName = displayName,
                size = 96.dp,
                isOnline = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Choose avatar preset / upload simulation
            Text(
                text = "Pilih Avatar / Foto:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = PulseTeal
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetAvatars) { url ->
                    val isSelected = url == selectedAvatarUrl
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PulseTeal else BorderDark,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatarUrl = url }
                            .padding(2.dp)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Avatar option",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Inputs
            Text(
                text = "Nama Tampilan",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    color = FogWhite
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            DappTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = "Nama Tampilan",
                leadingIcon = Icons.Outlined.Badge,
                testTag = "setup_display_name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Username",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    color = FogWhite
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            DappTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                leadingIcon = Icons.Outlined.AlternateEmail,
                testTag = "setup_username"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Status / Bio",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    color = FogWhite
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            DappTextField(
                value = statusMessage,
                onValueChange = { statusMessage = it },
                placeholder = "Status pesan Anda",
                leadingIcon = Icons.Outlined.EditNote,
                testTag = "setup_status_message"
            )

            Spacer(modifier = Modifier.height(36.dp))

            DappButton(
                text = "Simpan & Masuk ke Chat",
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        repository.updateProfile(displayName, statusMessage, selectedAvatarUrl)
                        isLoading = false
                        onSetupComplete()
                    }
                },
                icon = Icons.Outlined.CheckCircleOutline,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_profile_button"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
