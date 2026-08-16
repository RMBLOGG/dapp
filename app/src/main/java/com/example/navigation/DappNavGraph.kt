package com.example.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.DappRepository
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.SetupProfileScreen
import com.example.ui.screens.chat.ChatRoomScreen
import com.example.ui.screens.friends.FriendsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.story.CreateStoryDialog
import com.example.ui.screens.story.StoryViewScreen

object DappDestinations {
    const val AUTH = "auth"
    const val SETUP_PROFILE = "setup_profile"
    const val HOME = "home"
    const val FRIENDS = "friends"
    const val CHAT_ROOM = "chat/{chatId}"
    const val PROFILE = "profile"
    const val STORY_VIEW = "story/{storyId}"

    fun chatRoute(chatId: String) = "chat/$chatId"
    fun storyRoute(storyId: String) = "story/$storyId"
}

@Composable
fun DappNavGraph(
    repository: DappRepository,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by repository.currentUser.collectAsState()
    var showCreateStoryDialog by remember { mutableStateOf(false) }

    val startDestination = if (currentUser != null) DappDestinations.HOME else DappDestinations.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(DappDestinations.AUTH) {
            AuthScreen(
                repository = repository,
                onAuthSuccess = { isNewUser ->
                    if (isNewUser) {
                        navController.navigate(DappDestinations.SETUP_PROFILE) {
                            popUpTo(DappDestinations.AUTH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(DappDestinations.HOME) {
                            popUpTo(DappDestinations.AUTH) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(DappDestinations.SETUP_PROFILE) {
            SetupProfileScreen(
                repository = repository,
                onSetupComplete = {
                    navController.navigate(DappDestinations.HOME) {
                        popUpTo(DappDestinations.SETUP_PROFILE) { inclusive = true }
                    }
                }
            )
        }

        composable(DappDestinations.HOME) {
            HomeScreen(
                repository = repository,
                onOpenChat = { chatId ->
                    navController.navigate(DappDestinations.chatRoute(chatId))
                },
                onOpenFriends = {
                    navController.navigate(DappDestinations.FRIENDS)
                },
                onOpenProfile = {
                    navController.navigate(DappDestinations.PROFILE)
                },
                onOpenStory = { storyId ->
                    navController.navigate(DappDestinations.storyRoute(storyId))
                },
                onAddStory = {
                    showCreateStoryDialog = true
                }
            )

            if (showCreateStoryDialog) {
                CreateStoryDialog(
                    repository = repository,
                    onDismiss = { showCreateStoryDialog = false }
                )
            }
        }

        composable(DappDestinations.FRIENDS) {
            FriendsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onStartChat = { chatId ->
                    navController.navigate(DappDestinations.chatRoute(chatId)) {
                        popUpTo(DappDestinations.FRIENDS) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = DappDestinations.CHAT_ROOM,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatRoomScreen(
                chatId = chatId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(DappDestinations.PROFILE) {
            ProfileScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(DappDestinations.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = DappDestinations.STORY_VIEW,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            StoryViewScreen(
                storyId = storyId,
                repository = repository,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
