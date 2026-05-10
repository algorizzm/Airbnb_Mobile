package com.verdant.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class StorageService {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadHikeImage(hikeId: String, imageUri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("hike_images/$hikeId.jpg")
        ref.putFile(imageUri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun uploadAvatar(uid: String, imageUri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("avatars/$uid.jpg")
        ref.putFile(imageUri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun uploadBanner(uid: String, imageUri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("banners/$uid.jpg")
        ref.putFile(imageUri).await()
        ref.downloadUrl.await().toString()
    }
}
