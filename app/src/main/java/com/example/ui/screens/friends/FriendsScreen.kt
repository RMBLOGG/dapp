package com.example.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Profile
import com.example.data.repository.DappRepository
import com.example.ui.components.DappAvatar
import com.example.ui.components.DappButton
import com.example.ui.components.DappTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    repository: DappRepository,
    onBack: () -> Unit,
    onStartChat: (chatId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Teman Saya, 1: Permintaan, 2: Pengguna Diblokir

    val currentUser by repository.currentUser.collectAsState()
    val friendships by repository.friendships.collectAsState()
    val blockedUsers by repository.blockedUsers.collectAsState()
    val profiles by repository.profiles.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val myFriends = remember(friendships, profiles, currentUser, blockedUsers) {
        repository.getFriends()
    }

    val pendingRequests = remember(friendships, profiles, currentUser) {
        repository.getPendingRequests()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults = repository.searchUsers(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        containerColor = GraphiteVoid,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraphiteVoid)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_friends")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = FogWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Teman & Kontak",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = FogWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search user by username field
                DappTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Cari user berdasarkan @username atau nama...",
                    leadingIcon = Icons.Outlined.PersonSearch,
                    testTag = "input_search_friends"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GraphiteVoid,
                    contentColor = PulseTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PulseTeal
                        )
                    },
                    divider = { HorizontalDivider(color = BorderDark) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Teman (${myFriends.size})",
                                fontFamily = SpaceGroteskFontFamily,
                                color = if (selectedTab == 0) FogWhite else SlateText,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Permintaan (${pendingRequests.size})",
                                fontFamily = SpaceGroteskFontFamily,
                                color = if (selectedTab == 1) PulseTeal else SlateText,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Diblokir (${blockedUsers.size})",
                                fontFamily = SpaceGroteskFontFamily,
                                color = if (selectedTab == 2) ErrorRed else SlateText,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchQuery.isNotBlank()) {
                // Search Results
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Hasil Pencarian Global (${searchResults.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = PulseTeal
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (searchResults.isEmpty()) {
                        Text(
                            text = "Tidak ditemukan pengguna dengan username tersebut.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = SlateText)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(searchResults, key = { it.id }) { user ->
                                val isAlreadyFriend = myFriends.any { it.id == user.id }
                                val hasRequested = friendships.any { it.userId == currentUser?.id && it.friendId == user.id }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PanelColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DappAvatar(
                                            avatarUrl = user.avatarUrl,
                                            displayName = user.displayName,
                                            size = 46.dp,
                                            isOnline = user.isOnline
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.displayName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = SpaceGroteskFontFamily,
                                                    color = FogWhite
                                                )
                                            )
                                            Text(
                                                text = "@${user.username}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    color = SlateText
                                                )
                                            )
                                        }

                                        when {
                                            isAlreadyFriend -> {
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val chat = repository.createOrGetPrivateChat(user.id)
                                                            onStartChat(chat.id)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Chat,
                                                        contentDescription = "Chat",
                                                        tint = SignalBlue
                                                    )
                                                }
                                            }
                                            hasRequested -> {
                                                Text(
                                                    text = "Terkirim",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        color = PulseTeal
                                                    )
                                                )
                                            }
                                            else -> {
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.sendFriendRequest(user.id)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PersonAddAlt1,
                                                        contentDescription = "Tambah Teman",
                                                        tint = PulseTeal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // My Friends Tab
                        if (myFriends.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Belum ada teman. Cari username teman di atas!",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = SlateText)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(myFriends, key = { it.id }) { friend ->
                                    var showFriendMenu by remember { mutableStateOf(false) }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PanelColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DappAvatar(
                                                avatarUrl = friend.avatarUrl,
                                                displayName = friend.displayName,
                                                size = 48.dp,
                                                isOnline = friend.isOnline
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = friend.displayName,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = SpaceGroteskFontFamily,
                                                        color = FogWhite
                                                    )
                                                )
                                                Text(
                                                    text = friend.statusMessage,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = InterFontFamily,
                                                        color = SlateText
                                                    ),
                                                    maxLines = 1
                                                )
                                            }

                                            // Start Chat
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val chat = repository.createOrGetPrivateChat(friend.id)
                                                        onStartChat(chat.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Chat,
                                                    contentDescription = "Buka Chat",
                                                    tint = SignalBlue
                                                )
                                            }

                                            // More Options (Hapus teman, Blokir)
                                            Box {
                                                IconButton(onClick = { showFriendMenu = true }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.MoreVert,
                                                        contentDescription = "Pilihan",
                                                        tint = SlateText
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = showFriendMenu,
                                                    onDismissRequest = { showFriendMenu = false },
                                                    modifier = Modifier.background(PanelColor)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Hapus Pertemanan", color = FogWhite) },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.PersonRemove,
                                                                contentDescription = null,
                                                                tint = SlateText
                                                            )
                                                        },
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                repository.removeFriendship(friend.id)
                                                                showFriendMenu = false
                                                            }
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Blokir Pengguna", color = ErrorRed) },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Block,
                                                                contentDescription = null,
                                                                tint = ErrorRed
                                                            )
                                                        },
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                repository.blockUser(friend.id)
                                                                showFriendMenu = false
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Pending Requests Tab
                        if (pendingRequests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Tidak ada permintaan pertemanan tertunda",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = SlateText)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pendingRequests, key = { it.id }) { req ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PanelColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DappAvatar(
                                                avatarUrl = req.avatarUrl,
                                                displayName = req.displayName,
                                                size = 46.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = req.displayName,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = SpaceGroteskFontFamily,
                                                        color = FogWhite
                                                    )
                                                )
                                                Text(
                                                    text = "@${req.username}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        color = SlateText
                                                    )
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.acceptFriendRequest(req.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Terima",
                                                    tint = PulseTeal
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.rejectFriendRequest(req.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Tolak",
                                                    tint = ErrorRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Blocked Users Tab
                        if (blockedUsers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Tidak ada pengguna yang diblokir",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = SlateText)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(blockedUsers, key = { it.blockedId }) { item ->
                                    val user = item.blockedProfile
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PanelColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DappAvatar(
                                                avatarUrl = user?.avatarUrl,
                                                displayName = user?.displayName ?: "User",
                                                size = 46.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = user?.displayName ?: "Pengguna Dapp",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = SpaceGroteskFontFamily,
                                                        color = FogWhite
                                                    )
                                                )
                                                Text(
                                                    text = "Diblokir",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        color = ErrorRed
                                                    )
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.unblockUser(item.blockedId)
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "Buka Blokir",
                                                    color = PulseTeal,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = SpaceGroteskFontFamily
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
