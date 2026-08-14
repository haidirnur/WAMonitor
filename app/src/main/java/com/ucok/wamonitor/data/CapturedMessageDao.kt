package com.ucok.wamonitor.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CapturedMessageDao {

    @Insert
    suspend fun insert(message: CapturedMessage)

    @Query("SELECT * FROM captured_messages ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<CapturedMessage>>

    @Query("DELETE FROM captured_messages")
    suspend fun clearAll()
}
