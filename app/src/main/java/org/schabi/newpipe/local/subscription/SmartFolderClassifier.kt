package org.schabi.newpipe.local.subscription

/**
 * Conservative, fully offline folder classification. A channel can match more than one folder.
 * Unknown channels are left untouched rather than assigned a misleading category.
 */
object SmartFolderClassifier {
    private val rules = linkedMapOf(
        "Technology & AI" to listOf("technology", "tech ", "artificial intelligence", "machine learning", "programming", "developer", "coding", "software", "computer", "android", "linux", "gadget"),
        "News & Current Affairs" to listOf("news", "current affairs", "politics", "breaking", "journalism"),
        "Malayalam Entertainment" to listOf("malayalam", "mollywood", "kerala cinema", "movie review"),
        "Education" to listOf("education", "science", "history", "learning", "tutorial", "university", "academy"),
        "Kids & Family" to listOf("kids", "children", "nursery", "cartoon", "preschool", "toddler"),
        "Music & Podcasts" to listOf("music", "podcast", "songs", "concert", "audio show")
    )

    fun categories(name: String?, description: String?): Set<String> {
        val nameText = name.orEmpty().lowercase()
        val descriptionText = description.orEmpty().lowercase()
        return rules.filterValues { terms ->
            terms.any { term -> nameText.contains(term) || descriptionText.contains(term) }
        }.keys
    }

    fun folderNames(): Set<String> = rules.keys
}
