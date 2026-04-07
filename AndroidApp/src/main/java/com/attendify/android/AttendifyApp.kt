package com.attendify.android

import android.app.Application
import com.attendify.shared.di.sharedModule
import com.google.firebase.FirebaseApp
import com.attendify.android.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AttendifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startKoin {
            androidContext(this@AttendifyApp)
            modules(sharedModule, androidModule)
        }
    }
}
