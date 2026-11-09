package org.schabi.newpipe.local.subscription

import android.content.Context
import androidx.room3.withWriteTransaction
import org.schabi.newpipe.NewPipeDatabase
import org.schabi.newpipe.database.feed.model.FeedGroupEntity
import org.schabi.newpipe.database.feed.model.FeedGroupSubscriptionEntity

/**
 * Adds inferred memberships without deleting or overwriting any existing group assignments.
 * A group with a matching name is reused; no subscription is removed or renamed.
 */
class SmartFolderOrganizer(context: Context) {
    private val database = NewPipeDatabase.getInstance(context)

    suspend fun organize(): Int = database.withWriteTransaction {
        val groupsDao = database.feedGroupDAO()
        val subscriptions = database.subscriptionDAO().getAll().firstOrError().blockingGet()
        val existingGroups = groupsDao.getAll().firstOrError().blockingGet()
        val icons = mapOf(
            "Technology & AI" to FeedGroupIcon.COMPUTER,
            "News & Current Affairs" to FeedGroupIcon.NEWS,
            "Malayalam Entertainment" to FeedGroupIcon.MOVIE,
            "Education" to FeedGroupIcon.EDUCATION,
            "Kids & Family" to FeedGroupIcon.KIDS,
            "Music & Podcasts" to FeedGroupIcon.MUSIC
        )
        val grouped = subscriptions.flatMap { subscription ->
            SmartFolderClassifier.categories(subscription.name, subscription.description)
                .map { category -> category to subscription.uid }
        }.groupBy({ it.first }, { it.second })
        var added = 0
        grouped.forEach { (name, subscriptionIds) ->
            val groupId = existingGroups.firstOrNull { it.name.equals(name, ignoreCase = true) }?.uid
                ?: groupsDao.insert(FeedGroupEntity(0, name, icons.getValue(name)))
            val current = groupsDao.getSubscriptionIdsFor(groupId).firstOrError().blockingGet().toSet()
            val newIds = subscriptionIds.distinct().filterNot { it in current }
            if (newIds.isNotEmpty()) {
                groupsDao.insertSubscriptionsToGroup(
                    newIds.map { FeedGroupSubscriptionEntity(groupId, it) }
                )
                added += newIds.size
            }
        }
        added
    }
}
