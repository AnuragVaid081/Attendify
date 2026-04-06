package com.attendify.android.ui.timetable

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
import com.attendify.android.ui.components.TimetableCard
import com.attendify.android.ui.theme.*
import com.attendify.shared.model.DayOfWeek
import com.attendify.shared.viewmodel.AuthViewModel
import com.attendify.shared.viewmodel.TimetableViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    viewModel: TimetableViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    val user = authState.user

    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }

    LaunchedEffect(user) {
        user?.let {
            if (it.classId.isNotEmpty()) {
                viewModel.loadTimetableForClass(it.classId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable", color = OnDarkBackground) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Day selector tabs
            ScrollableTabRow(
                selectedTabIndex = selectedDay.ordinal,
                containerColor = DarkSurface,
                contentColor = AttendifyPrimary,
                edgePadding = 16.dp
            ) {
                DayOfWeek.entries.forEach { day ->
                    Tab(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        text = {
                            Text(
                                day.name.take(3),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            val dayEntries = state.entries.filter { it.dayOfWeek == selectedDay }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AttendifyPrimary)
                }
            } else if (dayEntries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Gray400
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No classes on ${selectedDay.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray400
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(dayEntries) { entry ->
                        TimetableCard(entry = entry, adjustment = null)
                    }
                }
            }
        }
    }
}
