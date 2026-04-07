package com.attendify.shared.repository.impl

import com.attendify.shared.model.NotificationModel
import com.attendify.shared.repository.NotificationRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseNotificationRepository : NotificationRepository {

    private val db = Firebase.firestore
    private val notifications = db.collection("notifications")

    override fun observeNotifications(userId: String): Flow<List<NotificationModel>> =
        notifications
            .where { "targetStudentIds" contains userId }
            .orderBy("timestamp", Direction.DESCENDING)
            .snapshots.map { snapshot ->
                snapshot.documents.map { it.data<NotificationModel>() }
            }

    override suspend fun markAsRead(notificationId: String) {
        notifications.document(notificationId).update("isRead" to true)
    }

    override suspend fun getUnreadCount(userId: String): Int =
        notifications
            .where { "targetStudentIds" contains userId }
            .where { "isRead" equalTo false }
            .get()
            .documents.size
}
