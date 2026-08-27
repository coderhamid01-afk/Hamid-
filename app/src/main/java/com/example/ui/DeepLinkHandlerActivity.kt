package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.MainActivity

class DeepLinkHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUri = intent?.data
        Log.d("DeepLinkHandler", "Received deep link uri: $incomingUri")

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            if (incomingUri != null) {
                data = incomingUri
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(mainIntent)
        finish()
    }
}
