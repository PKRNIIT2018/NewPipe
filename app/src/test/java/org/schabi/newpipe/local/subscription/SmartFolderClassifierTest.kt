package org.schabi.newpipe.local.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFolderClassifierTest {
    @Test
    fun `technology channel is classified`() {
        assertTrue("Technology & AI" in SmartFolderClassifier.categories("AI Programming", null))
    }

    @Test
    fun `description can classify channels`() {
        assertTrue(
            "Education" in SmartFolderClassifier.categories(
                "Example Channel",
                "Science tutorials and history"
            )
        )
    }

    @Test
    fun `unknown channels are not assigned to arbitrary folders`() {
        assertEquals(emptySet<String>(), SmartFolderClassifier.categories("Example Channel", null))
    }

    @Test
    fun `multiple categories are supported`() {
        val categories = SmartFolderClassifier.categories("Malayalam Movie Review", "News")
        assertEquals(setOf("News & Current Affairs"), categories)
    }

    @Test
    fun `description takes precedence over channel name`() {
        val categories = SmartFolderClassifier.categories(
            "Music Podcast",
            "Science tutorials and university education"
        )
        assertEquals(setOf("Education"), categories)
    }

    @Test
    fun `ambiguous descriptions remain unclassified`() {
        val categories = SmartFolderClassifier.categories("Movie Channel", "Music and science")
        assertEquals(emptySet<String>(), categories)
    }

    @Test
    fun `classification ignores letter case`()  {
        assertTrue("Kids & Family" in SmartFolderClassifier.categories("KIDS CARTOON", null))
    }
}
