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
        assertTrue("Malayalam Entertainment" in categories)
        assertTrue("News & Current Affairs" in categories)
    }

    @Test
    fun `classification ignores letter case`() {
        assertTrue("Kids & Family" in SmartFolderClassifier.categories("KIDS CARTOON", null))
    }
}
