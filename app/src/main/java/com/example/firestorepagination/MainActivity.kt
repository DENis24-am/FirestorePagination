package com.example.firestorepagination

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.firestorepagination.ui.theme.FirestorePaginationTheme
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        setContent {
            var list by remember { mutableStateOf(Pair(emptyList<FolderData>(), null as DocumentSnapshot?)) }
            var isLoading by remember { mutableStateOf(true) }
            var isLoadingNextPage by remember { mutableStateOf(false) }

            val scope = rememberCoroutineScope()

            FirestorePaginationTheme {
                LaunchedEffect(Unit) {
                    Log.d("LOG_TAG", "Starting initial data load")
                    list = Firebase.getPaginatedFolders(firestore, storage, "folders", 20)
                    isLoading = false
                }

                if (isLoading || isLoadingNextPage) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(top = 32.dp)
                                .align(Alignment.TopCenter)
                        )
                    }
                }

                if (!isLoading) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                        itemsIndexed(list.first) { index, item ->
                            key(item.title + index) {
                                FolderItem(item)

                                if (index == list.first.lastIndex && list.second != null && !isLoadingNextPage) {
                                    LaunchedEffect(Unit) {
                                        scope.launch(Dispatchers.IO) {
                                            isLoadingNextPage = true
                                            val nextPage = Firebase.getPaginatedFolders(
                                                firestore,
                                                storage,
                                                "folders",
                                                10,
                                                list.second
                                            )
                                            list = Pair(
                                                list.first + nextPage.first,
                                                nextPage.second
                                            )
                                            isLoadingNextPage = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItem(data: FolderData) {
    Surface(
        color = Color(0xFFE5CEAB),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 6.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(180.dp)
        ) {
            Text(
                text = data.title,
                modifier = Modifier.padding(8.dp)
            )
            SubcomposeAsyncImage(
                model = data.iconUrl,
                loading = {
                    CircularProgressIndicator()
                },
                contentDescription = null
            )
        }
    }
}
