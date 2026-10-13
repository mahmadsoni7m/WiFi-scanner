package com.wifiscanner.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val networkCount: Int,
    val strongestSsid: String?,
    val strongestRssi: Int?
)

@Entity(tableName = "speed_results")
data class SpeedResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Double,
    val jitterMs: Double,
    val server: String
)

/** Password is stored ONLY as AES-GCM ciphertext (Android Keystore key), never in plaintext. */
@Entity(tableName = "saved_networks", indices = [Index(value = ["ssid"], unique = true)])
data class SavedNetworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val encryptedPassword: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "security_checks")
data class SecurityCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val warnings: Int,
    val verified: Int,
    val unavailable: Int
)
