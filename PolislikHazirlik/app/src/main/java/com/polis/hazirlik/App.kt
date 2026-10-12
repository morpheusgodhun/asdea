package com.polis.hazirlik

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        dao = AppDb.build(this).dao()
    }

    companion object {
        lateinit var dao: AppDao

        /** Ekran kapansa bile yarım kalmaması gereken yazma işlemleri için. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
