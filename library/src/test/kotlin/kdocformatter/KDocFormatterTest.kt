package kdocformatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KDocFormatterTest {
    private fun checkFormatter(
        source: String,
        options: KDocFormattingOptions,
        expected: String,
        indent: String = "    "
    ) {
        val reformatted = reformatComment(source, options, indent)
        // Because .trimIndent() will remove it:
        val indentedExpected = expected.split("\n").joinToString("\n") { indent + it }
        assertEquals(indentedExpected, reformatted)
    }

    private fun reformatComment(source: String, options: KDocFormattingOptions, indent: String = "    "): String {
        return indent + KDocFormatter(options).reformatComment(source.trim(), indent)
    }

    @Test
    fun test1() {
        checkFormatter(
            """
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
            """.trimIndent(),
            KDocFormattingOptions(72),
            """
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
            """.trimIndent()
        )
    }

    @Test
    fun test2() {
        checkFormatter(
            """
            /** Returns whether lint should check all warnings,
             * including those off by default */
            """.trimIndent(),
            KDocFormattingOptions(72),
            """
            /**
             * Returns whether lint should check all warnings, including those
             * off by default
             */
            """.trimIndent(),
            indent = "    "
        )
    }

    @Test
    fun testHeader() {
        val source =
            """
            /**
             * Information about a request to run lint.
             *
             * **NOTE: This is not a public or final API; if you rely on this be prepared
             * to adjust your code for the next tools release.**
             */                
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            """
            /**
             * Information about a request to run lint.
             *
             * **NOTE: This is not a public or final API; if you rely on this be
             * prepared to adjust your code for the next tools release.**
             */
            """.trimIndent()
        )

        checkFormatter(
            source,
            KDocFormattingOptions(40),
            """
            /**
             * Information about a request to run
             * lint.
             *
             * **NOTE: This is not a public or final
             * API; if you rely on this be prepared
             * to adjust your code for the next
             * tools release.**
             */
            """.trimIndent(),
            indent = ""
        )

        checkFormatter(
            source,
            KDocFormattingOptions(100),
            """
            /**
             * Information about a request to run lint.
             *
             * **NOTE: This is not a public or final API; if you rely on this be prepared to adjust your code
             * for the next tools release.**
             */
            """.trimIndent(),
            indent = ""
        )
    }

    @Test
    fun testSingle() {
        val source =
            """
            /**
             * The lint client requesting the lint check
             *
             * @return the client, never null
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            """
            /**
             * The lint client requesting the lint check
             *
             * @return the client, never null
             */
            """.trimIndent()
        )
    }

    @Test
    fun testJavadocParams() {
        val source =
            """
            /**
             * Sets the scope to use; lint checks which require a wider scope set
             * will be ignored
             *
             * @param scope the scope
             *
             * @return this, for constructor chaining
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            """
            /**
             * Sets the scope to use; lint checks which require a wider scope
             * set will be ignored
             *
             * @param scope the scope
             * @return this, for constructor chaining
             */
            """.trimIndent()
        )
    }

    @Test
    fun testLineWidth1() {
        // Perform in KDocFileFormatter test too to make sure we properly account for indent!
        val source = """
            /**
             * 89 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789
             *
             *   10        20        30        40        50        60        70        80
             */
        """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            """
            /**
             * 89 123456789 123456789 123456789 123456789 123456789 123456789
             * 123456789 123456789
             *
             * 0 20 30 40 50 60 70 80
             */
            """.trimIndent()
        )

        checkFormatter(
            source,
            KDocFormattingOptions(40),
            """
            /**
             * 89 123456789 123456789 123456789
             * 123456789 123456789 123456789
             * 123456789 123456789
             *
             * 0 20 30 40 50 60 70 80
             */
            """.trimIndent()
        )
    }

    @Test
    fun testBlockTagsNoSeparators() {

        checkFormatter(
            """
             /**
              * Marks the given warning as "ignored".
              *
             * @param context The scanning context
             * @param issue the issue to be ignored
             * @param location The location to ignore the warning at, if any
             * @param message The message for the warning
             */
            """.trimIndent(),
            KDocFormattingOptions(72),
            """
             /**
              * Marks the given warning as "ignored".
              *
              * @param context The scanning context
              * @param issue the issue to be ignored
              * @param location The location to ignore the warning at, if any
              * @param message The message for the warning
              */
            """.trimIndent()
        )
    }

    @Test
    fun testBlockTagsHangingIndents() {
        checkFormatter(
            """
            /**
             * Creates a list of class entries from the given class path and specific set of files within
             * it.
             *
             * @param client the client to report errors to and to use to read files
             * @param classFiles the specific set of class files to look for
             * @param classFolders the list of class folders to look in (to determine the package root)
             * @param sort if true, sort the results
             * @return the list of class entries, never null.
             */
            """.trimIndent(),
            KDocFormattingOptions(40),
            """
            /**
             * Creates a list of class entries
             * from the given class path and
             * specific set of files within it.
             *
             * @param client the client to
             *     report errors to and to use
             *     to read files
             * @param classFiles the specific
             *     set of class files to look
             *     for
             * @param classFolders the list of
             *     class folders to look in (to
             *     determine the package root)
             * @param sort if true, sort the
             *     results
             * @return the list of class
             *     entries, never null.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testSingleLine() {
        val source =
            """
             /**
              * This could all fit on one line
              */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(72),
            """
             /** This could all fit on one line */
            """.trimIndent()
        )
        checkFormatter(
            source, KDocFormattingOptions(72, collapseSingleLine = false),
            """
             /**
              * This could all fit on one line
              */
            """.trimIndent()
        )
    }

    @Test
    fun testPreformattedText() {
        val source =
            """
            /**
             * Code sample:
             *
             *     val s = "hello, and   this is code so should not be line broken at all, it should stay on one line";
             *     println(s);
             *
             * This is not preformatted and can be combined into multiple sentences again.
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * Code sample:
             *
             *     val s = "hello, and   this is code so should not be line broken at all, it should stay on one line";
             *     println(s);
             *
             * This is not preformatted and can
             * be combined into multiple
             * sentences again.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testPreformattedText2() {
        val source =
            """
            /**
             * Code sample:
             * ```kotlin
             * val s = "hello, and this is code so should not be line broken at all, it should stay on one line";
             * println(s);
             * ```
             *
             * This is not preformatted and can be combined into multiple sentences again.
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * Code sample:
             *
             * ```kotlin
             * val s = "hello, and this is code so should not be line broken at all, it should stay on one line";
             * println(s);
             * ```
             *
             * This is not preformatted and can
             * be combined into multiple
             * sentences again.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testFormattingList() {
        val source =
            """
            /**
             * 1. This is a numbered list.
             * 2. This is another item. We should be wrapping extra text under the same item.
             * 3. This is the third item.
             *
             * Unordered list:
             * * First
             * * Second
             * * Third
             *
             * Other alternatives:
             * - First
             * - Second
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * 1. This is a numbered list.
             * 2. This is another item. We
             *    should be wrapping extra text
             *    under the same item.
             * 3. This is the third item.
             *
             * Unordered list:
             * * First
             * * Second
             * * Third
             *
             * Other alternatives:
             * - First
             * - Second
             */
            """.trimIndent()
        )
    }
}
