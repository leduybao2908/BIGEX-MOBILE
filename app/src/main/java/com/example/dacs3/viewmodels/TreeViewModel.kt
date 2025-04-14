package com.example.dacs3.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dacs3.data.TreeData
import com.example.dacs3.data.TreeState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TreeViewModel(private val context: Context, private val userId: String) : ViewModel() {
    private val database = FirebaseDatabase.getInstance()

    private val _treeList = MutableLiveData<MutableList<TreeData>>()
    val treeList: LiveData<MutableList<TreeData>> = _treeList

    private val _activeTree = MutableLiveData<TreeData>()
    val activeTree: LiveData<TreeData> = _activeTree

    init {
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
                    tree?.let {
                        treeList.add(it)
                    }
                }
                _treeList.value = treeList
                _activeTree.value = treeList.lastOrNull() ?: TreeData(userId = userId, treeId = UUID.randomUUID().toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TreeViewModel", "Failed to load tree list", error.toException())
            }
        })
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

    fun waterTree() {
        Log.d("TreeViewModel", "waterTree() called")
        val activeTree = _activeTree.value ?: return
        val today = LocalDate.now()
        Log.d("TreeViewModel", "waterTree: lastWateredDate = ${activeTree.lastWateredDate}, today = $today")

        if (activeTree.getLocalDate().isBefore(today)) {
            Log.d("TreeViewModel", "waterTree: Watering tree")
            when (activeTree.treeState) {
                TreeState.Seed -> {
                    activeTree.treeState = TreeState.Sprout
                    Log.d("TreeViewModel", "waterTree: TreeState changed to Sprout")
                }
                TreeState.Sprout -> {
                    activeTree.treeState = TreeState.Sapling
                    Log.d("TreeViewModel", "waterTree: TreeState changed to Sapling")
                }
                TreeState.Sapling -> {
                    activeTree.treeState = TreeState.Tree
                    Log.d("TreeViewModel", "waterTree: TreeState changed to Tree")
                }
                TreeState.Tree -> {
                    val newTree = TreeData(userId = userId, treeId = UUID.randomUUID().toString())
                    _treeList.value?.add(newTree)
                    _activeTree.value = newTree
                    Log.d("TreeViewModel", "waterTree: New tree created")
                    _treeList.value = _treeList.value
                }
            }
            activeTree.setLocalDate(today)
            Log.d("TreeViewModel", "waterTree: lastWateredDate updated to $today")
            _activeTree.value = activeTree
            saveTree(activeTree)
        } else {
            Log.d("TreeViewModel", "waterTree: Tree already watered today")
        }
        Log.d("TreeViewModel", "waterTree: treeState = ${activeTree.treeState}, lastWateredDate = ${activeTree.lastWateredDate}")
    }
}