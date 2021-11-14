package kdocformatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KDocFormatterTest {
    private fun checkFormatter(
        source: String,
        options: KDocFormattingOptions,
        expected: String,
        indent: String = "    ",
        verify: Boolean = true
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
    fun testWordBreaking() {
        // Without special handling, the "-" in the below would
        // be placed at the beginning of line 2, which then
        // implies a list item.
        val source =
            """
            /** Returns whether lint should check all warnings,
             * including aaaaaa - off by default */
            """.trimIndent()
        val reformatted =
            """
            /**
             * Returns whether lint should check all warnings, including
             * aaaaaa - off by default
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
             *       set of class files to look for
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
        // Also tests punctuation feature.
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
        options.addPunctuation = true
        checkFormatter(
            source, options,
            """
             /**
              * This could all fit on one line.
              */
            """.trimIndent()
        )
    }

    @Test
    fun testPunctuationWithLabelLink() {
        val source =
            """
             /** Default implementation of [MyInterface] */
            """.trimIndent()

        val options = KDocFormattingOptions(72)
        options.addPunctuation = true
        checkFormatter(
            source, options,
            """
             /** Default implementation of [MyInterface]. */
            """.trimIndent()
        )
    }

    @Test
    fun testWrapingOfLinkText() {
        val source =
            """
             /**
              * Sometimes the text of a link can have spaces, like [this link's text](https://example.com).
              * The following text should wrap like usual.
              */
            """.trimIndent()

        val options = KDocFormattingOptions(72)
        checkFormatter(
            source, options,
            """
            /**
             * Sometimes the text of a link can have spaces, like
             * [this link's text](https://example.com). The following text
             * should wrap like usual.
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
             * <PRE>
             *     val s = "hello, and   this is code so should not be line broken at all, it should stay on one line";
             *     println(s);
             * </pre>
             * This is not preformatted and
             * can be combined into multiple
             * sentences again.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testPreformattedTextWithBlankLines() {
        val source =
            """
            /**
             * Code sample:
             * ```kotlin
             * val s = "hello, and this is code so should not be line broken at all, it should stay on one line";
             *
             * println(s);
             * ```
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
             *
             * println(s);
             * ```
             */
            """.trimIndent()
        )
    }

    @Test
    fun testPreformattedTextWithBlankLinesAndTrailingSpaces() {
        val source =
            """
            /**
             * Code sample:
             * ```kotlin
             * val s = "hello, and this is code so should not be line broken at all, it should stay on one line";
             * 
             * println(s);
             * ```
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
             *
             * println(s);
             * ```
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

    @Test
    fun testHorizontalRuler() {
        checkFormatter(
            """
            /**
            * This is a header. Should appear alone.
            * --------------------------------------
            * This should not be on the same line as the header.
             */
            """.trimIndent(),
            KDocFormattingOptions(maxLineWidth = 100, maxCommentWidth = 30),
            """
            /**
             * This is a header. Should
             * appear alone.
             * --------------------------------------
             * This should not be on the same
             * line as the header.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testQuoteOnlyOnFirstLine() {
        checkFormatter(
            """
            /**
            * More:
            * > This whole paragraph should be treated as a block quote.
            * This whole paragraph should be treated as a block quote.
            * This whole paragraph should be treated as a block quote.
            * This whole paragraph should be treated as a block quote.
                 */
            """.trimIndent(),
            KDocFormattingOptions(maxLineWidth = 100, maxCommentWidth = 30),
            """
            /**
             * More:
             * > This whole paragraph should
             * > be treated as a block quote.
             * > This whole paragraph should
             * > be treated as a block quote.
             * > This whole paragraph should
             * > be treated as a block quote.
             * > This whole paragraph should
             * > be treated as a block quote.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testAsciiArt() {
        // Comment from
        // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:build-system/integration-test/application/src/test/java/com/android/build/gradle/integration/bundle/DynamicFeatureAndroidTestBuildTest.kt
        checkFormatter(
            """
            /**
             *       Base <------------ Middle DF <------------- DF <--------- Android Test DF
             *      /    \              /       \                |               /        \   \
             *     v      v            v         v               v              v          \   \
             *  appLib  sharedLib   midLib   sharedMidLib    featureLib    testFeatureLib   \   \
             *              ^                      ^_______________________________________/   /
             *              |________________________________________________________________/
             *
             *  DF has a feature-on-feature dep on Middle DF, both depend on Base, Android Test DF is an
             *  android test variant for DF.
             *
             *  Base depends on appLib and sharedLib.
             *  Middle DF depends on midLib and sharedMidLib.
             *  DF depends on featureLib.
             *  DF also has an android test dependency on testFeatureLib, shared and sharedMidLib.
             */
            """.trimIndent(),
            KDocFormattingOptions(maxLineWidth = 100, maxCommentWidth = 30),
            """
            /**
             *       Base <------------ Middle DF <------------- DF <--------- Android Test DF
             *      /    \              /       \                |               /        \   \
             *     v      v            v         v               v              v          \   \
             *  appLib  sharedLib   midLib   sharedMidLib    featureLib    testFeatureLib   \   \
             *              ^                      ^_______________________________________/   /
             *              |________________________________________________________________/
             *
             * DF has a feature-on-feature
             * dep on Middle DF, both depend
             * on Base, Android Test DF is an
             * android test variant for DF.
             *
             * Base depends on appLib and
             * sharedLib. Middle DF depends
             * on midLib and sharedMidLib. DF
             * depends on featureLib. DF also
             * has an android test dependency
             * on testFeatureLib, shared and
             * sharedMidLib.
             */
            """.trimIndent()
        )

        checkFormatter(
            """
            /**
             *             +-> lib1
             *             |
             * feature1 ---+-> javalib1
             *             |
             *             +-> baseModule
             */
            """.trimIndent(),
            KDocFormattingOptions(maxLineWidth = 100, maxCommentWidth = 30),
            """
            /**
             *             +-> lib1
             *             |
             * feature1 ---+-> javalib1
             *             |
             *             +-> baseModule
             */
            """.trimIndent()
        )
    }

    @Test
    fun testHtmlLists() {
        checkFormatter(
            """
            /**
             * <ul>
             *   <li>Incremental merge will never clean the output.
             *   <li>The inputs must be able to tell which changes to relative files have been made.
             *   <li>Intermediate state must be saved between merges.
             * </ul>
             */
            """.trimIndent(),
            KDocFormattingOptions(maxLineWidth = 100, maxCommentWidth = 60),
            """
            /**
             * <ul>
             * <li>Incremental merge will never clean the output.
             * <li>The inputs must be able to tell which changes to
             *     relative files have been made.
             * <li>Intermediate state must be saved between merges.
             * </ul>
             */
            """.trimIndent()
        )
    }

    @Test
    fun testVariousMarkup() {
        val source = "/**\n" +
            """
            This document contains a bunch of markup examples
            that I will use
            to verify that things are handled
            correctly via markdown.

            This is a header. Should appear alone.
            --------------------------------------
            This should not be on the same line as the header.

            This is a header. Should appear alone.
            -
            This should not be on the same line as the header.

            This is a header. Should appear alone.
            ======================================
            This should not be on the same line as the header.

            This is a header. Should appear alone.
            =
            This should not be on the same line as the header.
            Note that we don't treat this as a header
            because it's not on its own line. Instead
            it's considered a separating line.
            ---
            More text. Should not be on the previous line.

            --- This usage of --- where it's not on its own
            line should not be used as a header or separator line.

            List stuff:
            1. First item
            2. Second item
            3. Third item

            # Text styles #
            **Bold**, *italics*. \*Not italics\*.

            ## More text styles
            ~~strikethrough~~, _underlined_.

            ### Blockquotes #

            Here's some text.
            > Here's some more text that
            > is indented. More text.
            > > And here's some even
            > > more indented text
            > Back to the top level

            More:
            > This whole paragraph should be treated as a block quote.
            This whole paragraph should be treated as a block quote.
            This whole paragraph should be treated as a block quote.
            This whole paragraph should be treated as a block quote.

            ### Lists
            Plus lists:
            + First
            + Second
            + Third

            Dash lists:
            - First
            - Second
            - Third

            List items with multiple paragraphs:

            * This is my list item. It has
              text on many lines.

              This is a continuation of the first bullet.
            * And this is the second.

            ### Code blocks in list items

            Escapes: I should look for cases where I place a number followed
            by a period (or asterisk) at the beginning of a line and if so,
            escape it:

            The meaning of life:
            42\. This doesn't seem to work in IntelliJ's markdown formatter.

            ### Horizontal rules
            *********
            ---------
            ***
            * * *
            - - -
            """.trimIndent().split("\n").joinToString(separator = "\n") { " * $it".trimEnd() } + "\n */"

        checkFormatter(
            source,
            KDocFormattingOptions(100),
            """
            /**
             * This document contains a bunch of markup examples that I will use to verify that things are
             * handled correctly via markdown.
             *
             * This is a header. Should appear alone.
             * --------------------------------------
             * This should not be on the same line as the header.
             *
             * This is a header. Should appear alone.
             * -
             * This should not be on the same line as the header.
             *
             * This is a header. Should appear alone.
             * ======================================
             * This should not be on the same line as the header.
             *
             * This is a header. Should appear alone.
             * =
             * This should not be on the same line as the header. Note that we don't treat this as a header
             * because it's not on its own line. Instead it's considered a separating line.
             * ---
             * More text. Should not be on the previous line.
             *
             * --- This usage of --- where it's not on its own line should not be used as a header or
             * separator line.
             *
             * List stuff:
             * 1. First item
             * 2. Second item
             * 3. Third item
             *
             * # Text styles #
             * **Bold**, *italics*. \*Not italics\*.
             *
             * ## More text styles
             * ~~strikethrough~~, _underlined_.
             *
             * ### Blockquotes #
             *
             * Here's some text.
             * > Here's some more text that is indented. More text. > And here's some even > more indented
             * > text Back to the top level
             *
             * More:
             * > This whole paragraph should be treated as a block quote. This whole paragraph should be
             * > treated as a block quote. This whole paragraph should be treated as a block quote. This
             * > whole paragraph should be treated as a block quote.
             *
             * ### Lists
             * Plus lists:
             * + First
             * + Second
             * + Third
             *
             * Dash lists:
             * - First
             * - Second
             * - Third
             *
             * List items with multiple paragraphs:
             * * This is my list item. It has text on many lines.
             *
             *   This is a continuation of the first bullet.
             * * And this is the second.
             *
             * ### Code blocks in list items
             *
             * Escapes: I should look for cases where I place a number followed by a period (or asterisk) at
             * the beginning of a line and if so, escape it:
             *
             * The meaning of life: 42\. This doesn't seem to work in IntelliJ's markdown formatter.
             *
             * ### Horizontal rules
             * *********
             * ---------
             * ***
             * * * *
             * - - -
             */
            """.trimIndent()
        )
    }

    @Test
    fun testLineComments() {
        val source =
            """
            //
            // Information about a request to run lint.
            //
            // **NOTE: This is not a public or final API; if you rely on this be prepared
            // to adjust your code for the next tools release.**
            //
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(40),
            """
            // Information about a request to
            // run lint.
            //
            // **NOTE: This is not a public or
            // final API; if you rely on this be
            // prepared to adjust your code for
            // the next tools release.**
            """.trimIndent()
        )
    }

    @Test
    fun testMoreLineComments() {
        val source =
            """
            // Do not clean
            // this
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(70),
            """
            // Do not clean this
            """.trimIndent()
        )
    }

    @Test
    fun testListContinuations() {
        val source =
            """
            /**
             * * This is my list item. It has
             *   text on many lines.
             *
             *   This is a continuation of the first bullet.
             * * And this is the second.
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(40),
            """
            /**
             * * This is my list item. It has
             *   text on many lines.
             *
             *   This is a continuation of the
             *   first bullet.
             * * And this is the second.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testListContinuations2() {
        val source = "/**\n" + """
            List items with multiple paragraphs:

            * This is my list item. It has
              text on many lines.

              This is a continuation of the first bullet.
            * And this is the second.
        """.trimIndent().split("\n").joinToString(separator = "\n") { " * $it".trimEnd() } + "\n */"

        checkFormatter(
            source,
            KDocFormattingOptions(100),
            """
            /**
             * List items with multiple paragraphs:
             * * This is my list item. It has text on many lines.
             *
             *   This is a continuation of the first bullet.
             * * And this is the second.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testAccidentalHeader() {
        val source =
            """
             /**
             * Constructs a simplified version of the internal JVM description of the given method. This is
             * in the same format as {@link #getMethodDescription} above, the difference being we don't have
             * the actual PSI for the method type, we just construct the signature from the [method] name,
             * the list of [argumentTypes] and optionally include the [returnType].
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            // Note how this places the "#" in column 0 which will then
            // be re-interpreted as a header next time we format it!
            // Idea: @{link #} should become {@link#} or with a nbsp;
            """
            /**
             * Constructs a simplified version of the internal JVM
             * description of the given method. This is in the same format as
             * {@link #getMethodDescription} above, the difference being we
             * don't have the actual PSI for the method type, we just construct
             * the signature from the [method] name, the list of [argumentTypes]
             * and optionally include the [returnType].
             */
            """.trimIndent()
        )
    }

    @Test
    fun testTODO() {
        val source =
            """
            /**
             * Adds the given dependency graph (the output of the Gradle dependency task)
             * to be constructed when mocking a Gradle model for this project.
             * <p>
             * To generate this, run for example
             * <pre>
             *     ./gradlew :app:dependencies
             * </pre>
             * and then look at the debugCompileClasspath (or other graph that you want
             * to model).
             * TODO: Adds the given dependency graph (the output of the Gradle dependency task)
             * to be constructed when mocking a Gradle model for this project.
             * TODO: More stuff to do here
             * @param dependencyGraph the graph description
             * @return this for constructor chaining
             * TODO: Consider looking at the localization="suggested" attribute in
             * the platform attrs.xml to catch future recommended attributes.
             * TODO: Also adds the given dependency graph (the output of the Gradle dependency task)
             * to be constructed when mocking a Gradle model for this project.
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(72),
            // Note how this places the "#" in column 0 which will then
            // be re-interpreted as a header next time we format it!
            // Idea: @{link #} should become {@link#} or with a nbsp;
            """
            /**
             * Adds the given dependency graph (the output of the Gradle
             * dependency task) to be constructed when mocking a Gradle model
             * for this project.
             *
             * To generate this, run for example
             * <pre>
             *     ./gradlew :app:dependencies
             * </pre>
             * and then look at the debugCompileClasspath (or other graph that
             * you want to model).
             *
             * TODO: Adds the given dependency graph (the output of the Gradle
             *     dependency task) to be constructed when
             *     mocking a Gradle model for this project.
             * TODO: More stuff to do here
             *
             * @param dependencyGraph the graph description
             * @return this for constructor chaining
             *
             * TODO: Consider looking at the localization="suggested" attribute
             *     in the platform attrs.xml to catch future recommended attributes.
             * TODO: Also adds the given dependency graph (the output of the
             *     Gradle dependency task) to be constructed
             *     when mocking a Gradle model for this project.
             */
            """.trimIndent()
        )
    }

    @Test
    fun testHtml() {
        // Comment from lint's SourceCodeScanner class doc. Tests a number of things --
        // markup conversion (<h2> to ##, <p> to blank lines), list item indentation,
        // trimming blank lines from the end, etc.
        val source =
            """
             /**
             * Interface to be implemented by lint detectors that want to analyze
             * Java source files (or other similar source files, such as Kotlin files.)
             * <p>
             * There are several different common patterns for detecting issues:
             * <ul>
             * <li> Checking calls to a given method. For this see
             * {@link #getApplicableMethodNames()} and
             * {@link #visitMethodCall(JavaContext, UCallExpression, PsiMethod)}</li>
             * <li> Instantiating a given class. For this, see
             * {@link #getApplicableConstructorTypes()} and
             * {@link #visitConstructor(JavaContext, UCallExpression, PsiMethod)}</li>
             * <li> Referencing a given constant. For this, see
             * {@link #getApplicableReferenceNames()} and
             * {@link #visitReference(JavaContext, UReferenceExpression, PsiElement)}</li>
             * <li> Extending a given class or implementing a given interface.
             * For this, see {@link #applicableSuperClasses()} and
             * {@link #visitClass(JavaContext, UClass)}</li>
             * <li> More complicated scenarios: perform a general AST
             * traversal with a visitor. In this case, first tell lint which
             * AST node types you're interested in with the
             * {@link #getApplicableUastTypes()} method, and then provide a
             * {@link UElementHandler} from the {@link #createUastHandler(JavaContext)}
             * where you override the various applicable handler methods. This is
             * done rather than a general visitor from the root node to avoid
             * having to have every single lint detector (there are hundreds) do a full
             * tree traversal on its own.</li>
             * </ul>
             * <p>
             * {@linkplain SourceCodeScanner} exposes the UAST API to lint checks.
             * UAST is short for "Universal AST" and is an abstract syntax tree library
             * which abstracts away details about Java versus Kotlin versus other similar languages
             * and lets the client of the library access the AST in a unified way.
             * <p>
             * UAST isn't actually a full replacement for PSI; it <b>augments</b> PSI.
             * Essentially, UAST is used for the <b>inside</b> of methods (e.g. method bodies),
             * and things like field initializers. PSI continues to be used at the outer
             * level: for packages, classes, and methods (declarations and signatures).
             * There are also wrappers around some of these for convenience.
             * <p>
             * The {@linkplain SourceCodeScanner} interface reflects this fact. For example,
             * when you indicate that you want to check calls to a method named {@code foo},
             * the call site node is a UAST node (in this case, {@link UCallExpression},
             * but the called method itself is a {@link PsiMethod}, since that method
             * might be anywhere (including in a library that we don't have source for,
             * so UAST doesn't make sense.)
             * <p>
             * <h2>Migrating JavaPsiScanner to SourceCodeScanner</h2>
             * As described above, PSI is still used, so a lot of code will remain the
             * same. For example, all resolve methods, including those in UAST, will
             * continue to return PsiElement, not necessarily a UElement. For example,
             * if you resolve a method call or field reference, you'll get a
             * {@link PsiMethod} or {@link PsiField} back.
             * <p>
             * However, the visitor methods have all changed, generally to change
             * to UAST types. For example, the signature
             * {@link JavaPsiScanner#visitMethodCall(JavaContext, JavaElementVisitor, PsiMethodCallExpression, PsiMethod)}
             * should be changed to {@link SourceCodeScanner#visitMethodCall(JavaContext, UCallExpression, PsiMethod)}.
             * <p>
             * Similarly, replace {@link JavaPsiScanner#createPsiVisitor} with {@link SourceCodeScanner#createUastHandler},
             * {@link JavaPsiScanner#getApplicablePsiTypes()} with {@link SourceCodeScanner#getApplicableUastTypes()}, etc.
             * <p>
             * There are a bunch of new methods on classes like {@link JavaContext} which lets
             * you pass in a {@link UElement} to match the existing {@link PsiElement} methods.
             * <p>
             * If you have code which does something specific with PSI classes,
             * the following mapping table in alphabetical order might be helpful, since it lists the
             * corresponding UAST classes.
             * <table>
             *     <caption>Mapping between PSI and UAST classes</caption>
             *     <tr><th>PSI</th><th>UAST</th></tr>
             *     <tr><th>com.intellij.psi.</th><th>org.jetbrains.uast.</th></tr>
             *     <tr><td>IElementType</td><td>UastBinaryOperator</td></tr>
             *     <tr><td>PsiAnnotation</td><td>UAnnotation</td></tr>
             *     <tr><td>PsiAnonymousClass</td><td>UAnonymousClass</td></tr>
             *     <tr><td>PsiArrayAccessExpression</td><td>UArrayAccessExpression</td></tr>
             *     <tr><td>PsiBinaryExpression</td><td>UBinaryExpression</td></tr>
             *     <tr><td>PsiCallExpression</td><td>UCallExpression</td></tr>
             *     <tr><td>PsiCatchSection</td><td>UCatchClause</td></tr>
             *     <tr><td>PsiClass</td><td>UClass</td></tr>
             *     <tr><td>PsiClassObjectAccessExpression</td><td>UClassLiteralExpression</td></tr>
             *     <tr><td>PsiConditionalExpression</td><td>UIfExpression</td></tr>
             *     <tr><td>PsiDeclarationStatement</td><td>UDeclarationsExpression</td></tr>
             *     <tr><td>PsiDoWhileStatement</td><td>UDoWhileExpression</td></tr>
             *     <tr><td>PsiElement</td><td>UElement</td></tr>
             *     <tr><td>PsiExpression</td><td>UExpression</td></tr>
             *     <tr><td>PsiForeachStatement</td><td>UForEachExpression</td></tr>
             *     <tr><td>PsiIdentifier</td><td>USimpleNameReferenceExpression</td></tr>
             *     <tr><td>PsiIfStatement</td><td>UIfExpression</td></tr>
             *     <tr><td>PsiImportStatement</td><td>UImportStatement</td></tr>
             *     <tr><td>PsiImportStaticStatement</td><td>UImportStatement</td></tr>
             *     <tr><td>PsiJavaCodeReferenceElement</td><td>UReferenceExpression</td></tr>
             *     <tr><td>PsiLiteral</td><td>ULiteralExpression</td></tr>
             *     <tr><td>PsiLocalVariable</td><td>ULocalVariable</td></tr>
             *     <tr><td>PsiMethod</td><td>UMethod</td></tr>
             *     <tr><td>PsiMethodCallExpression</td><td>UCallExpression</td></tr>
             *     <tr><td>PsiNameValuePair</td><td>UNamedExpression</td></tr>
             *     <tr><td>PsiNewExpression</td><td>UCallExpression</td></tr>
             *     <tr><td>PsiParameter</td><td>UParameter</td></tr>
             *     <tr><td>PsiParenthesizedExpression</td><td>UParenthesizedExpression</td></tr>
             *     <tr><td>PsiPolyadicExpression</td><td>UPolyadicExpression</td></tr>
             *     <tr><td>PsiPostfixExpression</td><td>UPostfixExpression or UUnaryExpression</td></tr>
             *     <tr><td>PsiPrefixExpression</td><td>UPrefixExpression or UUnaryExpression</td></tr>
             *     <tr><td>PsiReference</td><td>UReferenceExpression</td></tr>
             *     <tr><td>PsiReference</td><td>UResolvable</td></tr>
             *     <tr><td>PsiReferenceExpression</td><td>UReferenceExpression</td></tr>
             *     <tr><td>PsiReturnStatement</td><td>UReturnExpression</td></tr>
             *     <tr><td>PsiSuperExpression</td><td>USuperExpression</td></tr>
             *     <tr><td>PsiSwitchLabelStatement</td><td>USwitchClauseExpression</td></tr>
             *     <tr><td>PsiSwitchStatement</td><td>USwitchExpression</td></tr>
             *     <tr><td>PsiThisExpression</td><td>UThisExpression</td></tr>
             *     <tr><td>PsiThrowStatement</td><td>UThrowExpression</td></tr>
             *     <tr><td>PsiTryStatement</td><td>UTryExpression</td></tr>
             *     <tr><td>PsiTypeCastExpression</td><td>UBinaryExpressionWithType</td></tr>
             *     <tr><td>PsiWhileStatement</td><td>UWhileExpression</td></tr>
             * </table>
             * Note however that UAST isn't just a "renaming of classes"; there are
             * some changes to the structure of the AST as well. Particularly around
             * calls.
             *
             * <h3>Parents</h3>
             * In UAST, you get your parent {@linkplain UElement} by calling
             * {@code getUastParent} instead of {@code getParent}. This is to avoid
             * method name clashes on some elements which are both UAST elements
             * and PSI elements at the same time - such as {@link UMethod}.
             * <h3>Children</h3>
             * When you're going in the opposite direction (e.g. you have a {@linkplain PsiMethod}
             * and you want to look at its content, you should <b>not</b> use
             * {@link PsiMethod#getBody()}. This will only give you the PSI child content,
             * which won't work for example when dealing with Kotlin methods.
             * Normally lint passes you the {@linkplain UMethod} which you should be procesing
             * instead. But if for some reason you need to look up the UAST method
             * body from a {@linkplain PsiMethod}, use this:
             * <pre>
             *     UastContext context = UastUtils.getUastContext(element);
             *     UExpression body = context.getMethodBody(method);
             * </pre>
             * Similarly if you have a {@link PsiField} and you want to look up its field
             * initializer, use this:
             * <pre>
             *     UastContext context = UastUtils.getUastContext(element);
             *     UExpression initializer = context.getInitializerBody(field);
             * </pre>
             *
             * <h3>Call names</h3>
             * In PSI, a call was represented by a PsiCallExpression, and to get to things
             * like the method called or to the operand/qualifier, you'd first need to get
             * the "method expression". In UAST there is no method expression and this
             * information is available directly on the {@linkplain UCallExpression} element.
             * Therefore, here's how you'd change the code:
             * <pre>
             * &lt;    call.getMethodExpression().getReferenceName();
             * ---
             * &gt;    call.getMethodName()
             * </pre>
             * <h3>Call qualifiers</h3>
             * Similarly,
             * <pre>
             * &lt;    call.getMethodExpression().getQualifierExpression();
             * ---
             * &gt;    call.getReceiver()
             * </pre>
             * <h3>Call arguments</h3>
             * PSI had a separate PsiArgumentList element you had to look up before you could
             * get to the actual arguments, as an array. In UAST these are available directly on
             * the call, and are represented as a list instead of an array.
             * <pre>
             * &lt;    PsiExpression[] args = call.getArgumentList().getExpressions();
             * ---
             * &gt;    List<UExpression> args = call.getValueArguments();
             * </pre>
             * Typically you also need to go through your code and replace array access,
             * arg\[i], with list access, {@code arg.get(i)}. Or in Kotlin, just arg\[i]...
             *
             * <h3>Instanceof</h3>
             * You may have code which does something like "parent instanceof PsiAssignmentExpression"
             * to see if something is an assignment. Instead, use one of the many utilities
             * in {@link UastExpressionUtils} - such as {@link UastExpressionUtils#isAssignment(UElement)}.
             * Take a look at all the methods there now - there are methods for checking whether
             * a call is a constructor, whether an expression is an array initializer, etc etc.
             *
             * <h3>Android Resources</h3>
             * Don't do your own AST lookup to figure out if something is a reference to
             * an Android resource (e.g. see if the class refers to an inner class of a class
             * named "R" etc.)  There is now a new utility class which handles this:
             * {@link ResourceReference}. Here's an example of code which has a {@link UExpression}
             * and wants to know it's referencing a R.styleable resource:
             * <pre>
             *        ResourceReference reference = ResourceReference.get(expression);
             *        if (reference == null || reference.getType() != ResourceType.STYLEABLE) {
             *            return;
             *        }
             *        ...
             * </pre>
             *
             * <h3>Binary Expressions</h3>
             * If you had been using {@link PsiBinaryExpression} for things like checking comparator
             * operators or arithmetic combination of operands, you can replace this with
             * {@link UBinaryExpression}. <b>But you normally shouldn't; you should use
             * {@link UPolyadicExpression} instead</b>. A polyadic expression is just like a binary
             * expression, but possibly with more than two terms. With the old parser backend,
             * an expression like "A + B + C" would be represented by nested binary expressions
             * (first A + B, then a parent element which combined that binary expression with C).
             * However, this will now be provided as a {@link UPolyadicExpression} instead. And
             * the binary case is handled trivially without the need to special case it.
             * <h3>Method name changes</h3>
             * The following table maps some common method names and what their corresponding
             * names are in UAST.
             * <table>
             *     <caption>Mapping between PSI and UAST method names</caption></caption>
             *     <tr><th>PSI</th><th>UAST</th></tr>
             *     <tr><td>getArgumentList</td><td>getValueArguments</td></tr>
             *     <tr><td>getCatchSections</td><td>getCatchClauses</td></tr>
             *     <tr><td>getDeclaredElements</td><td>getDeclarations</td></tr>
             *     <tr><td>getElseBranch</td><td>getElseExpression</td></tr>
             *     <tr><td>getInitializer</td><td>getUastInitializer</td></tr>
             *     <tr><td>getLExpression</td><td>getLeftOperand</td></tr>
             *     <tr><td>getOperationTokenType</td><td>getOperator</td></tr>
             *     <tr><td>getOwner</td><td>getUastParent</td></tr>
             *     <tr><td>getParent</td><td>getUastParent</td></tr>
             *     <tr><td>getRExpression</td><td>getRightOperand</td></tr>
             *     <tr><td>getReturnValue</td><td>getReturnExpression</td></tr>
             *     <tr><td>getText</td><td>asSourceString</td></tr>
             *     <tr><td>getThenBranch</td><td>getThenExpression</td></tr>
             *     <tr><td>getType</td><td>getExpressionType</td></tr>
             *     <tr><td>getTypeParameters</td><td>getTypeArguments</td></tr>
             *     <tr><td>resolveMethod</td><td>resolve</td></tr>
             * </table>
             * <h3>Handlers versus visitors</h3>
             * If you are processing a method on your own, or even a full class, you should switch
             * from JavaRecursiveElementVisitor to AbstractUastVisitor.
             * However, most lint checks don't do their own full AST traversal; they instead
             * participate in a shared traversal of the tree, registering element types they're
             * interested with using {@link #getApplicableUastTypes()} and then providing
             * a visitor where they implement the corresponding visit methods. However, from
             * these visitors you should <b>not</b> be calling super.visitX. To remove this
             * whole confusion, lint now provides a separate class, {@link UElementHandler}.
             * For the shared traversal, just provide this handler instead and implement the
             * appropriate visit methods. It will throw an error if you register element types
             * in {@linkplain #getApplicableUastTypes()} that you don't override.
             *
             * <p>
             * <h3>Migrating JavaScanner to SourceCodeScanner</h3>
             * First read the javadoc on how to convert from the older {@linkplain JavaScanner}
             * interface over to {@linkplain JavaPsiScanner}. While {@linkplain JavaPsiScanner} is itself
             * deprecated, it's a lot closer to {@link SourceCodeScanner} so a lot of the same concepts
             * apply; then follow the above section.
             * <p>
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(120),
            """
            /**
             * Interface to be implemented by lint detectors that want to analyze Java source files (or other similar source
             * files, such as Kotlin files.)
             *
             * There are several different common patterns for detecting issues:
             * <ul>
             * <li> Checking calls to a given method. For this see {@link #getApplicableMethodNames()} and
             *     {@link #visitMethodCall(JavaContext, UCallExpression, PsiMethod)}</li>
             * <li> Instantiating a given class. For this, see {@link #getApplicableConstructorTypes()} and
             *     {@link #visitConstructor(JavaContext, UCallExpression, PsiMethod)}</li>
             * <li> Referencing a given constant. For this, see {@link #getApplicableReferenceNames()} and
             *     {@link #visitReference(JavaContext, UReferenceExpression, PsiElement)}</li>
             * <li> Extending a given class or implementing a given interface. For this, see {@link #applicableSuperClasses()}
             *     and {@link #visitClass(JavaContext, UClass)}</li>
             * <li> More complicated scenarios: perform a general AST traversal with a visitor. In this case, first tell lint
             *     which AST node types you're interested in with the {@link #getApplicableUastTypes()} method, and then
             *     provide a {@link UElementHandler} from the {@link #createUastHandler(JavaContext)} where you override the
             *     various applicable handler methods. This is done rather than a general visitor from the root node to avoid
             *     having to have every single lint detector (there are hundreds) do a full tree traversal on its own.</li>
             * </ul>
             * {@linkplain SourceCodeScanner} exposes the UAST API to lint checks. UAST is short for "Universal AST" and is an
             * abstract syntax tree library which abstracts away details about Java versus Kotlin versus other similar languages
             * and lets the client of the library access the AST in a unified way.
             *
             * UAST isn't actually a full replacement for PSI; it **augments** PSI. Essentially, UAST is used for the **inside**
             * of methods (e.g. method bodies), and things like field initializers. PSI continues to be used at the outer level:
             * for packages, classes, and methods (declarations and signatures). There are also wrappers around some of these
             * for convenience.
             *
             * The {@linkplain SourceCodeScanner} interface reflects this fact. For example, when you indicate that you
             * want to check calls to a method named {@code foo}, the call site node is a UAST node (in this case, {@link
             * UCallExpression}, but the called method itself is a {@link PsiMethod}, since that method might be anywhere
             * (including in a library that we don't have source for, so UAST doesn't make sense.)
             *
             * ## Migrating JavaPsiScanner to SourceCodeScanner
             * As described above, PSI is still used, so a lot of code will remain the same. For example, all resolve methods,
             * including those in UAST, will continue to return PsiElement, not necessarily a UElement. For example, if you
             * resolve a method call or field reference, you'll get a {@link PsiMethod} or {@link PsiField} back.
             *
             * However, the visitor methods have all changed, generally to change to UAST types. For example, the signature
             * {@link JavaPsiScanner#visitMethodCall(JavaContext, JavaElementVisitor, PsiMethodCallExpression, PsiMethod)}
             * should be changed to {@link SourceCodeScanner#visitMethodCall(JavaContext, UCallExpression, PsiMethod)}.
             *
             * Similarly, replace {@link JavaPsiScanner#createPsiVisitor} with {@link SourceCodeScanner#createUastHandler},
             * {@link JavaPsiScanner#getApplicablePsiTypes()} with {@link SourceCodeScanner#getApplicableUastTypes()}, etc.
             *
             * There are a bunch of new methods on classes like {@link JavaContext} which lets you pass in a {@link UElement} to
             * match the existing {@link PsiElement} methods.
             *
             * If you have code which does something specific with PSI classes, the following mapping table in alphabetical
             * order might be helpful, since it lists the corresponding UAST classes.
             * <table>
             *
             *     <caption>Mapping between PSI and UAST classes</caption>
             *     <tr><th>PSI</th><th>UAST</th></tr>
             *     <tr><th>com.intellij.psi.</th><th>org.jetbrains.uast.</th></tr>
             *     <tr><td>IElementType</td><td>UastBinaryOperator</td></tr>
             *     <tr><td>PsiAnnotation</td><td>UAnnotation</td></tr>
             *     <tr><td>PsiAnonymousClass</td><td>UAnonymousClass</td></tr>
             *     <tr><td>PsiArrayAccessExpression</td><td>UArrayAccessExpression</td></tr>
             *     <tr><td>PsiBinaryExpression</td><td>UBinaryExpression</td></tr>
             *     <tr><td>PsiCallExpression</td><td>UCallExpression</td></tr>
             *     <tr><td>PsiCatchSection</td><td>UCatchClause</td></tr>
             *     <tr><td>PsiClass</td><td>UClass</td></tr>
             *     <tr><td>PsiClassObjectAccessExpression</td><td>UClassLiteralExpression</td></tr>
             *     <tr><td>PsiConditionalExpression</td><td>UIfExpression</td></tr>
             *     <tr><td>PsiDeclarationStatement</td><td>UDeclarationsExpression</td></tr>
             *     <tr><td>PsiDoWhileStatement</td><td>UDoWhileExpression</td></tr>
             *     <tr><td>PsiElement</td><td>UElement</td></tr>
             *     <tr><td>PsiExpression</td><td>UExpression</td></tr>
             *     <tr><td>PsiForeachStatement</td><td>UForEachExpression</td></tr>
             *     <tr><td>PsiIdentifier</td><td>USimpleNameReferenceExpression</td></tr>
             *     <tr><td>PsiIfStatement</td><td>UIfExpression</td></tr>
             *     <tr><td>PsiImportStatement</td><td>UImportStatement</td></tr>
             *     <tr><td>PsiImportStaticStatement</td><td>UImportStatement</td></tr>
             *     <tr><td>PsiJavaCodeReferenceElement</td><td>UReferenceExpression</td></tr>
             *     <tr><td>PsiLiteral</td><td>ULiteralExpression</td></tr>
             *     <tr><td>PsiLocalVariable</td><td>ULocalVariable</td></tr>
             *     <tr><td>PsiMethod</td><td>UMethod</td></tr>
             *     <tr><td>PsiMethodCallExpression</td><td>UCallExpression</td></tr>
             *     <tr><td>PsiNameValuePair</td><td>UNamedExpression</td></tr>
             *     <tr><td>PsiNewExpression</td><td>UCallExpression</td></tr>
             *     <tr><td>PsiParameter</td><td>UParameter</td></tr>
             *     <tr><td>PsiParenthesizedExpression</td><td>UParenthesizedExpression</td></tr>
             *     <tr><td>PsiPolyadicExpression</td><td>UPolyadicExpression</td></tr>
             *     <tr><td>PsiPostfixExpression</td><td>UPostfixExpression or UUnaryExpression</td></tr>
             *     <tr><td>PsiPrefixExpression</td><td>UPrefixExpression or UUnaryExpression</td></tr>
             *     <tr><td>PsiReference</td><td>UReferenceExpression</td></tr>
             *     <tr><td>PsiReference</td><td>UResolvable</td></tr>
             *     <tr><td>PsiReferenceExpression</td><td>UReferenceExpression</td></tr>
             *     <tr><td>PsiReturnStatement</td><td>UReturnExpression</td></tr>
             *     <tr><td>PsiSuperExpression</td><td>USuperExpression</td></tr>
             *     <tr><td>PsiSwitchLabelStatement</td><td>USwitchClauseExpression</td></tr>
             *     <tr><td>PsiSwitchStatement</td><td>USwitchExpression</td></tr>
             *     <tr><td>PsiThisExpression</td><td>UThisExpression</td></tr>
             *     <tr><td>PsiThrowStatement</td><td>UThrowExpression</td></tr>
             *     <tr><td>PsiTryStatement</td><td>UTryExpression</td></tr>
             *     <tr><td>PsiTypeCastExpression</td><td>UBinaryExpressionWithType</td></tr>
             *     <tr><td>PsiWhileStatement</td><td>UWhileExpression</td></tr>
             * </table>
             *
             * Note however that UAST isn't just a "renaming of classes"; there are some changes to the structure of the AST as
             * well. Particularly around calls.
             *
             * ### Parents
             * In UAST, you get your parent {@linkplain UElement} by calling {@code getUastParent} instead of {@code getParent}.
             * This is to avoid method name clashes on some elements which are both UAST elements and PSI elements at the same
             * time - such as {@link UMethod}.
             *
             * ### Children
             * When you're going in the opposite direction (e.g. you have a {@linkplain PsiMethod} and you want to look at its
             * content, you should **not** use {@link PsiMethod#getBody()}. This will only give you the PSI child content,
             * which won't work for example when dealing with Kotlin methods. Normally lint passes you the {@linkplain UMethod}
             * which you should be procesing instead. But if for some reason you need to look up the UAST method body from a
             * {@linkplain PsiMethod}, use this:
             * <pre>
             *     UastContext context = UastUtils.getUastContext(element);
             *     UExpression body = context.getMethodBody(method);
             * </pre>
             * Similarly if you have a {@link PsiField} and you want to look up its field initializer, use this:
             * <pre>
             *     UastContext context = UastUtils.getUastContext(element);
             *     UExpression initializer = context.getInitializerBody(field);
             * </pre>
             *
             * ### Call names
             * In PSI, a call was represented by a PsiCallExpression, and to get to things like the method called or to the
             * operand/qualifier, you'd first need to get the "method expression". In UAST there is no method expression and
             * this information is available directly on the {@linkplain UCallExpression} element. Therefore, here's how you'd
             * change the code:
             * <pre>
             * &lt;    call.getMethodExpression().getReferenceName();
             * ---
             * &gt;    call.getMethodName()
             * </pre>
             *
             * ### Call qualifiers
             * Similarly,
             * <pre>
             * &lt;    call.getMethodExpression().getQualifierExpression();
             * ---
             * &gt;    call.getReceiver()
             * </pre>
             *
             * ### Call arguments
             * PSI had a separate PsiArgumentList element you had to look up before you could get to the actual arguments, as an
             * array. In UAST these are available directly on the call, and are represented as a list instead of an array.
             * <pre>
             * &lt;    PsiExpression[] args = call.getArgumentList().getExpressions();
             * ---
             * &gt;    List<UExpression> args = call.getValueArguments();
             * </pre>
             * Typically you also need to go through your code and replace array access, arg\[i], with list access, {@code
             * arg.get(i)}. Or in Kotlin, just arg\[i]...
             *
             * ### Instanceof
             * You may have code which does something like "parent instanceof PsiAssignmentExpression" to see if something
             * is an assignment. Instead, use one of the many utilities in {@link UastExpressionUtils} - such as {@link
             * UastExpressionUtils#isAssignment(UElement)}. Take a look at all the methods there now - there are methods for
             * checking whether a call is a constructor, whether an expression is an array initializer, etc etc.
             *
             * ### Android Resources
             * Don't do your own AST lookup to figure out if something is a reference to an Android resource (e.g. see if the
             * class refers to an inner class of a class named "R" etc.) There is now a new utility class which handles this:
             * {@link ResourceReference}. Here's an example of code which has a {@link UExpression} and wants to know it's
             * referencing a R.styleable resource:
             * <pre>
             *        ResourceReference reference = ResourceReference.get(expression);
             *        if (reference == null || reference.getType() != ResourceType.STYLEABLE) {
             *            return;
             *        }
             *        ...
             * </pre>
             *
             * ### Binary Expressions
             * If you had been using {@link PsiBinaryExpression} for things like checking comparator operators or arithmetic
             * combination of operands, you can replace this with {@link UBinaryExpression}. **But you normally shouldn't;
             * you should use {@link UPolyadicExpression} instead**. A polyadic expression is just like a binary expression,
             * but possibly with more than two terms. With the old parser backend, an expression like "A + B + C" would
             * be represented by nested binary expressions (first A + B, then a parent element which combined that binary
             * expression with C). However, this will now be provided as a {@link UPolyadicExpression} instead. And the binary
             * case is handled trivially without the need to special case it.
             *
             * ### Method name changes
             * The following table maps some common method names and what their corresponding names are in UAST.
             * <table>
             *
             *     <caption>Mapping between PSI and UAST method names</caption></caption>
             *     <tr><th>PSI</th><th>UAST</th></tr>
             *     <tr><td>getArgumentList</td><td>getValueArguments</td></tr>
             *     <tr><td>getCatchSections</td><td>getCatchClauses</td></tr>
             *     <tr><td>getDeclaredElements</td><td>getDeclarations</td></tr>
             *     <tr><td>getElseBranch</td><td>getElseExpression</td></tr>
             *     <tr><td>getInitializer</td><td>getUastInitializer</td></tr>
             *     <tr><td>getLExpression</td><td>getLeftOperand</td></tr>
             *     <tr><td>getOperationTokenType</td><td>getOperator</td></tr>
             *     <tr><td>getOwner</td><td>getUastParent</td></tr>
             *     <tr><td>getParent</td><td>getUastParent</td></tr>
             *     <tr><td>getRExpression</td><td>getRightOperand</td></tr>
             *     <tr><td>getReturnValue</td><td>getReturnExpression</td></tr>
             *     <tr><td>getText</td><td>asSourceString</td></tr>
             *     <tr><td>getThenBranch</td><td>getThenExpression</td></tr>
             *     <tr><td>getType</td><td>getExpressionType</td></tr>
             *     <tr><td>getTypeParameters</td><td>getTypeArguments</td></tr>
             *     <tr><td>resolveMethod</td><td>resolve</td></tr>
             * </table>
             *
             * ### Handlers versus visitors
             * If you are processing a method on your own, or even a full class, you should switch from
             * JavaRecursiveElementVisitor to AbstractUastVisitor. However, most lint checks don't do their own full AST
             * traversal; they instead participate in a shared traversal of the tree, registering element types they're
             * interested with using {@link #getApplicableUastTypes()} and then providing a visitor where they implement the
             * corresponding visit methods. However, from these visitors you should **not** be calling super.visitX. To remove
             * this whole confusion, lint now provides a separate class, {@link UElementHandler}. For the shared traversal, just
             * provide this handler instead and implement the appropriate visit methods. It will throw an error if you register
             * element types in {@linkplain #getApplicableUastTypes()} that you don't override.
             *
             * ### Migrating JavaScanner to SourceCodeScanner
             * First read the javadoc on how to convert from the older {@linkplain JavaScanner} interface over to {@linkplain
             * JavaPsiScanner}. While {@linkplain JavaPsiScanner} is itself deprecated, it's a lot closer to {@link
             * SourceCodeScanner} so a lot of the same concepts apply; then follow the above section.
             */
            """.trimIndent(),
            verify = false // Not quite working yet
        )
    }

    @Test
    fun testWordJoining() {
        val source =
            """
            /**
             * which you can render with something like this:
             * `dot -Tpng -o/tmp/graph.png toString.dot`
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(65),
            """
            /**
             * which you can render with something like this:
             * `dot -Tpng -o/tmp/graph.png toString.dot`
             */
            """.trimIndent())
    }

    // --------------------------------------------------------------------
    // A few failing test cases here for corner cases that aren't handled
    // right yet.
    // --------------------------------------------------------------------

    @Disabled("Lists within quoted blocks not yet supported")
    @Test
    fun testNestedWithinQuoted() {
        val source =
            """
            /*
             * Lists within a block quote:
             * > Here's my quoted text.
             * > 1. First item
             * > 2. Second item
             * > 3. Third item
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(40),
            """
            /*
             * Lists within a block quote:
             * > Here's my quoted text.
             * > 1. First item
             * > 2. Second item
             * > 3. Third item
             */
            """.trimIndent()
        )
    }

    @Disabled("Tables are not properly fullsupported")
    @Test
    fun testTables() {
        // Leave formatting within table cells alone
        val source =
            """
            /**
             * ### Tables
             * column 1 | column 2
             * ---------|---------
             * value 1  | value 2
             */
            """.trimIndent()
        checkFormatter(
            source,
            KDocFormattingOptions(40),
            """
            /**
             * ### Tables
             * column 1 | column 2
             * ---------|---------
             * value 1  | value 2
             */
            """.trimIndent()
        )
    }
}
