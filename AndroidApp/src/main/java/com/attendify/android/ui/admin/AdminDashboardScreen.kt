package com.attendify.android.ui.admin

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
import com.attendify.android.ui.components.StatCard
import com.attendify.android.ui.theme.*
import com.attendify.shared.viewmodel.AuthViewModel
import com.attendify.shared.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToTimetable: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val state by viewModel.state.collectAsState()
    val user = authState.user

    LaunchedEffect(user?.departmentId) {
        user?.departmentId?.let { viewModel.loadAdminDashboard(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Dashboard", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                        user?.name?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
                        }
                    }
                },
                actions = {
                    RoleBadge(role = user?.userRole?.name ?: "")
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Gray400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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

                // Overview stats
                item {
                    Text("Department Overview", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Students",
                            value = state.totalStudents.toString(),
                            icon = Icons.Default.People,
                            color = AttendifyPrimary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Teachers",
                            value = state.classRoster.size.toString(),
                            icon = Icons.Default.Person,
                            color = AttendifySecondary
                        )
                    }
                }

                // Quick actions
                item {
                    Text("Actions", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminActionButton(
                            modifier = Modifier.weight(1f),
                            label = "Timetable",
                            icon = Icons.Default.TableChart,
                            onClick = onNavigateToTimetable
                        )
                        AdminActionButton(
                            modifier = Modifier.weight(1f),
                            label = "Notifications",
                            icon = Icons.Default.Notifications,
                            onClick = onNavigateToNotifications
                        )
                    }
                }

                // Teachers list
                if (state.classRoster.isNotEmpty()) {
                    item {
                        Text("Teaching Staff", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                    }
                    items(state.classRoster) { teacher ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = AttendifyPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                teacher.name.firstOrNull()?.toString() ?: "T",
                                                color = AttendifyPrimary,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                    Column {
                                        Text(teacher.name, style = MaterialTheme.typography.bodyMedium, color = OnDarkBackground)
                                        Text(teacher.email, style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gray400)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AdminActionButton(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RoleBadge(role: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = AttendifyPrimary.copy(alpha = 0.2f)
    ) {
        Text(
            text = role.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = AttendifyPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
