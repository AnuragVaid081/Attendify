package com.attendify.android.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.attendify.android.ui.components.*
import com.attendify.android.ui.theme.*
import com.attendify.shared.model.LectureSession
import com.attendify.shared.model.SessionStatus
import com.attendify.shared.viewmodel.AttendanceViewModel
import com.attendify.shared.viewmodel.AuthViewModel
import com.attendify.shared.viewmodel.DashboardViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    onStartScanner: (String) -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    dashboardViewModel: DashboardViewModel = koinViewModel(),
    attendanceViewModel: AttendanceViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val dashState by dashboardViewModel.state.collectAsState()
    val attState by attendanceViewModel.state.collectAsState()
    val user = authState.user

    LaunchedEffect(user?.id) {
        user?.id?.let { dashboardViewModel.loadTeacherDashboard(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Teacher Panel", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                        user?.name?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = OnDarkBackground)
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Gray400)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Quick actions
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Start Session",
                        subtitle = "Initiate attendance",
                        icon = Icons.Default.PlayArrow,
                        color = ColorPresent,
                        onClick = {
                            // Open dialog to select class/subject — simplified: create a session
                            user?.let { teacher ->
                                attendanceViewModel.createSession(
                                    timetableEntryId = "manual",
                                    classId = teacher.classId,
                                    subjectId = teacher.subjectIds.firstOrNull() ?: "",
                                    teacherId = teacher.id,
                                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
                                    initiatedBy = teacher.id
                                )
                            }
                        }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Scan QR",
                        subtitle = "Mark attendance",
                        icon = Icons.Default.QrCodeScanner,
                        color = AttendifyPrimary,
                        onClick = {
                            attState.activeSession?.id?.let { onStartScanner(it) }
                        }
                    )
                }
            }

            // Active session status
            attState.activeSession?.let { session ->
                item {
                    Text("Active Session", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                }
                item {
                    ActiveSessionCard(
                        session = session,
                        recordCount = attState.records.size,
                        onScan = { onStartScanner(session.id) },
                        onLock = { attendanceViewModel.lockSession(session.id) }
                    )
                }
            }

            // Recent sessions
            if (dashState.recentSessions.isNotEmpty()) {
                item {
                    Text("Recent Sessions", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                }
                items(dashState.recentSessions) { session ->
                    SessionHistoryCard(session = session)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    session: LectureSession,
    recordCount: Int,
    onScan: () -> Unit,
    onLock: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = DarkSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Session Active", style = MaterialTheme.typography.titleSmall, color = ColorPresent)
                    Text(
                        "$recordCount / ${session.totalStudents} marked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDarkBackground
                    )
                }
                SessionStatusBadge(status = session.sessionStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (session.sessionStatus == SessionStatus.PENDING) {
                Text(
                    "⚠ Awaiting teacher QR verification",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorLow
                )
            } else if (session.sessionStatus == SessionStatus.ACTIVE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onScan,
                        colors = ButtonDefaults.buttonColors(containerColor = AttendifyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan")
                    }
                    OutlinedButton(
                        onClick = onLock,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock", color = ColorAbsent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(session: LectureSession) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = CardBackground
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.date, style = MaterialTheme.typography.bodyMedium, color = OnDarkBackground)
                Text(
                    "${session.markedCount} students marked",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDarkSurface
                )
            }
            SessionStatusBadge(status = session.sessionStatus)
        }
    }
}
