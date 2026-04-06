package com.attendify.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendify.android.ui.admin.AdminDashboardScreen
import com.attendify.android.ui.auth.LoginScreen
import com.attendify.android.ui.notifications.NotificationsScreen
import com.attendify.android.ui.student.StudentDashboardScreen
import com.attendify.android.ui.student.StudentQRScreen
import com.attendify.android.ui.teacher.AttendanceScannerScreen
import com.attendify.android.ui.teacher.TeacherDashboardScreen
import com.attendify.android.ui.timetable.TimetableScreen
import com.attendify.shared.model.UserRole
import com.attendify.shared.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StudentDashboard : Screen("student_dashboard")
    object StudentQR : Screen("student_qr")
    object TeacherDashboard : Screen("teacher_dashboard")
    object AttendanceScanner : Screen("attendance_scanner/{sessionId}") {
        fun createRoute(sessionId: String) = "attendance_scanner/$sessionId"
    }
    object AdminDashboard : Screen("admin_dashboard")
    object Timetable : Screen("timetable")
    object Notifications : Screen("notifications")
}

@Composable
fun AttendifyNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.state.collectAsState()

    val startDestination = when {
        !authState.isAuthenticated -> Screen.Login.route
        else -> when (authState.user?.userRole) {
            UserRole.STUDENT, UserRole.CLASS_REPRESENTATIVE -> Screen.StudentDashboard.route
            UserRole.TEACHER, UserRole.CLASS_INCHARGE -> Screen.TeacherDashboard.route
            UserRole.HOD, UserRole.PRINCIPAL -> Screen.AdminDashboard.route
            null -> Screen.Login.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { user ->
                    val dest = when (user.userRole) {
                        UserRole.STUDENT, UserRole.CLASS_REPRESENTATIVE -> Screen.StudentDashboard.route
                        UserRole.TEACHER, UserRole.CLASS_INCHARGE -> Screen.TeacherDashboard.route
                        UserRole.HOD, UserRole.PRINCIPAL -> Screen.AdminDashboard.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(
                onNavigateToQR = { navController.navigate(Screen.StudentQR.route) },
                onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }

        composable(Screen.StudentQR.route) {
            StudentQRScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TeacherDashboard.route) {
            TeacherDashboardScreen(
                onStartScanner = { sessionId ->
                    navController.navigate(Screen.AttendanceScanner.createRoute(sessionId))
                },
                onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }

        composable(Screen.AttendanceScanner.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            AttendanceScannerScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }

        composable(Screen.Timetable.route) {
            TimetableScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
    }
}
