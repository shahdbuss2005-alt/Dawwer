package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.compose.ui.text.font.FontWeight


enum class AppDestination(val route: String) {
    HOME("home"),
    SCANNER("scanner"),
    CHALLENGES("challenges"),
    REWARDS("rewards"),
    PROFILE("profile")
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationHost(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    val autoTriggerVoice by viewModel.autoTriggerVoiceAssistant.collectAsState()
    LaunchedEffect(autoTriggerVoice) {
        if (autoTriggerVoice) {
            currentDestination = AppDestination.SCANNER
        }
    }

    if (!hasSeenOnboarding) {
        OnboardingScreen(
            viewModel = viewModel,
            onFinish = { viewModel.completeOnboarding() }
        )
    } else if (!isLoggedIn) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = { /* Handle navigation to Home */ }
        )
    } else {
        Scaffold(
            bottomBar = {
                // Customized Curved Floating Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        // Arranged RTL Right to Left (Profile | Challenges | Map | Scanner | Home)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home Tab
                            NavigationItem(
                                icon = Icons.Default.Home,
                                label = viewModel.translate("home"),
                                isSelected = currentDestination == AppDestination.HOME,
                                onClick = { currentDestination = AppDestination.HOME },
                                modifier = Modifier.testTag("nav_home")
                            )

                            // Scanner Tab
                            NavigationItem(
                                icon = Icons.Default.QrCodeScanner,
                                label = viewModel.translate("scanner"),
                                isSelected = currentDestination == AppDestination.SCANNER,
                                onClick = { currentDestination = AppDestination.SCANNER },
                                modifier = Modifier.testTag("nav_scanner")
                            )

                            // Challenges Tab
                            NavigationItem(
                                icon = Icons.Default.EmojiEvents,
                                label = viewModel.translate("challenges"),
                                isSelected = currentDestination == AppDestination.CHALLENGES,
                                onClick = { currentDestination = AppDestination.CHALLENGES },
                                modifier = Modifier.testTag("nav_challenges")
                            )

                            // Rewards Tab
                            NavigationItem(
                                icon = Icons.Default.CardGiftcard,
                                label = viewModel.translate("rewards"),
                                isSelected = currentDestination == AppDestination.REWARDS,
                                onClick = { currentDestination = AppDestination.REWARDS },
                                modifier = Modifier.testTag("nav_rewards")
                            )

                            // Profile Tab
                            NavigationItem(
                                icon = Icons.Default.Person,
                                label = viewModel.translate("profile"),
                                isSelected = currentDestination == AppDestination.PROFILE,
                                onClick = { currentDestination = AppDestination.PROFILE },
                                modifier = Modifier.testTag("nav_profile")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Multi Screen Navigation Router with Animations
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                    },
                    label = "navigation_crossfade"
                ) { targetDest ->
                    when (targetDest) {
                        AppDestination.HOME -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { currentDestination = AppDestination.SCANNER }
                        )
                        AppDestination.SCANNER -> ScannerScreen(
                            viewModel = viewModel
                        )
                        AppDestination.CHALLENGES -> ChallengesScreen(viewModel = viewModel)
                        AppDestination.REWARDS -> RewardsScreen(viewModel = viewModel)
                        AppDestination.PROFILE -> ProfileSettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(if (isSelected) 26.dp else 22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
