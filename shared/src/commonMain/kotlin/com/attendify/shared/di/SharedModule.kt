package com.attendify.shared.di

import com.attendify.shared.repository.AttendanceRepository
import com.attendify.shared.repository.AuthRepository
import com.attendify.shared.repository.NotificationRepository
import com.attendify.shared.repository.TimetableRepository
import com.attendify.shared.repository.UserRepository
import com.attendify.shared.repository.impl.FirebaseAttendanceRepository
import com.attendify.shared.repository.impl.FirebaseAuthRepository
import com.attendify.shared.repository.impl.FirebaseTimetableRepository
import com.attendify.shared.repository.impl.FirebaseUserRepository
import com.attendify.shared.repository.impl.FirebaseNotificationRepository
import com.attendify.shared.usecase.GetStudentAttendanceSummaryUseCase
import com.attendify.shared.usecase.GetTimetableForDayUseCase
import com.attendify.shared.usecase.MarkAttendanceUseCase
import com.attendify.shared.usecase.VerifyTeacherSessionUseCase
import com.attendify.shared.viewmodel.AttendanceViewModel
import com.attendify.shared.viewmodel.AuthViewModel
import com.attendify.shared.viewmodel.DashboardViewModel
import org.koin.dsl.module

val sharedModule = module {
    // Repositories
    single<AuthRepository> { FirebaseAuthRepository() }
    single<AttendanceRepository> { FirebaseAttendanceRepository() }
    single<TimetableRepository> { FirebaseTimetableRepository() }
    single<UserRepository> { FirebaseUserRepository() }
    single<NotificationRepository> { FirebaseNotificationRepository() }

    // Use Cases
    factory { VerifyTeacherSessionUseCase(get()) }
    factory { MarkAttendanceUseCase(get()) }
    factory { GetStudentAttendanceSummaryUseCase(get(), get()) }
    factory { GetTimetableForDayUseCase(get()) }

    // ViewModels
    factory { AuthViewModel(get()) }
    factory { AttendanceViewModel(get(), get(), get()) }
    factory { DashboardViewModel(get(), get(), get(), get()) }
}
