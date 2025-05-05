package com.example.dacs3.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.TreeData
import com.example.dacs3.data.TreeState
import com.example.dacs3.service.NotificationService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TreeViewModel(
    private val context: Context,
    private val userId: String,
    private val notificationService: NotificationService
) : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _treeList = MutableLiveData<MutableList<TreeData>>()
    val treeList: LiveData<MutableList<TreeData>> = _treeList

    private val _activeTree = MutableLiveData<TreeData>()
    val activeTree: LiveData<TreeData> = _activeTree

    private val _isWateredToday = MutableLiveData<Boolean>()
    val isWateredToday: LiveData<Boolean> = _isWateredToday

    init {
        NotificationService.initialize(context)
        loadTreeList()
    }

    private fun loadTreeList() {
        if (userId.isEmpty()) {
            Log.e("TreeViewModel", "User ID is empty")
            return
        }

        val treeRef = database.getReference("trees").child(userId)
        treeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val treeList = mutableListOf<TreeData>()
                for (treeSnapshot in snapshot.children) {
                    val tree = treeSnapshot.getValue(TreeData::class.java)
                    tree?.let { treeList.add(it) }
                }
                _treeList.value = treeList
                val activeTree = treeList.lastOrNull() ?: TreeData(userId = userId, treeId = UUID.randomUUID().toString())
                _activeTree.value = activeTree
                updateWateringStatus(activeTree)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TreeViewModel", "Failed to load tree list", error.toException())
            }
        })
    }

    private fun updateWateringStatus(tree: TreeData) {
        val today = LocalDate.now().format(dateFormatter)
        _isWateredToday.value = tree.lastWateredDate == today
    }

    private fun saveTree(tree: TreeData) {
        if (userId.isEmpty()) {
            Log.e("TreeViewModel", "User ID is empty")
            return
        }

        val treeRef = database.getReference("trees").child(userId).child(tree.treeId)
        treeRef.setValue(tree)
            .addOnSuccessListener {
                Log.d("TreeViewModel", "Tree saved successfully")
            }
            .addOnFailureListener { error ->
                Log.e("TreeViewModel", "Failed to save tree", error)
            }
    }

    fun waterTree(fcmToken: String) {
        Log.d("TreeViewModel", "waterTree() called")
        val activeTree = _activeTree.value ?: return
        val today = LocalDate.now()
        Log.d("TreeViewModel", "waterTree: lastWateredDate = ${activeTree.lastWateredDate}, today = $today")

        if (activeTree.getLocalDate().isBefore(today)) {
            Log.d("TreeViewModel", "waterTree: Watering tree")
            // Update watering history
            activeTree.wateringHistory.add(today.format(dateFormatter))

            // Update tree state
            when (activeTree.treeState) {
                TreeState.Seed -> activeTree.treeState = TreeState.Sprout
                TreeState.Sprout -> activeTree.treeState = TreeState.Sapling
                TreeState.Sapling -> activeTree.treeState = TreeState.Tree
                TreeState.Tree -> {
                    val newTree = TreeData(userId = userId, treeId = UUID.randomUUID().toString())
                    _treeList.value?.add(newTree)
                    _activeTree.value = newTree
                    _treeList.value = _treeList.value
                }
            }
            activeTree.setLocalDate(today)
            _activeTree.value = activeTree
            _isWateredToday.value = true
            saveTree(activeTree)

            // Send success notification (using local notification for simplicity)
            notificationService.showLocalNotification(
                title = "Tưới cây thành công",
                body = "Bạn đã tưới cây hôm nay!"
            )
        } else {
            Log.d("TreeViewModel", "waterTree: Tree already watered today")
        }
    }

    fun setWateringReminder(hour: Int, minute: Int) {
        val activeTree = _activeTree.value ?: return
        activeTree.reminderHour = hour
        activeTree.reminderMinute = minute
        _activeTree.value = activeTree
        saveTree(activeTree)

        // Schedule daily reminder
        notificationService.scheduleDailyReminder(
            hour = hour,
            minute = minute,
            title = "Nhắc nhở tưới cây",
            body = "Đã đến giờ tưới cây của bạn!"
        )
    }
}