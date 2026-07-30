package com.schednd.data.repository

import com.schednd.domain.repository.NoteRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NoteRepository {
    private val eventsCollection = firestore.collection("events")

    private fun notesRef(code: String) =
        eventsCollection.document(code).collection("notes")

    override fun observeNotes(code: String): Flow<List<Note>> = callbackFlow {
        val listener = notesRef(code)
            .orderBy("pinned", Query.Direction.DESCENDING)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    Note(
                        id = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        tag = NoteTag.fromString(doc.getString("tag")),
                        pinned = doc.getBoolean("pinned") ?: false,
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                        updatedAt = doc.getTimestamp("updatedAt") ?: Timestamp.now()
                    )
                } ?: emptyList()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createNote(
        code: String,
        authorId: String,
        authorName: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ): String {
        val data = hashMapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "title" to title,
            "body" to body,
            "tag" to tag.name,
            "pinned" to pinned,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val ref = notesRef(code).add(data).await()
        return ref.id
    }

    override suspend fun updateNote(
        code: String,
        noteId: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ) {
        val data = hashMapOf<String, Any>(
            "title" to title,
            "body" to body,
            "tag" to tag.name,
            "pinned" to pinned,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        notesRef(code).document(noteId).update(data).await()
    }

    override suspend fun deleteNote(code: String, noteId: String) {
        notesRef(code).document(noteId).delete().await()
    }

    override suspend fun togglePin(code: String, noteId: String, pinned: Boolean) {
        notesRef(code).document(noteId).update(
            mapOf(
                "pinned" to pinned,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
