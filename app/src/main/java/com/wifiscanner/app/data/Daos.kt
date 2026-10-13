package com.wifiscanner.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<ScanHistoryEntity>>

    @Query("DELETE FROM scan_history WHERE id NOT IN (SELECT id FROM scan_history ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trim()

    @Query("DELETE FROM scan_history")
    suspend fun clear()
}

@Dao
interface SpeedResultDao {
    @Insert
    suspend fun insert(entity: SpeedResultEntity): Long

    @Query("SELECT * FROM speed_results ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<SpeedResultEntity>>

    @Query("SELECT * FROM speed_results ORDER BY timestamp DESC LIMIT 1")
    fun latest(): Flow<SpeedResultEntity?>

    @Query("DELETE FROM speed_results WHERE id NOT IN (SELECT id FROM speed_results ORDER BY timestamp DESC LIMIT 50)")
    suspend fun trim()

    @Query("DELETE FROM speed_results")
    suspend fun clear()
}

@Dao
interface SecurityCheckDao {
    @Insert
    suspend fun insert(entity: SecurityCheckEntity): Long

    @Query("SELECT * FROM security_checks ORDER BY timestamp DESC LIMIT 1")
    fun latest(): Flow<SecurityCheckEntity?>

    @Query("DELETE FROM security_checks WHERE id NOT IN (SELECT id FROM security_checks ORDER BY timestamp DESC LIMIT 30)")
    suspend fun trim()

    @Query("DELETE FROM security_checks")
    suspend fun clear()
}

@Dao
interface SavedNetworkDao {
    @Query("SELECT * FROM saved_networks ORDER BY ssid COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SavedNetworkEntity>>

    @Query("SELECT * FROM saved_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun findBySsid(ssid: String): SavedNetworkEntity?

    @Insert
    suspend fun insert(entity: SavedNetworkEntity): Long

    @Update
    suspend fun update(entity: SavedNetworkEntity)

    @Delete
    suspend fun delete(entity: SavedNetworkEntity)

    @Query("DELETE FROM saved_networks")
    suspend fun deleteAll()
}
