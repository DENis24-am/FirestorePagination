package com.example.firestorepagination

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object Firebase {

    suspend fun getPaginatedFolders(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        collectionPath: String,
        limit: Long,
        startAfter: DocumentSnapshot? = null
    ): Pair<List<FolderData>, DocumentSnapshot?> {
        val folderList = mutableListOf<FolderData>()

        val collectionRef = firestore.collection(collectionPath)

        val query = if (startAfter != null) {
            collectionRef.orderBy("title").limit(limit).startAfter(startAfter)
        } else {
            collectionRef.orderBy("title").limit(limit)
        }

        return try {
            val querySnapshot = query.get().await()

            for (document in querySnapshot.documents) {
                val title = document.getString("title") ?: "Unknown title"
                val iconPath = document.getString("iconPath") ?: ""

                val iconUrl = if (iconPath.isNotBlank()) {
                    val iconFileRef = storage.reference.child(iconPath)
                    iconFileRef.downloadUrl.await().toString()
                } else {
                    ""
                }

                folderList.add(FolderData(iconUrl, title))
            }

            Pair(folderList, querySnapshot.documents.lastOrNull())
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), null)
        }
    }
}

data class FolderData(
    val iconUrl: String,
    val title: String
)
