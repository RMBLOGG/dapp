package com.example.ui.screens.profile

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import com.example.data.supabase.SupabaseConfig
import com.example.ui.components.DappAvatar
import com.example.ui.components.DappButton
import com.example.ui.components.DappTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: DappRepository,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var statusMessage by remember { mutableStateOf(currentUser?.statusMessage ?: "") }
    var selectedAvatarUrl by remember { mutableStateOf(currentUser?.avatarUrl ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var showSupabaseConfigDialog by remember { mutableStateOf(false) }
    var saveSuccessNotice by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = GraphiteVoid,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraphiteVoid)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_profile")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = FogWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profil Saya",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        color = FogWhite
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar with Pulse Ring
            DappAvatar(
                avatarUrl = selectedAvatarUrl,
                displayName = displayName,
                size = 90.dp,
                isOnline = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "@${currentUser?.username ?: "alex_dev"}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = PulseTeal
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Selector
            Text(
                text = "Ganti Foto Profil (Bucket: avatars):",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    color = SlateText
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetAvatars) { url ->
                    val isSelected = url == selectedAvatarUrl
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                            contentDescription = "Pilihan avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit Fields
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
                testTag = "profile_edit_display_name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Status Pesan",
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
                testTag = "profile_edit_status_message"
            )

            if (saveSuccessNotice) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PulseTeal.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PulseTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = PulseTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profil berhasil diperbarui ke Supabase!",
                            style = MaterialTheme.typography.bodySmall.copy(color = PulseTeal)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            DappButton(
                text = "Simpan Perubahan",
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        repository.updateProfile(displayName, statusMessage, selectedAvatarUrl)
                        isSaving = false
                        saveSuccessNotice = true
                    }
                },
                icon = Icons.Outlined.Save,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_save_profile"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Supabase Config & Diagnostic Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PanelColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSupabaseConfigDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = PulseTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Konfigurasi Backend Supabase",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                color = FogWhite
                            )
                        )
                        Text(
                            text = "Status: Terhubung (Realtime & Storage Aktif)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PulseTeal
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SlateText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            DappButton(
                text = "Keluar dari Akun",
                onClick = {
                    coroutineScope.launch {
                        repository.signOut()
                        onLogout()
                    }
                },
                icon = Icons.AutoMirrored.Outlined.Logout,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_logout"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Supabase Settings Dialog
        if (showSupabaseConfigDialog) {
            AlertDialog(
                onDismissRequest = { showSupabaseConfigDialog = false },
                containerColor = PanelColor,
                titleContentColor = FogWhite,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Pengaturan Supabase Dapp",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "URL Proyek:",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateText)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = SupabaseConfig.SUPABASE_URL,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = FogWhite
                                ),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text(
                            text = "Tabel & Bucket Terhubung:",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateText)
                        )
                        Text(
                            text = "• profiles, friendships, chats, chat_participants\n• messages, message_reactions, pinned_messages\n• stories, story_views, blocked_users\n• Storage buckets: avatars, chat-media",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PulseTeal
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSupabaseConfigDialog = false }) {
                        Text("Tutup", color = SignalBlue)
                    }
                }
            )
        }
    }
}
