package com.example.dacs3.ui.screens.SocialNetwork.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.storage.FirebaseStorage
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import java.util.UUID
//
//// Post data class định nghĩa ngay trong ViewModel
//data class Post(
//    val id: String = "",
//    val caption: String = "",
//    val imageUrl: String? = null,
//    val timestamp: Long = System.currentTimeMillis()
//)
//
////class SocialViewModel : ViewModel() {
////    private val db = FirebaseFirestore.getInstance()
////    private val storage = FirebaseStorage.getInstance()
////
////    private val _posts = MutableStateFlow<List<Post>>(emptyList())
////    val posts: StateFlow<List<Post>> = _posts
////
////    init {
////        loadPosts()
////    }
////
////    private fun loadPosts() {
////        db.collection("posts")
////            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
////            .addSnapshotListener { snapshot, e ->
////                if (e != null || snapshot == null) return@addSnapshotListener
////                val postList = snapshot.documents.mapNotNull { doc ->
////                    doc.toObject(Post::class.java)?.copy(id = doc.id)
////                }
////                _posts.value = postList
////            }
////    }
////
////    fun uploadPost(caption: String, imageUri: String?, onComplete: (Boolean) -> Unit) {
////        viewModelScope.launch {
////            if (imageUri == null) {
////                savePostToFirestore(caption, null, onComplete)
////            } else {
////                val imageRef = storage.reference.child("post_images/${UUID.randomUUID()}.jpg")
////                val uploadTask = imageRef.putFile(android.net.Uri.parse(imageUri))
////                uploadTask.continueWithTask { task ->
////                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
////                    imageRef.downloadUrl
////                }.addOnSuccessListener { uri ->
////                    savePostToFirestore(caption, uri.toString(), onComplete)
////                }.addOnFailureListener {
////                    onComplete(false)
////                }
////            }
////        }
////    }
////
////    private fun savePostToFirestore(caption: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
////        val post = Post(
////            caption = caption,
////            imageUrl = imageUrl,
////            timestamp = System.currentTimeMillis()
////        )
////        db.collection("posts")
////            .add(post)
////            .addOnSuccessListener { onComplete(true) }
////            .addOnFailureListener { onComplete(false) }
////    }
////}
