package com.salaryapp.jigong

import android.app.Application
import com.salaryapp.jigong.data.repository.AppContainer

class JiGongApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
