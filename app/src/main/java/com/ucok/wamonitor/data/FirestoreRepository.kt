package com.ucok.wamonitor.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirestoreRepository {

    private const val COLLECTION = "captured_messages"
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Tambah callback onSuccess dan onFailure untuk debug
    fun uploadMessage(
        message: CapturedMessage,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val doc = hashMapOf(
            "senderTitle" to message.senderTitle,
            "messageText" to message.messageText,
            "timestamp" to message.timestamp
        )
        db.collection(COLLECTION)
            .add(doc)
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    fun listenMessages(onUpdate: (List<CapturedMessage>) -> Unit) {
        db.collection(COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { docSnap ->
                    val senderTitle = docSnap.getString("senderTitle") ?: return@mapNotNull null
                    val messageText = docSnap.getString("messageText") ?: return@mapNotNull null
                    val timestamp = docSnap.getLong("timestamp") ?: 0L
                    CapturedMessage(
                        id = docSnap.id.hashCode().toLong(),
                        senderTitle = senderTitle,
                        messageText = messageText,
                        timestamp = timestamp
                    )
                }
                onUpdate(messages)
            }
    }

    fun clearAll(onComplete: () -> Unit = {}) {
        db.collection(COLLECTION).get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().addOnCompleteListener { onComplete() }
        }
    }
}
