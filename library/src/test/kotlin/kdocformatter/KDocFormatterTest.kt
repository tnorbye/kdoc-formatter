package kdocformatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class KDocFormatterTest {
    private fun checkFormatter(
        source: String,
        options: KDocFormattingOptions,
        expected: String,
        indent: String = "    ",
        verify: Boolean = false
    ) {
        val reformatted = reformatComment(source, options, indent, verify)
        // Because .trimIndent() will remove it:
        val indentedExpected = expected.split("\n").joinToString("\n") { indent + it }
        assertEquals(indentedExpected, reformatted)
    }

    private fun reformatComment(
        source: String,
        options: KDocFormattingOptions,
        indent: String = "    ",
        verify: Boolean = true
    ): String {
        val formatter = KDocFormatter(options)
        val formatted = formatter.reformatComment(source.trim(), indent)
        // Make sure that formatting is stable -- format again and make sure it's the same
        if (verify) {
            assertEquals(formatted, formatter.reformatComment(formatted.trim(), indent))
        }
        return indent + formatted
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
             * Returns whether lint should check all warnings, including
             * those off by default, or null if not configured in
             * this configuration. This is a really really really
             * long sentence which needs to be broken up. And
             * ThisIsALongSentenceWhichCannotBeBrokenUpAndMustBeIncludedAsAWholeWithoutNewlinesInTheMiddle.
             *
             * This is a separate section which should be flowed together with
             * the first one. *bold* should not be removed even at beginning.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testWithOffset() {
        val source =
            """
            /** Returns whether lint should check all warnings,
             * including those off by default */
            """.trimIndent()
        val reformatted =
            """
            /**
             * Returns whether lint should check all warnings, including those
             * off by default
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            reformatted,
            indent = "    "
        )
        val initialOffset = source.indexOf("default")
        val newOffset = findSamePosition(source, initialOffset, reformatted)
        assertNotEquals(initialOffset, newOffset)
        assertEquals("default", reformatted.substring(newOffset, newOffset + "default".length))
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
        val source =
            """
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
             * 10 20 30 40 50 60 70 80
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
             * 10 20 30 40 50 60 70 80
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
        val options = KDocFormattingOptions(40)
        options.hangingIndent = 6
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
            options,
            """
            /**
             * Creates a list of class entries
             * from the given class path and
             * specific set of files within it.
             *
             * @param client the client to
             *       report errors to and
             *       to use to read files
             * @param classFiles the specific
             *       set of class files to look
             *       for
             * @param classFolders the list of
             *       class folders to look
             *       in (to determine
             *       the package root)
             * @param sort if true, sort the
             *       results
             * @return the list of class
             *       entries, never null.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testBlockTagsHangingIndents2() {
        checkFormatter(
            """
            /**
             * @param client the client to
             *     report errors to and to use to
             *     read files
             */
            """.trimIndent(),
            KDocFormattingOptions(40),
            """
            /**
             * @param client the client to
             *     report errors to and
             *     to use to read files
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
        val options = KDocFormattingOptions(72)
        options.collapseSingleLine = false
        checkFormatter(
            source, options,
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
             * This is not preformatted and
             * can be combined into multiple
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
             * This is not preformatted and
             * can be combined into multiple
             * sentences again.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testPreformattedText3() {
        val source =
            """
            /**
             * Code sample:
             * <PRE>
             *     val s = "hello, and   this is code so should not be line broken at all, it should stay on one line";
             *     println(s);
             * </pre>
             * This is not preformatted and can be combined into multiple sentences again.
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * Code sample:
             *
             * <PRE>
             *     val s = "hello, and   this is code so should not be line broken at all, it should stay on one line";
             *     println(s);
             * </pre>
             *
             * This is not preformatted and
             * can be combined into multiple
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
             *    should be wrapping extra
             *    text under the same item.
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

    @Test
    fun testList1() {
        val source =
            """
            /**
             *  * pre.errorlines: General > Text > Default Text
             *  * .prefix: XML > Namespace Prefix
             *  * .attribute: XML > Attribute name
             *  * .value: XML > Attribute value
             *  * .tag: XML > Tag name
             *  * .lineno: For color, General > Code > Line number, Foreground, and for background-color,
             * Editor > Gutter background
             *  * .error: General > Errors and Warnings > Error
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * * pre.errorlines: General > Text
             *   > Default Text
             * * .prefix: XML > Namespace Prefix
             * * .attribute: XML > Attribute
             *   name
             * * .value: XML > Attribute value
             * * .tag: XML > Tag name
             * * .lineno: For color, General >
             *   Code > Line number, Foreground,
             *   and for background-color,
             *   Editor > Gutter background
             * * .error: General > Errors and
             *   Warnings > Error
             */
            """.trimIndent()
        )
    }

    @Test
    fun testIndentedList() {
        val source =
            """
            /**
            * Basic usage:
            *   1. Create a configuration via [UastEnvironment.Configuration.create] and mutate it as needed.
            *   2. Create a project environment via [UastEnvironment.create].
            *      You can create multiple environments in the same process (one for each "module").
            *   3. Call [analyzeFiles] to initialize PSI machinery and precompute resolve information.
            */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * Basic usage:
             * 1. Create a configuration via
             *    [UastEnvironment.Configuration.create]
             *    and mutate it as needed.
             * 2. Create a project environment
             *    via [UastEnvironment.create].
             *    You can create multiple
             *    environments in the
             *    same process (one
             *    for each "module").
             * 3. Call [analyzeFiles] to
             *    initialize PSI machinery
             *    and precompute
             *    resolve information.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testDocTags() {
        val source =
            """
            /**
             * @param configuration the configuration to look up which issues are
             * enabled etc from
             * @param platforms the platforms applying to this analysis
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(40),
            """
            /**
             * @param configuration the
             *     configuration to
             *     look up which issues
             *     are enabled etc from
             * @param platforms the platforms
             *     applying to this analysis
             */
            """.trimIndent()
        )
    }

    @Test
    fun testAtInMiddle() {
        val source =
            """
            /**
             * If non-null, this issue can **only** be suppressed with one of the
             * given annotations: not with @Suppress, not with @SuppressLint, not
             * with lint.xml, not with lintOptions{} and not with baselines.
             */
            """.trimIndent()
        checkFormatter(
            source, KDocFormattingOptions(72),
            """
            /**
             * If non-null, this issue can **only** be suppressed with
             * one of the given annotations: not with @Suppress, not with
             * @SuppressLint, not with lint.xml, not with lintOptions{} and not
             * with baselines.
             */
            """.trimIndent(),
            // After reflowing @SuppressLint ends up on at the beginning of a line
            // which is then interpreted as a doc tag
            verify = true

        )
    }

    @Test
    fun testMaxCommentWidth() {
        checkFormatter(
            """
            /**
            * Returns whether lint should check all warnings,
             * including those off by default, or null if
             *not configured in this configuration. This is a really really really long sentence which needs to be broken up.
             * This is a separate section
             * which should be flowed together with the first one.
             * *bold* should not be removed even at beginning.
             */
            """.trimIndent(),
            KDocFormattingOptions(maxLineWidth = 100, maxCommentWidth = 30),
            """
            /**
             * Returns whether lint should
             * check all warnings, including
             * those off by default, or
             * null if not configured in
             * this configuration. This is
             * a really really really long
             * sentence which needs to be
             * broken up. This is a separate
             * section which should be flowed
             * together with the first one.
             * *bold* should not be removed
             * even at beginning.
             */
            """.trimIndent()
        )
    }
}
