package com.ysdigi.puratrip.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ysdigi.puratrip.R

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onGetStartedClick: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to PuraTrip!",
            description = "Create trips to organize your photos, expenses, and plans all in one place.",
            icon = Icons.Default.CardTravel
        ),
        OnboardingPage(
            title = "Capture Every Moment",
            description = "Upload and share photos with everyone on the trip. Long-press to select, tap to view in full-screen, and pinch to zoom!",
            icon = Icons.Default.PhotoCamera
        ),
        OnboardingPage(
            title = "Track Your Spending",
            description = "Easily add expenses, split them with friends, and see who owes who. Settle up debts with a single tap.",
            icon = Icons.Default.MonetizationOn
        ),
        OnboardingPage(
            title = "Plan Your Adventure",
            description = "Collaborate on a shared trip plan. The rich text editor lets you add links, lists, and formatting to keep everything organized.",
            icon = Icons.Default.Edit
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(12.dp)
                )
            }
        }

        if (pagerState.currentPage == pages.size - 1) {
            Button(
                onClick = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Get Started")
            }
        } else {
            Box(modifier = Modifier.height(68.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                SwipeIndicator()
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(128.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SwipeIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "swipe")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "swipe translation"
    )

    Icon(
        painter = painterResource(id = R.drawable.ic_swipe),
        contentDescription = "Swipe left",
        modifier = Modifier.graphicsLayer(translationX = translationX),
        tint = MaterialTheme.colorScheme.onSurface
    )
}
