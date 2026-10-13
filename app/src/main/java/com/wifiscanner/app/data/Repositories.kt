package com.wifiscanner.app.data

import com.wifiscanner.app.domain.Finding
import com.wifiscanner.app.domain.FindingKind
import com.wifiscanner.app.domain.SpeedResult
import com.wifiscanner.app.domain.Validators
import com.wifiscanner.app.domain.WifiAccessPoint
import com.wifiscanner.app.security.KeystoreCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScanHistoryRepository(private val dao: ScanHistoryDao) {
    fun recent(limit: Int = 5): Flow<List<ScanHistoryEntity>> = dao.recent(limit)

    suspend fun record(results: List<WifiAccessPoint>, timestamp: Long) {
        val best = results.maxByOrNull { it.rssi }
        dao.insert(
            ScanHistoryEntity(
                timestamp = timestamp,
                networkCount = results.size,
                strongestSsid = best?.ssid,
                strongestRssi = best?.rssi
            )
        )
        dao.trim()
    }

    suspend fun clear() = dao.clear()
}

class SpeedResultRepository(private val dao: SpeedResultDao) {
    fun recent(limit: Int = 20): Flow<List<SpeedResultEntity>> = dao.recent(limit)
    fun latest(): Flow<SpeedResultEntity?> = dao.latest()

    suspend fun save(result: SpeedResult) {
        dao.insert(
            SpeedResultEntity(
                timestamp = result.timestamp,
                downloadMbps = result.downloadMbps,
                uploadMbps = result.uploadMbps,
                latencyMs = result.latencyMs,
                jitterMs = result.jitterMs,
                server = result.server
            )
        )
        dao.trim()
    }

    suspend fun clear() = dao.clear()
}

class SecurityCheckRepository(private val dao: SecurityCheckDao) {
    fun latest(): Flow<SecurityCheckEntity?> = dao.latest()

    suspend fun record(findings: List<Finding>, timestamp: Long) {
        dao.insert(
            SecurityCheckEntity(
                timestamp = timestamp,
                warnings = findings.count { it.kind == FindingKind.WARNING },
                verified = findings.count { it.kind == FindingKind.VERIFIED },
                unavailable = findings.count { it.kind == FindingKind.UNAVAILABLE }
            )
        )
        dao.trim()
    }

    suspend fun clear() = dao.clear()
}

enum class SaveResult { OK, INVALID_SSID, INVALID_PASSWORD, DUPLICATE, ERROR }

/** Saved networks. Passwords exist only as Keystore-backed ciphertext in the database. */
class SavedNetworkRepository(private val dao: SavedNetworkDao) {
    val all: Flow<List<SavedNetworkEntity>> = dao.observeAll()

    suspend fun add(ssid: String, password: String?): SaveResult = withContext(Dispatchers.Default) {
        val name = ssid.trim()
        val pw = password?.takeIf { it.isNotEmpty() }
        validate(name, pw)?.let { return@withContext it }
        if (dao.findBySsid(name) != null) return@withContext SaveResult.DUPLICATE
        try {
            val now = System.currentTimeMillis()
            dao.insert(
                SavedNetworkEntity(
                    ssid = name,
                    encryptedPassword = pw?.let { KeystoreCipher.encrypt(it) },
                    createdAt = now,
                    updatedAt = now
                )
            )
            SaveResult.OK
        } catch (e: Exception) {
            SaveResult.ERROR
        }
    }

    /** [newPassword] empty keeps the stored password unless [removePassword] is true. */
    suspend fun update(
        entity: SavedNetworkEntity,
        ssid: String,
        newPassword: String?,
        removePassword: Boolean
    ): SaveResult = withContext(Dispatchers.Default) {
        val name = ssid.trim()
        val pw = newPassword?.takeIf { it.isNotEmpty() }
        validate(name, pw)?.let { return@withContext it }
        val clash = dao.findBySsid(name)
        if (clash != null && clash.id != entity.id) return@withContext SaveResult.DUPLICATE
        try {
            val encrypted = when {
                pw != null -> KeystoreCipher.encrypt(pw)
                removePassword -> null
                else -> entity.encryptedPassword
            }
            dao.update(entity.copy(ssid = name, encryptedPassword = encrypted, updatedAt = System.currentTimeMillis()))
            SaveResult.OK
        } catch (e: Exception) {
            SaveResult.ERROR
        }
    }

    /** Used by Quick Connect: stores or replaces the password for [ssid]. */
    suspend fun upsertPassword(ssid: String, password: String): SaveResult {
        val existing = dao.findBySsid(ssid)
        return if (existing == null) add(ssid, password) else update(existing, ssid, password, false)
    }

    suspend fun reveal(entity: SavedNetworkEntity): String? = withContext(Dispatchers.Default) {
        entity.encryptedPassword?.let { runCatching { KeystoreCipher.decrypt(it) }.getOrNull() }
    }

    suspend fun delete(entity: SavedNetworkEntity) = dao.delete(entity)

    /** Removes every row and destroys the encryption key. */
    suspend fun deleteAll() {
        dao.deleteAll()
        KeystoreCipher.deleteKey()
    }

    private fun validate(name: String, password: String?): SaveResult? = when {
        !Validators.isValidSsid(name) -> SaveResult.INVALID_SSID
        password != null && !Validators.isValidPassphrase(password) -> SaveResult.INVALID_PASSWORD
        else -> null
    }
}
