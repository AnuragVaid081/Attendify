package com.attendify.shared.repository

import com.attendify.shared.model.NotificationModel
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<NotificationModel>>
    suspend fun markAsRead(notificationId: String)
    suspend fun getUnreadCount(userId: String): Int
}
