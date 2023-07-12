package com.example.desafiouelloone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.desafiouelloone.data.models.MarkerEntity

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers")
    fun getAllMarkers(): List<MarkerEntity>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMarker(marker: MarkerEntity)

    @Query("DELETE FROM markers")
    fun deleteAllMarkers()
}