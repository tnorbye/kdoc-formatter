package kdocformatter.cli

import kdocformatter.KDocFormattingOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

// The formatter is mostly tested by KDocFormatterTest. This tests the part
// where we (1) find the comments and format them, and (2) process the code
// outside the comments and stitch it all together with the right indentation
// etc.
class KDocFileFormatterTest {
    private fun reformatFile(source: String, options: KDocFormattingOptions): String {
        val fileOptions = KDocFileFormattingOptions()
        fileOptions.formattingOptions = options
        val formatter = KDocFileFormatter(fileOptions)
        val reformatted = formatter.reformatFile(null, source.trim())
        // Make sure that formatting is stable -- format again and make sure it's the same
        assertEquals(reformatted, formatter.reformatFile(null, reformatted.trim()))
        return reformatted
    }

    // TODO: Test driver --reading filenames from @-file etc

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

                private val string = ""\"
                /** This is NOT
                     a comment to reformat, it's
                   in a string   */
                ""\"
            }
            """.trimIndent()

        val reformatted = reformatFile(source, KDocFormattingOptions(72))
        assertEquals(
            """
            class Test {
                /**
                 * Returns whether lint should check all warnings, including
                 * those off by default, or null if not configured in
                 * this configuration. This is a really really really
                 * long sentence which needs to be broken up. And
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

                private val string = ""\"
                /** This is NOT
                     a comment to reformat, it's
                   in a string   */
                ""\"
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
             * This should fit on a
             * single And this should
             * also fit!! And this should
             * not!!!!!!!!!
             */
            """.trimIndent(),
            reformatted
        )
    }

    @Test
    fun testGitRanges() {
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

        val diff = """
            diff --git a/README.md b/README.md
            index c26815b..30a8dbb 100644
            --- a/README.md
            +++ b/README.md
            @@ -31,25 +31,29 @@ ${'$'} kdoc-formatter
             Usage: kdoc-formatter [options] file(s)
             diff --git Test.kt Test.kt
            index d66825c..7a324fb 100644
            --- Test.kt
            +++ Test.kt
            @@ -15 +15 @@ class Test {
            -     * including those off by default */  // additional
            +     * modified including those off by default */  // additional            
            diff --git a/README.md b/README.md
            index c26815b..30a8dbb 100644
            --- a/README.md
            +++ b/README.md
            @@ -31,25 +31,29 @@ ${'$'} kdoc-formatter
             Usage: kdoc-formatter [options] file(s)
        """.trimIndent()

        val fileOptions = KDocFileFormattingOptions()
        val file = File("Test.kt")
        fileOptions.filter = GitRangeFilter.create(null, diff)
        val reformatted = KDocFileFormatter(fileOptions).reformatFile(file, source.trim())

        // Only the second comment should be formatted:
        assertEquals(
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
}
