package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pastoral.assistant.PastoralAssistantEngine
import com.example.pastoral.auth.AuthManager
import com.example.pastoral.auth.AuthState
import com.example.pastoral.data.repository.PastoralRepository
import com.example.pastoral.ui.MainPastoralApp
import com.example.pastoral.ui.screens.SignInScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = AuthManager(applicationContext)
        val repository = PastoralRepository(applicationContext)
        val assistantEngine = PastoralAssistantEngine(repository)

        setContent {
            val authState by authManager.authState.collectAsStateWithLifecycle()

            when (authState) {
                is AuthState.Authenticated -> {
                    MainPastoralApp(
                        repository = repository,
                        assistantEngine = assistantEngine,
                        authManager = authManager
                    )
                }
                else -> {
                    SignInScreen(authManager = authManager)
                }
            }
        }
    }
}
