package com.example.dacs3.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dacs3.data.TreeState
import com.example.dacs3.data.UserDatabase
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Tree(
    val treeState: TreeState = TreeState.Seed,
    val wateringHistory: List<String> = emptyList(),
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null
)

class TreeViewModel(
    private val context: Context,
    private val userId: String
) : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://dacs3-5cf79-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val userDatabase = UserDatabase()
    private val _activeTree = MutableLiveData<Tree>()
    val activeTree: LiveData<Tree> = _activeTree
    private val _isWateredToday = MutableLiveData<Boolean>()
    val isWateredToday: LiveData<Boolean> = _isWateredToday

    init {
        loadTreeData()
    }

    private fun loadTreeData() {
        database.getReference("trees").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tree = snapshot.getValue(Tree::class.java) ?: Tree()
                _activeTree.value = tree
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                _isWateredToday.value = tree.wateringHistory.contains(today)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun waterTree(fcmToken: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTree = _activeTree.value ?: Tree()
        if (currentTree.wateringHistory.contains(today)) return

        val newHistory = currentTree.wateringHistory + today
        val newState = when {
            newHistory.size >= 30 -> TreeState.Tree
            newHistory.size >= 15 -> TreeState.Sapling
            newHistory.size >= 5 -> TreeState.Sprout
            else -> TreeState.Seed
        }

        val updatedTree = currentTree.copy(
            treeState = newState,
            wateringHistory = newHistory
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Cập nhật cây
                database.getReference("trees").child(userId).setValue(updatedTree).await()
                // Cộng 1 điểm và thêm giao dịch
                val transaction = Transaction(
                    id = database.getReference("transactions").push().key ?: "",
                    type = "watering",
                    points = 1,
                    timestamp = System.currentTimeMillis(),
                    details = "Tưới cây ngày $today"
                )
                userDatabase.addPointsAndTransaction(userId, 1, transaction)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun setWateringReminder(hour: Int, minute: Int) {
        val currentTree = _activeTree.value ?: Tree()
        val updatedTree = currentTree.copy(
            reminderHour = hour,
            reminderMinute = minute
        )
        database.getReference("trees").child(userId).setValue(updatedTree)
    }
}