package com.attendify.android.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.attendify.android.ui.components.*
import com.attendify.android.ui.theme.*
import com.attendify.shared.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    onNavigateToQR: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
    authViewModel: com.attendify.shared.viewmodel.AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    val user = authState.user

    LaunchedEffect(user?.id) {
        user?.id?.let { viewModel.loadStudentDashboard(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                        user?.name?.let {
                            Text("Welcome, $it", style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = OnDarkBackground)
                    }
                    IconButton(onClick = onNavigateToQR) {
                        Icon(Icons.Default.QrCode, contentDescription = "My QR", tint = AttendifyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            AttendifyBottomNav(
                currentRoute = "dashboard",
                onDashboard = {},
                onTimetable = onNavigateToTimetable,
                onNotifications = onNavigateToNotifications
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AttendifyPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Overall attendance banner
                item {
                    OverallAttendanceBanner(
                        percentage = state.overallAttendance,
                        lowAttendanceCount = state.lowAttendanceSubjects.size
                    )
                }

                // Low attendance warning
                if (state.lowAttendanceSubjects.isNotEmpty()) {
                    item {
                        LowAttendanceWarning(subjects = state.lowAttendanceSubjects.map { it.subjectName })
                    }
                }

                // Stats row
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Subjects",
                            value = state.attendanceSummaries.size.toString(),
                            icon = Icons.Default.MenuBook,
                            color = AttendifyPrimary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Today's Classes",
                            value = state.todaySchedule.size.toString(),
                            icon = Icons.Default.CalendarToday,
                            color = AttendifySecondary
                        )
                    }
                }

                // Today's schedule
                item {
                    Text(
                        "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnDarkBackground
                    )
                }

                if (state.todaySchedule.isEmpty()) {
                    item {
                        EmptyStateCard(message = "No classes scheduled today")
                    }
                } else {
                    items(state.todaySchedule) { entry ->
                        val adjustment = state.adjustments[entry.id]
                        TimetableCard(entry = entry, adjustment = adjustment)
                    }
                }

                // Attendance per subject
                item {
                    Text(
                        "Attendance Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnDarkBackground
                    )
                }

                items(state.attendanceSummaries) { summary ->
                    AttendanceSubjectCard(summary = summary)
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OverallAttendanceBanner(percentage: Float, lowAttendanceCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AttendifyPrimaryDark, AttendifyPrimary)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Overall Attendance", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        "${percentage.toInt()}%",
                        style = MaterialTheme.typography.displayLarge.copy(color = Color.White)
                    )
                    if (lowAttendanceCount > 0) {
                        Text(
                            "$lowAttendanceCount subject(s) below 75%",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorLow
                        )
                    }
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier.size(80.dp),
                        color = AttendifySecondary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 7.dp
                    )
                    Text(
                        "${percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LowAttendanceWarning(subjects: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ColorLow.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ColorLow, modifier = Modifier.size(20.dp))
            Text(
                "Low attendance in: ${subjects.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = ColorLow,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
