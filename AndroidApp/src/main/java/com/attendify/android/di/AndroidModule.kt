package com.attendify.android.di

import com.attendify.shared.viewmodel.TimetableViewModel
import org.koin.dsl.module

val androidModule = module {
    factory { TimetableViewModel(get()) }
}
