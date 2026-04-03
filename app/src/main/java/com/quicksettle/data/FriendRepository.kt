package com.quicksettle.data

import com.quicksettle.data.local.FriendDao
import com.quicksettle.domain.model.Friend
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that mediates between the domain layer and the Room [FriendDao].
 *
 * All reactive queries return [Flow] so that the UI layer stays reactive to database
 * changes without requiring explicit refresh calls.
 */
@Singleton
class FriendRepository @Inject constructor(private val friendDao: FriendDao) {

    /** Emits the full friend list, ordered by most-recently-used first. */
    fun getAllFriends(): Flow<List<Friend>> = friendDao.getAllFriends()

    /** Emits friends whose names contain [query] (case-insensitive via SQLite LIKE). */
    fun searchFriends(query: String): Flow<List<Friend>> = friendDao.searchFriends(query)

    /**
     * Inserts a new [Friend] with the given [name] and optional [upiId].
     * A new UUID is generated automatically.
     */
    suspend fun addFriend(name: String, upiId: String? = null) {
        val friend = Friend(
            id = UUID.randomUUID().toString(),
            name = name,
            upiId = upiId,
            lastUsed = System.currentTimeMillis(),
        )
        friendDao.insertFriend(friend)
    }

    /** Deletes [friend] from the database. */
    suspend fun removeFriend(friend: Friend) {
        friendDao.deleteFriend(friend)
    }

    /**
     * Stamps [friendId]'s [Friend.lastUsed] with the current time so that the
     * most-recently-used ordering stays accurate as friends are selected for splits.
     */
    suspend fun updateLastUsed(friendId: String) {
        friendDao.updateLastUsed(id = friendId, timestamp = System.currentTimeMillis())
    }
}
