package com.example.dacs3.ui.screens.tree

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.R
import com.example.dacs3.data.TreeState
import com.example.dacs3.viewmodels.TreeViewModel
import com.example.dacs3.viewmodels.TreeViewModelFactory
import com.google.firebase.auth.FirebaseAuth

@Composable
fun TreeScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val treeViewModel: TreeViewModel = viewModel(factory = TreeViewModelFactory(context, userId))
    val activeTree by treeViewModel.activeTree.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tree Image
        val treeImage = when (activeTree?.treeState) {
            TreeState.Seed -> R.drawable.seed
            TreeState.Sprout -> R.drawable.sprout
            TreeState.Sapling -> R.drawable.sapling
            TreeState.Tree -> R.drawable.tree // Corrected line
            null -> R.drawable.ground
        }

        Image(
            painter = painterResource(id = treeImage),
            contentDescription = "Tree",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Water Button
        Button(onClick = { treeViewModel.waterTree() }) {
            Text("Water Tree")
        }
    }
}