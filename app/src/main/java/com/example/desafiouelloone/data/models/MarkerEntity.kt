package com.example.desafiouelloone.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "markers")
data class MarkerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val latitude: Double,

    val longitude: Double,

    val distance: Float
)
