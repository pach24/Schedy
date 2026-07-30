package com.schednd.data.repository

import com.schednd.domain.repository.StorageRepository
import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.schednd.R

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : StorageRepository {
    override suspend fun uploadProfilePhoto(userId: String, uri: Uri): String {
        val ref = storage.reference.child("profiles/$userId.jpg")
        val stream = context.contentResolver.openInputStream(uri)
            ?: error(context.getString(R.string.error_image_open))
        stream.use { ref.putStream(it).await() }
        return ref.downloadUrl.await().toString()
    }
}
