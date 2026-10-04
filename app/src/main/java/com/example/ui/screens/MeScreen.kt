package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.generateSixDigitReferralCode
import com.example.ui.components.AdminPostsBannerSection
import com.example.ui.components.KingoLogoBadge
import com.example.ui.components.SupportChatDialog
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

private val PrimaryBlue = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val walletBalance by viewModel.walletBalance.collectAsState()
    val videoTasks by viewModel.videoTasks.collectAsState()
    val adminPosts by viewModel.adminPosts.collectAsState()
    val dismissedPostIds by viewModel.dismissedPostIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val supportMessages by viewModel.supportMessages.collectAsState()
    val appDownloadUrl by viewModel.appDownloadUrl.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSupportChatDialog by remember { mutableStateOf(false) }

    val availableTasks = remember(videoTasks) {
        videoTasks.filter { !it.isCompletionLimitReached || it.isCompleted }
    }
    val completedCount = availableTasks.count { it.isCompleted }

    val myEmail = currentUser?.email?.lowercase() ?: ""
    val myUserId = currentUser?.userId ?: ""
    val myReferralCode = remember(currentUser, myEmail) {
        currentUser?.referralCode?.ifBlank { generateSixDigitReferralCode(myEmail) }
            ?: generateSixDigitReferralCode(myEmail)
    }
    val referredFriendsCount = remember(allUsers, myReferralCode, myEmail) {
        allUsers.count {
            !it.email.equals(myEmail, ignoreCase = true) && it.referredByCode == myReferralCode
        }
    }
    val totalReferralBonusCoins = remember(transactions) {
        transactions.filter {
            it.id.startsWith("ref_withdraw_bonus_") || it.title.contains("Referral", ignoreCase = true)
        }.sumOf { it.coins.coerceAtLeast(0) }
    }
    val mySupportMessages = remember(supportMessages, myEmail, myUserId) {
        supportMessages.filter {
            (myEmail.isNotBlank() && it.userEmail.equals(myEmail, ignoreCase = true)) ||
                (myUserId.isNotBlank() && it.userId == myUserId)
        }.sortedBy { it.timestampMillis }
    }

    // Authentication form state
    var authTabIndex by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Sign Up
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthLoading by remember { mutableStateOf(false) }

    val isUserLoggedIn = currentUser != null && currentUser?.email != "guest@watchearn.com"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B12)),
        containerColor = Color(0xFF070B12),
        topBar = {
            Surface(
                color = Color(0xFF0B111E).copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KingoLogoBadge(
                                isAdmin = false,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = Color.White
                            )
                        }

                        // Top-Right Live Support Button
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showSupportChatDialog = true }
                                .testTag("me_support_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Support",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Support",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E293B))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Announcements / Tab Banners
            item {
                AdminPostsBannerSection(
                    posts = adminPosts,
                    currentTab = "ME",
                    dismissedIds = dismissedPostIds,
                    onDismissPost = { viewModel.dismissAdminPost(it) }
                )
            }

            // User Profile Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = AmberPrimary.copy(alpha = 0.15f), spotColor = AmberPrimary.copy(alpha = 0.25f))
                        .testTag("me_profile_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF161F30),
                                        Color(0xFF0B111E)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        AmberPrimary.copy(alpha = 0.5f),
                                        Color(0xFFFBBF24).copy(alpha = 0.25f),
                                        AmberPrimary.copy(alpha = 0.6f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // User Avatar + Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // VIP Avatar with Crown
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                Brush.linearGradient(listOf(AmberPrimary, Color(0xFFD97706))),
                                                CircleShape
                                            )
                                            .border(2.dp, AmberLight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser?.name?.take(1)?.uppercase() ?: "K",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = currentUser?.name?.ifBlank { "Kingo Member" } ?: "Guest User",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )

                                        Text(
                                            text = currentUser?.email ?: "guest@watchearn.com",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                val uid = currentUser?.userId ?: "GUEST"
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("User ID", uid))
                                                Toast.makeText(context, "User ID copied: $uid", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = "ID: ${currentUser?.userId ?: "GUEST"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberLight
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy ID",
                                                tint = AmberLight,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3 Quick Stats Chips inside Profile Hero (Adaptive & Clean)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileMiniStat(
                                    label = "Wallet Balance",
                                    value = "$walletBalance C",
                                    color = AmberPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .width(1.dp)
                                        .background(Color(0xFF334155))
                                )
                                ProfileMiniStat(
                                    label = "Tasks Done",
                                    value = "$completedCount/${availableTasks.size}",
                                    color = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .width(1.dp)
                                        .background(Color(0xFF334155))
                                )
                                ProfileMiniStat(
                                    label = "Status",
                                    value = "Active 🟢",
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // World-Class Refer & Earn 2.0 (NO "User A" or "User B"!)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = AmberPrimary.copy(alpha = 0.2f), spotColor = AmberPrimary.copy(alpha = 0.3f))
                        .testTag("me_referral_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF161F30),
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        AmberPrimary.copy(alpha = 0.6f),
                                        Color(0xFFFBBF24).copy(alpha = 0.3f),
                                        AmberPrimary.copy(alpha = 0.7f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Section Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(AmberPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Invite & Earn 10%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "10% commission on every friend's cashout",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "10% ROYALTY",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // 6-Digit Code Showcase Box
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF090D16),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "YOUR 6-DIGIT REFERRAL KEY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 10.sp
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, AmberPrimary)
                                    ) {
                                        Text(
                                            text = myReferralCode,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AmberPrimary,
                                            letterSpacing = 4.sp,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                                .testTag("me_referral_code_text")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // One-Tap Copy Button
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.recordSharedReferralCode(myReferralCode)
                                                val directDownloadUrl = viewModel.getEffectiveShareDownloadUrl(myReferralCode)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val copyText = "👑 Kingo King Referral Key: $myReferralCode\n📲 Direct Download App: $directDownloadUrl"
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("Kingo Refer Key", copyText))
                                                Toast.makeText(context, "Referral Key $myReferralCode & Direct Download Link copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("me_copy_referral_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Copy Key",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }

                                        // Direct Share Button (WhatsApp / System)
                                        Button(
                                            onClick = {
                                                viewModel.recordSharedReferralCode(myReferralCode)
                                                val directDownloadUrl = viewModel.getEffectiveShareDownloadUrl(myReferralCode)
                                                val shareMsg = "👑 *Watch & Earn Real Cash with Kingo King!*\n\n" +
                                                    "🎁 Referral Code Attached: *$myReferralCode*\n" +
                                                    "🎉 Get *+50 Free Bonus Coins* on signup!\n\n" +
                                                    "📲 Direct Download App (1-Click Auto-Install):\n$directDownloadUrl"

                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Invite Friends to Kingo King")
                                                context.startActivity(shareIntent)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("me_share_referral_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = Color.Black,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Share Key",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // 3-Step Illustrated Guide (Concise, Clean, No Cramping)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF090D16), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "How Refer & Earn Program Works",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AmberLight
                                )

                                ReferralStepItem(
                                    stepNumber = "1",
                                    title = "Share Your Key",
                                    desc = "Send your key ($myReferralCode) to friends on WhatsApp or Telegram."
                                )
                                ReferralStepItem(
                                    stepNumber = "2",
                                    title = "Friend Installs & Enters Key",
                                    desc = "They sign up with your key & instantly get +50 Welcome Bonus Coins."
                                )
                                ReferralStepItem(
                                    stepNumber = "3",
                                    title = "10% Royalties for Life",
                                    desc = "Earn 10% bonus coins every time your invited friend cashes out!"
                                )
                            }

                            // Referral Analytics Counters (Clean & Responsive)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Friends Joined",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$referredFriendsCount Friends",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CardGiftcard,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Commission",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "+$totalReferralBonusCoins Coins",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = SuccessGreen,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick App Actions & Navigation Group
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Account & Services",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    // Live Support Chat Card
                    AccountActionCard(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconTint = PrimaryBlue,
                        title = "24x7 Live Support Chat",
                        subtitle = "Chat directly with admin • Quick resolution",
                        onClick = { showSupportChatDialog = true }
                    )

                    // Wallet & Passbook Card
                    AccountActionCard(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = AmberPrimary,
                        title = "My Wallet & Payout Passbook",
                        subtitle = "Redeem earned coins directly via UPI ID",
                        onClick = { viewModel.navigateTo(AppScreen.WALLET) }
                    )

                    // Video Watch Tasks Card
                    AccountActionCard(
                        icon = Icons.Default.VideoLibrary,
                        iconTint = SuccessGreen,
                        title = "Video Watch Tasks",
                        subtitle = "$completedCount of ${availableTasks.size} tasks completed",
                        onClick = { viewModel.switchTab(AppScreen.TASKS) }
                    )

                    // Change Password Card (Only if logged in)
                    if (isUserLoggedIn) {
                        AccountActionCard(
                            icon = Icons.Default.Lock,
                            iconTint = AmberLight,
                            title = "Change Account Password",
                            subtitle = "Secure OTP verification & reset",
                            onClick = { showChangePasswordDialog = true }
                        )

                        // Logout Action
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.logout()
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFF43F5E).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = null,
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Sign Out of Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFF43F5E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // If Guest: Show High-Budget Dark Auth Card
            if (!isUserLoggedIn) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.2.dp, AmberPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Sign In or Create Kingo Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            // Tabs: Login / Sign Up
                            TabRow(
                                selectedTabIndex = authTabIndex,
                                containerColor = Color(0xFF1E293B),
                                contentColor = AmberPrimary,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[authTabIndex]),
                                        color = AmberPrimary
                                    )
                                }
                            ) {
                                Tab(
                                    selected = authTabIndex == 0,
                                    onClick = { authTabIndex = 0; authError = null },
                                    text = { Text("Log In", fontWeight = FontWeight.Bold, color = if (authTabIndex == 0) AmberPrimary else Color(0xFF94A3B8)) }
                                )
                                Tab(
                                    selected = authTabIndex == 1,
                                    onClick = { authTabIndex = 1; authError = null },
                                    text = { Text("Sign Up (+50 Coins)", fontWeight = FontWeight.Bold, color = if (authTabIndex == 1) AmberPrimary else Color(0xFF94A3B8)) }
                                )
                            }

                            if (authTabIndex == 1) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Your Name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberPrimary,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberPrimary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password (min 4 characters)") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberPrimary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            authError?.let { err ->
                                Text(
                                    text = err,
                                    color = Color(0xFFF43F5E),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    isAuthLoading = true
                                    authError = null
                                    if (authTabIndex == 0) {
                                        viewModel.login(emailInput, passwordInput) { success, err ->
                                            isAuthLoading = false
                                            if (!success) authError = err
                                        }
                                    } else {
                                        viewModel.signUp(
                                            email = emailInput,
                                            password = passwordInput,
                                            name = nameInput,
                                            referralCodeInput = viewModel.pendingReferralCode.value
                                        ) { success, err ->
                                            isAuthLoading = false
                                            if (!success) authError = err
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                } else {
                                    Text(
                                        text = if (authTabIndex == 0) "Log In to My Account" else "Create Account & Get +50 Coins",
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSupportChatDialog) {
        SupportChatDialog(
            title = "Live Support Chat",
            subtitle = "ID: ${currentUser?.userId ?: "Guest"} • Direct to Admin",
            messages = mySupportMessages,
            isAdminViewer = false,
            onSendMessage = { text ->
                viewModel.sendSupportMessage(text)
            },
            onDismiss = { showSupportChatDialog = false }
        )
    }

    if (showChangePasswordDialog && currentUser != null) {
        var otpCodeInput by remember { mutableStateOf("") }
        var generatedCode by remember { mutableStateOf<String?>(null) }
        var otpSent by remember { mutableStateOf(false) }
        var newPass by remember { mutableStateOf("") }
        var feedbackMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = {
                Text(
                    text = "Change Account Password",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            },
            containerColor = Color(0xFF0F172A),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Account: ${currentUser?.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.sendEmailVerificationOtp(
                                email = currentUser?.email ?: "",
                                isPasswordReset = true
                            ) { ok, msg, code ->
                                feedbackMsg = msg
                                if (ok) {
                                    otpSent = true
                                    generatedCode = code
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (otpSent) "Resend 6-Digit OTP" else "Send Verification OTP", color = AmberPrimary)
                    }
                    generatedCode?.let { code ->
                        Text(
                            text = "Verified OTP: $code (Tap to Auto-Fill)",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { otpCodeInput = code }
                        )
                    }
                    if (otpSent) {
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { if (it.length <= 6) otpCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("6-Digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password (min 4 chars)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    feedbackMsg?.let {
                        Text(it, fontSize = 12.sp, color = AmberDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPasswordWithOtp(
                            email = currentUser?.email ?: "",
                            enteredOtp = otpCodeInput,
                            newPassword = newPass
                        ) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) showChangePasswordDialog = false
                            else feedbackMsg = msg
                        }
                    },
                    enabled = otpSent && otpCodeInput.length == 6 && newPass.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update Password", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun ProfileMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReferralStepItem(
    stepNumber: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AmberPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun AccountActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
