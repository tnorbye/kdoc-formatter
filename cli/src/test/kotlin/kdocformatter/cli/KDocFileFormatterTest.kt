package kdocformatter.cli

import kdocformatter.KDocFormattingOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The formatter is mostly tested by KDocFormatterTest. This tests the part
// where we (1) find the comments and format them, and (2) process the code
// outside the comments and stitch it all together with the right indentation
// etc.
class KDocFileFormatterTest {
    private fun reformatFile(source: String, options: KDocFormattingOptions,): String {
        return KDocFileFormatter(options).reformatFile(source.trim())
    }

    @Test
    fun test() {
        val source =
            """
            class Test {
                /**
                * Returns whether lint should check all warnings,
                 * including those off by default, or null if
                 *not configured in this configuration. This is a really really really long sentence which needs to be broken up.
                 * And ThisIsALongSentenceWhichCannotBeBrokenUpAndMustBeIncludedAsAWholeWithoutNewlinesInTheMiddle.
                 *
                 * This is a separate section
                 * which should be flowed together with the first one.
             * *bold* should not be removed even at beginning.
                 */
                private var checkAllWarnings: Boolean? = null

                /** Returns whether lint should check all warnings,
                 * including those off by default */  // additional
                private var ignoreAll: Boolean? = null
            }
            """.trimIndent()

        val reformatted = reformatFile(source, KDocFormattingOptions(72))
        assertEquals(
            """
            class Test {
                /**
                 * Returns whether lint should check all warnings, including those
                 * off by default, or null if not configured in this configuration.
                 * This is a really really really long sentence which needs to be
                 * broken up. And
                 * ThisIsALongSentenceWhichCannotBeBrokenUpAndMustBeIncludedAsAWholeWithoutNewlinesInTheMiddle.
                 *
                 * This is a separate section which should be flowed together with
                 * the first one. *bold* should not be removed even at beginning.
                 */
                private var checkAllWarnings: Boolean? = null
            
                /**
                 * Returns whether lint should check all warnings, including those
                 * off by default
                 */  // additional
                private var ignoreAll: Boolean? = null
            }
            """.trimIndent(),
            reformatted
        )
    }

    @Test
    fun testLineWidth() {
        // Perform in KDocFileFormatter test too to make sure we properly account for indent!
        val source = """
            //3456789012345678901234567890 <- 30
            /**
             * This should fit on a single
             * And this should also fit!! 
             * And this should not!!!!!!!!! 
             */
        """.trimIndent()
        val reformatted = reformatFile(source, KDocFormattingOptions(30))
        assertEquals(
            """
            //3456789012345678901234567890 <- 30
            /**
             * This should fit on a single
             * And this should also fit!!
             * And this should
             * not!!!!!!!!!
             */
            """.trimIndent(),
            reformatted
        )
    }
}
