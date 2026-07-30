package com.schednd.domain.model

import com.google.firebase.Timestamp

data class Note(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val body: String = "",
    val tag: NoteTag = NoteTag.OTROS,
    val pinned: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
