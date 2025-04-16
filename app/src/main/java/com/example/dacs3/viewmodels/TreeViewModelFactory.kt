package com.example.dacs3.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TreeViewModelFactory(private val context: Context, private val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TreeViewModel(context, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}