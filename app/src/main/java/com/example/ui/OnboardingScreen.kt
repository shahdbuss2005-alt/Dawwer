package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreamPaper
import com.example.ui.theme.EcoBlack
import com.example.ui.theme.LeafGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onFinish: () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        Box(
            modifier = Modifier.fillMaxSize().background(EcoBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Recycling, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(100.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("EcoScanner", color = LeafGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        OnboardingPager(viewModel, onFinish)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPager(viewModel: MainViewModel, onFinish: () -> Unit) {
    val currentLang = viewModel.currentLanguage.collectAsState().value
    val isAr = currentLang == "ar"

    val pages = listOf(
        OnboardingPage(
            titleAr = "امسح واكتشف",
            titleEn = "Scan & Discover",
            descAr = "التقط صورة للمخلفات وسيقوم الذكاء الاصطناعي بتحديد نوعها وقابليتها لإعادة التدوير.",
            descEn = "Take a picture of waste and AI will identify its type and recyclability.",
            icon = Icons.Default.CameraAlt
        ),
        OnboardingPage(
            titleAr = "أفكار إبداعية لإعادة الاستخدام",
            titleEn = "Creative Upcycling Ideas",
            descAr = "احصل على خريطة ذهنية مليئة بالأفكار الإبداعية للاستفادة من مخلفاتك.",
            descEn = "Get a mind map full of creative ideas to upcycle and reuse your waste.",
            icon = Icons.Default.Lightbulb
        ),
        OnboardingPage(
            titleAr = "احمِ بيئتك",
            titleEn = "Protect Your Environment",
            descAr = "اجمع النقاط وتنافس في التحديات مع تقليل انبعاثات الكربون وحماية الأرض.",
            descEn = "Collect points and compete in challenges while reducing carbon emissions.",
            icon = Icons.Default.EmojiNature
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onFinish()
        } else {
            onFinish()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(LeafGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(pages[page].icon, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(80.dp))
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = if (isAr) pages[page].titleAr else pages[page].titleEn,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isAr) pages[page].descAr else pages[page].descEn,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Pager indicators
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) LeafGreen else LeafGreen.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = { onFinish() }) {
                    Text(text = if (isAr) "تخطي" else "Skip", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (isAr) "التالي" else "Next")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = EcoBlack),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isAr) "ابدأ الاستخدام" else "Get Started")
                }
            }
        }
    }
}
