package com.example.desafiouelloone.data.repository

import com.example.desafiouelloone.data.db.MarkerDao
import com.example.desafiouelloone.data.models.MarkerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarkerRepository(private val markerDao: MarkerDao) {

    suspend fun getAllMarkers(): List<MarkerEntity>? = withContext(Dispatchers.IO) {
        markerDao.getAllMarkers()
    }

    suspend fun insertMarker(marker: MarkerEntity) = withContext(Dispatchers.IO) {
        markerDao.insertMarker(marker)
    }

    suspend fun deleteAllMarkers() = withContext(Dispatchers.IO) {
        markerDao.deleteAllMarkers()
    }
}