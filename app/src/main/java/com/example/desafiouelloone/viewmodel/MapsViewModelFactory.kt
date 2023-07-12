package com.example.desafiouelloone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.desafiouelloone.data.repository.MarkerRepository

class MapsViewModelFactory(private val markerRepository: MarkerRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            return MapsViewModel(markerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}