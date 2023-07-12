package com.example.desafiouelloone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.desafiouelloone.data.models.MarkerEntity

@Database(entities = [MarkerEntity::class], version = 1, exportSchema = false)
abstract class MarkersDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao

    companion object {
        @Volatile
        private var instance: MarkersDatabase? = null

        fun getInstance(context: Context): MarkersDatabase {
            return instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarkersDatabase::class.java,
                    "markers_database"
                ).build()
                Companion.instance = instance
                instance
            }
        }
    }
}