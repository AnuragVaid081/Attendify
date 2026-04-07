package com.attendify.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TableChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.attendify.android.ui.theme.*
import com.attendify.shared.model.AttendanceSummary
import com.attendify.shared.model.LectureAdjustment
import com.attendify.shared.model.SessionStatus
import com.attendify.shared.model.TimetableEntry

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = OnDarkBackground)
            Text(label, style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
        }
    }
}

// ─── Timetable Card ───────────────────────────────────────────────────────────

@Composable
fun TimetableCard(
    entry: TimetableEntry,
    adjustment: LectureAdjustment?
) {
    val isCancelled = adjustment?.adjustmentType == "CANCEL"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isCancelled) ColorAbsent.copy(alpha = 0.08f) else CardBackground
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isCancelled) ColorAbsent
                        else if (adjustment != null) ColorLow
                        else AttendifyPrimary
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCancelled) Gray400 else OnDarkBackground
                )
                Text(
                    "${entry.timeSlot.startTime} – ${entry.timeSlot.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDarkSurface
                )
                Text(
                    "📍 ${entry.venue}  •  ${entry.teacherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDarkSurface
                )

                adjustment?.let {
                    val label = when (it.adjustmentType) {
                        "CANCEL" -> "🚫 Cancelled"
                        "RESCHEDULE" -> "🔄 Rescheduled"
                        "SWAP" -> "↔ Swapped"
                        else -> it.adjustmentType
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCancelled) ColorAbsent else ColorLow,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Text(
                "P${entry.period}",
                style = MaterialTheme.typography.labelLarge,
                color = Gray400
            )
        }
    }
}

// ─── Attendance Subject Card ──────────────────────────────────────────────────

@Composable
fun AttendanceSubjectCard(summary: AttendanceSummary) {
    val pct = summary.percentage
    val barColor = when {
        pct >= 75f -> ColorPresent
        pct >= 60f -> ColorLow
        else -> ColorAbsent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = CardBackground
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    summary.subjectName.ifEmpty { summary.subjectId },
                    style = MaterialTheme.typography.titleSmall,
                    color = OnDarkBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${pct.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = barColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${summary.attendedLectures}/${summary.totalLectures} lectures attended",
                style = MaterialTheme.typography.bodySmall,
                color = OnDarkSurface
            )
        }
    }
}

// ─── Session Status Badge ─────────────────────────────────────────────────────

@Composable
fun SessionStatusBadge(status: SessionStatus) {
    val (label, color) = when (status) {
        SessionStatus.PENDING -> "Pending" to ColorPending
        SessionStatus.ACTIVE -> "Active" to ColorPresent
        SessionStatus.LOCKED -> "Locked" to Gray400
        SessionStatus.CANCELLED -> "Cancelled" to ColorAbsent
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ─── Action Card ──────────────────────────────────────────────────────────────

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.12f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = OnDarkBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
        }
    }
}

// ─── Empty State Card ─────────────────────────────────────────────────────────

@Composable
fun EmptyStateCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = DarkSurfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Gray400)
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
fun AttendifyBottomNav(
    currentRoute: String,
    onDashboard: () -> Unit,
    onTimetable: () -> Unit,
    onNotifications: () -> Unit
) {
    NavigationBar(containerColor = DarkSurface) {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = onDashboard,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "timetable",
            onClick = onTimetable,
            icon = { Icon(Icons.AutoMirrored.Filled.TableChart, contentDescription = null) },
            label = { Text("Timetable") }
        )
        NavigationBarItem(
            selected = currentRoute == "notifications",
            onClick = onNotifications,
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            label = { Text("Alerts") }
        )
    }
}
