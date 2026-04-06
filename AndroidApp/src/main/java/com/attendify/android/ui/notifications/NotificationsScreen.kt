package com.attendify.android.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.attendify.android.ui.theme.*
import com.attendify.shared.model.NotificationModel
import com.attendify.shared.repository.NotificationRepository
import com.attendify.shared.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel(),
    notificationRepository: NotificationRepository = koinInject()
) {
    val authState by authViewModel.state.collectAsState()
    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }

    LaunchedEffect(authState.user?.id) {
        authState.user?.id?.let { userId ->
            notificationRepository.observeNotifications(userId).collectLatest {
                notifications = it
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = OnDarkBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnDarkBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Gray400
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No notifications yet", style = MaterialTheme.typography.bodyMedium, color = Gray400)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(notifications) { notif ->
                    NotificationCard(
                        notification = notif,
                        onTap = {
                            notificationRepository.let { /* markAsRead */ }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationModel, onTap: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (notification.isRead) CardBackground else DarkSurfaceVariant,
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val (icon, iconColor) = when (notification.type) {
                "RESCHEDULE" -> Icons.Default.Schedule to ColorLow
                "CANCEL" -> Icons.Default.Cancel to ColorAbsent
                else -> Icons.Default.Info to AttendifyPrimary
            }

            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnDarkBackground
                )
                Text(
                    notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDarkSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!notification.isRead) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = AttendifyPrimary,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}
