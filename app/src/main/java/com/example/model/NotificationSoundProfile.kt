package com.example.model

import com.example.R

enum class NotificationSoundProfile(val displayName: String, val resId: Int, val systemKey: String) {
    CYBER_ALERT("Cyber Alert", 0, "sound_cyber_alert"),
    MINIMAL_PING("Minimal Ping", 0, "sound_minimal_ping"),
    RETRO_SYNTH("Retro Synth", 0, "sound_retro_synth"),
    AMBIENT_BREEZE("Ambient Breeze", 0, "sound_ambient_breeze"),
    ECHO_DROP("Echo Drop", 0, "sound_echo_drop");

    companion object {
        fun fromSystemKey(key: String?): NotificationSoundProfile {
            return entries.find { it.systemKey == key } ?: MINIMAL_PING
        }
    }
}
