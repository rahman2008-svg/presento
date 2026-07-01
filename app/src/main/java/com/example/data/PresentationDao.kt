package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresentationDao {
    @Query("SELECT * FROM presentations ORDER BY lastModified DESC")
    fun getAllPresentations(): Flow<List<PresentationEntity>>

    @Query("SELECT * FROM presentations WHERE id = :id")
    suspend fun getPresentationById(id: String): PresentationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresentation(presentation: PresentationEntity)

    @Query("DELETE FROM presentations WHERE id = :id")
    suspend fun deletePresentationById(id: String)

    @Query("DELETE FROM presentations")
    suspend fun deleteAllPresentations()
}
