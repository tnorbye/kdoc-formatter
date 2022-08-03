package kdocformatter.cli

import java.io.File
import kdocformatter.KDocFormattingOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The formatter is mostly tested by KDocFormatterTest. This tests the
// part where we (1) find the comments and format them, and (2) process
// the code outside the comments and stitch it all together with the right
// indentation etc.
class KDocFileFormatterTest {
    private fun reformatFile(
        source: String,
        options: KDocFormattingOptions,
        fileOptions: KDocFileFormattingOptions = KDocFileFormattingOptions(),
        markdown: Boolean = false,
    ): String {
        fileOptions.apply {
            formattingOptions = options
            includeMd = markdown
        }
        val formatter = KDocFileFormatter(fileOptions)
        val file = if (markdown) File("test.md") else File("test.kt")
        val reformatted = formatter.reformatFile(file, source.trim())
        // Make sure that formatting is stable -- format again and make sure it's
        // the same
        val reformattedAgain = formatter.reformatFile(file, reformatted.trim())
        if (reformatted != reformattedAgain) {
            assertEquals(
                "Formatting is not stable; reformatting breaks it",
                reformatted,
                reformattedAgain
            )
        }
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
    fun testBlockComment() {
        val source =
            """
            @file:JvmName("Constraints")

            /* Copyright notice.
             We don't touch this.
             */
            class Test {
                /*
                * Returns whether lint should check all warnings,
                 * including those off by default, or null if
                 *not configured in this configuration. This is a really really really long sentence which needs to be broken up.
                 *
                 * Separate paragraph.
                 */
                 // We're not
                 //  reformatting
                 // line comments in this test
            }
            """.trimIndent()

        val reformatted =
            reformatFile(
                source,
                KDocFormattingOptions(50),
                KDocFileFormattingOptions().apply { blockComments = true }
            )
        assertEquals(
            """
            @file:JvmName("Constraints")

            /* Copyright notice.
             We don't touch this.
             */
            class Test {
                /*
                Returns whether lint should check all
                warnings, including those off by default, or
                null if not configured in this configuration.
                This is a really really really long
                sentence which needs to be broken up.

                Separate paragraph.
                */
                 // We're not
                 //  reformatting
                 // line comments in this test
            }
            """.trimIndent(),
            reformatted
        )
    }

    @Test
    fun testFit() {
        val source =
            """
            class Test {
               ...
                                ":app:writeDebugSigningConfigVersions", /** Intentionally not cacheable. See [com.android.build.gradle.internal.tasks.SigningConfigVersionsWriterTask] */

                                ":feature1:checkDebugAarMetadata",
            }
            """.trimIndent()
        val reformatted = reformatFile(source, KDocFormattingOptions(72))
        assertEquals(
            """
            class Test {
               ...
                                ":app:writeDebugSigningConfigVersions",
                                /**
                                 * Intentionally not cacheable. See
                                 * [com.android.build.gradle.internal.tasks.SigningConfigVersionsWriterTask]
                                 */

                                ":feature1:checkDebugAarMetadata",
            }
            """.trimIndent(),
            reformatted
        )
    }

    @Test
    fun testCrLf() {
        // https://github.com/tnorbye/kdoc-formatter/issues/76
        val source =
            """
            /**
             * Sample summary.
             *
             * More summary.
             */
            """
                .trimIndent()
                .replace("\n", "\r\n")
        val reformatted = reformatFile(source, KDocFormattingOptions(72))
        assertEquals(source, reformatted)
    }

    @Test
    fun testLineComment() {
        val source =
            """
            class Test {
                /*
                *  we're not reformatting
                *   block comments
                 * in this test
                 */
                 // We *ARE*
                 //  reformatting
                 // line comments in this test
                 var x: String = 0; // Some
                 //  more comments here that won't fit on @the same line.
                 var y: String = 0; // Some
                                    //  more comments here that won't fit [on
                                    // the   same
                                    // line].

                 // This is separate

                 fun test() {
                    if (lineWithIndentation.startsWith("    ") && // markdown preformatted text
                        (i == 1 || lineContent(lines[i - 2]).isBlank()) && // we've already ++'ed i above.
                            // Make sure it's not just deeply indented inside a different block
                            (paragraph.prev == null)
                 }
            }
            """.trimIndent()

        val reformatted =
            reformatFile(
                source,
                KDocFormattingOptions(50),
                KDocFileFormattingOptions().apply { lineComments = true }
            )
        assertEquals(
            """
            class Test {
                /*
                *  we're not reformatting
                *   block comments
                 * in this test
                 */
                 // We *ARE* reformatting line comments in
                 // this test
                 var x: String = 0; // Some more comments here
                 // that won't fit on @the same line.
                 var y: String = 0; // Some more comments here
                                    // that won't fit [on the
                                    // same line].

                 // This is separate

                 fun test() {
                    if (lineWithIndentation.startsWith("    ") &&
                        // markdown preformatted text
                        (i == 1 || lineContent(lines[i - 2]).isBlank()) &&
                            // we've already ++'ed i above.
                            // Make sure it's not just deeply
                            // indented inside a different
                            // block
                            (paragraph.prev == null)
                 }
            }
            """.trimIndent(),
            reformatted
        )
    }

    @Test
    fun testReorderParameters() {
        val source =
            """
            class Test {
                /** My comment
                 * @param third Description of third parameter
                 * @param   second Description of second parameter
                 * @param first   Description of first parameter */
                @Suppress("all")
                fun test(first: String, @Suppress("unused") second: String, vararg third: String): String = "hello world"

                /**
                 * @param fourth Desc 4
                 * @param third Desc 3
                 * @param second Desc 2
                 * @param first Desc 1
                 */
                fun `test(third)`(
                    first: MutableMap<String, MutableList<String>>?,
                    // fourth: Boolean = false,
                    second: Boolean = false,
                    third: String = "fun(test: String, test2: String)",
                    /* , first: String */
                    fourth: (Collection<String>) -> Unit = {}
                ) {
                }

                /**
                 * (Signature and doc list different parameters)
                 * @param foo Desc 1
                 * @param baz Desc 2
                 */
                fun test2(foo: String, bar: String) {
                }
            }
            """.trimIndent()
        val reformatted =
            reformatFile(source, KDocFormattingOptions(72).apply { orderDocTags = true })
        assertEquals(
            """
            class Test {
                /**
                 * My comment
                 *
                 * @param first Description of first parameter
                 * @param second Description of second parameter
                 * @param third Description of third parameter
                 */
                @Suppress("all")
                fun test(first: String, @Suppress("unused") second: String, vararg third: String): String = "hello world"

                /**
                 * @param first Desc 1
                 * @param second Desc 2
                 * @param third Desc 3
                 * @param fourth Desc 4
                 */
                fun `test(third)`(
                    first: MutableMap<String, MutableList<String>>?,
                    // fourth: Boolean = false,
                    second: Boolean = false,
                    third: String = "fun(test: String, test2: String)",
                    /* , first: String */
                    fourth: (Collection<String>) -> Unit = {}
                ) {
                }

                /**
                 * (Signature and doc list different parameters)
                 *
                 * @param foo Desc 1
                 * @param baz Desc 2
                 */
                fun test2(foo: String, bar: String) {
                }
            }
            """.trimIndent(),
            reformatted.trim()
        )
    }

    @Test
    fun testLineWidth() {
        // Perform in KDocFileFormatter test too to make sure we properly account
        // for indent!
        val source =
            """
            //3456789012345678901234567890 <- 30
            /**
             * This should fit on a single
             *
             * And this should also fit!!
             *
             * And this should not!!!!!!!!!
             */
            """.trimIndent()

        val reformatted =
            """
            //3456789012345678901234567890 <- 30
            /**
             * This should fit on a single
             *
             * And this should also fit!!
             *
             * And this should
             * not!!!!!!!!!
             */
            """.trimIndent()

        assertEquals(
            reformatted,
            reformatFile(source, KDocFormattingOptions(30).apply { optimal = true })
        )

        assertEquals(
            reformatted,
            reformatFile(source, KDocFormattingOptions(30).apply { optimal = false })
        )
    }

    @Test
    fun testGreedyIndent() {
        val source =
            "" +
                "class FormatterTest {\n" +
                "  /**\n" +
                "   * Handles a chain of qualified expressions, i.e. `a[5].b!!.c()[4].f()`\n" +
                "   *\n" +
                "   * This is by far the most complicated part of this formatter. We start by breaking the expression\n" +
                "   * to the steps it is executed in (which are in the opposite order of how the syntax tree is\n" +
                "   * built).\n" +
                "   *\n" +
                "   * We then calculate information to know which parts need to be groups, and finally go part by\n" +
                "   * part, emitting it to the [builder] while closing and opening groups.\n" +
                "   */\n" +
                "  private fun emitQualifiedExpression(expression: Any) {\n" +
                "  }\n" +
                "}"
        val reformatted =
            reformatFile(source, KDocFormattingOptions(100, 100).apply { optimal = false })
        assertEquals(source, reformatted)
    }

    @Test
    fun testStringInterpolation() {
        val source =
            "val t = \"\"\"This is a raw string! \${'\"'.minus(50) /**  KDoc  comment */ }string\"\"\""
        val reformatted = reformatFile(source, KDocFormattingOptions(100, 72))
        assertEquals(
            "val t = \"\"\"This is a raw string! \${'\"'.minus(50) /** KDoc comment */ }string\"\"\"",
            reformatted
        )
    }

    @Test
    fun testLexer() {
        val source =
            "" +
                "class Test {\n" +
                "  @Test\n" +
                "  fun `There's something going on`() {}\n" +
                "  fun test2() =\n" +
                "      test(\n" +
                "          \"\"\"\n" +
                "      |// Comment 1\n" +
                "      |// There's something else going on.\n" +
                "      |\"\"\")\n" +
                "\n" +
                "  fun test3() {\n" +
                "    val s =\n" +
                "        \"\"\"\n" +
                "          |/**\n" +
                "          | * @throws Exception\n" +
                "          | * @exception Exception\n" +
                "          | * @param unused [Param]\n" +
                "          | */\n" +
                "          |class Sample\n" +
                "          |\"\"\"\n" +
                "  }\n" +
                "}"
        val reformatted = reformatFile(source, KDocFormattingOptions(100, 72))
        assertEquals(source, reformatted)
    }

    @Test
    fun testLineSuffixFits() {
        val source =
            """
            class ComposeIssueNotificationAction(
              private val createInformationPopup: (Project, ComposePreviewManager, DataContext) -> InformationPopup = ::defaultCreateInformationPopup)
              : AnAction(), RightAlignedToolbarAction, CustomComponentAction, Disposable {  /**
               * [Alarm] used to trigger the popup as a hint.
               */
              private val popupAlarm = Alarm()
            }
            """.trimIndent()
        val reformatted = reformatFile(source, KDocFormattingOptions(1000, 1000))
        assertEquals(
            """
            class ComposeIssueNotificationAction(
              private val createInformationPopup: (Project, ComposePreviewManager, DataContext) -> InformationPopup = ::defaultCreateInformationPopup)
              : AnAction(), RightAlignedToolbarAction, CustomComponentAction, Disposable {  /** [Alarm] used to trigger the popup as a hint. */
              private val popupAlarm = Alarm()
            }
            """.trimIndent(),
            reformatted
        )
    }

    @Test
    fun testLineSuffixDoesNotFit() {
        val source =
            """
            class ComposeIssueNotificationAction(
              private val createInformationPopup: (Project, ComposePreviewManager, DataContext) -> InformationPopup = ::defaultCreateInformationPopup)
              : AnAction(), RightAlignedToolbarAction, CustomComponentAction, Disposable {  /**
               * [Alarm] used to trigger the popup as a hint.
               */
              private val popupAlarm = Alarm()
            }
            """.trimIndent()

        assertEquals(
            """
            class ComposeIssueNotificationAction(
              private val createInformationPopup: (Project, ComposePreviewManager, DataContext) -> InformationPopup = ::defaultCreateInformationPopup)
              : AnAction(), RightAlignedToolbarAction, CustomComponentAction, Disposable {
              /** [Alarm] used to trigger the popup as a hint. */
              private val popupAlarm = Alarm()
            }
            """.trimIndent(),
            reformatFile(source, KDocFormattingOptions(60, 60))
        )

        assertEquals(
            """
            class ComposeIssueNotificationAction(
              private val createInformationPopup: (Project, ComposePreviewManager, DataContext) -> InformationPopup = ::defaultCreateInformationPopup)
              : AnAction(), RightAlignedToolbarAction, CustomComponentAction, Disposable {
              /**
               * [Alarm] used to trigger the popup as a hint.
               */
              private val popupAlarm = Alarm()
            }
            """.trimIndent(),
            reformatFile(
                source,
                KDocFormattingOptions(100, 72).apply { collapseSingleLine = false }
            )
        )
    }

    @Test
    fun testIssue42() {
        // Regression test for https://github.com/tnorbye/kdoc-formatter/issues/42
        val source =
            """
            @Suppress("SpellCheckingInspection")
            /**
             * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
             */
            fun KDocFormatterTest() {
            }
            """.trimIndent()

        assertEquals(source, reformatFile(source, KDocFormattingOptions(60, 60)))
    }

    @Test
    fun testGreedyVsOptimal() {
        val source =
            """
            # KDoc Formatter Plugin Changelog

            ## [1.5.5]
            - Improved support for .editorconfig files; these settings will now be
              reflected immediately (in prior versions you had to restart the IDE
              because they were improperly cached)
            - Fixed a copy/paste bug which prevented the "Collapse short comments
              that fit on a single line" option from working.
            """.trimIndent()

        assertEquals(source, reformatFile(source, KDocFormattingOptions(72), markdown = true))
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

        val diff =
            """
            diff --git a/README.md b/README.md
            index c26815b..30a8dbb 100644
            --- README.md
            +++ README.md
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
            --- README.md
            +++ README.md
            @@ -31,25 +31,29 @@ ${'$'} kdoc-formatter
             Usage: kdoc-formatter [options] file(s)
            """.trimIndent()

        val fileOptions = KDocFileFormattingOptions()
        val root = File("").canonicalFile
        val file = File(root, "Test.kt")
        fileOptions.filter = GitRangeFilter.create(root, diff)
        assertTrue(fileOptions.filter.includes(file))
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

    @Test
    fun testFormatMd() {
        val source =
            """
            KDoc Formatter
            ==============

            Reformats Kotlin KDoc comments, reflowing text and other cleanup, both
            via IDE plugin and command line utility.

            This tool reflows comments in KDoc; either on a file or recursively
            over nested folders, as well as an IntelliJ IDE plugin where you can
            reflow the current comment around the cursor.

            Features
            --------

            * Reflow using optimal instead of greedy algorithm (though in the IDE
              plugin you can turn on alternate formatting and invoking the action
              repeatedly alternates between the two modes.)

            * Command line script which can recursively format a whole source
              folder.

            * IDE plugin to format selected files or current comment. Preserves
              caret position in the current comment.

            * Gradle plugin to format the source folders in the current project.

            * Block tags (like @param) are separated out from the main text, and
              subsequent lines are indented. Blank spaces between doc tags
              are removed. Preformatted text (indented 4 spaces or more) is left
              alone.

            * Can be run in a mode where it only reformats comments that were
              touched by the current git HEAD commit, or the currently staged
              files. Can also be passed specific line ranges to limit formatting
              to.

            * Multiline comments that would fit on a single line are converted to
              a single line comment (configurable via options)

            * Adds hanging indents for ordered and unordered indents.

            * Cleans up the double spaces left by the IntelliJ "Convert to Kotlin"
              action right before the closing comment token.

            * Removes trailing spaces.

            * Can optionally convert various remaining HTML tags in the comments
              to the corresponding KDoc/markdown text. For example, <b>bold</b> is
              converted into **bold**, <p> is converted to a blank line,
              \<h1>Heading</h1> is converted into # Heading, and so on.

            * Support for .editorconfig configuration files to automatically pick
              up line widths. It will normally use the line width configured for
              Kotlin files, but, if Markdown (.md) files are also configured, it
              will use that width as the maximum comment width. This allows you to
              have code line widths of for example 140 but limit comments to 70
              characters (possibly indented). For code, avoiding line breaking is
              helpful, but for text, shorter lines are better for reading.

            Command Usage
            -------------
            ```
            ${'$'} kdoc-formatter
            Usage: kdoc-formatter [options] file(s)

            Options:
              --max-line-width=<n>
                Sets the length of lines. Defaults to 72.
              --max-comment-width=<n>
                Sets the maximum width of comments. This is helpful in a codebase
                with large line lengths, such as 140 in the IntelliJ codebase. Here,
                you don't want to limit the formatter maximum line width since
                indented code still needs to be properly formatted, but you also
                don't want comments to span 100+ characters, since that's less
                readable. By default this option is not set.
              --hanging-indent=<n>
                Sets the number of spaces to use for hanging indents, e.g. second
                and subsequent lines in a bulleted list or kdoc blog tag.
              --convert-markup
                Convert unnecessary HTML tags like &lt; and &gt; into < and >
              --single-line-comments=<collapse | expand>
                With `collapse`, turns multi-line comments into a single line if it
                fits, and with `expand` it will always format commands with /** and
                */ on their own lines. The default is `collapse`.
              --overlaps-git-changes=<HEAD | staged>
                If git is on the path, and the command is invoked in a git
                repository, kdoc-formatter will invoke git to find the changes either
                in the HEAD commit or in the staged files, and will format only the
                KDoc comments that overlap these changes.
              --lines <start:end>, --line <start>
                Line range(s) to format, like 5:10 (1-based; default is all). Can be
                specified multiple times.
              --dry-run, -n
                Prints the paths of the files whose contents would change if the
                formatter were run normally.
              --quiet, -q
                Quiet mode
              --help, -help, -h
                Print this usage statement.
              @<filename>
                Read filenames from file.

            kdoc-formatter: Version 1.3
            https://github.com/tnorbye/kdoc-formatter
            ```

            IntelliJ Plugin Usage
            ---------------------
            Install the IDE plugin. Then move the caret to a KDoc comment and invoke
            Code > Reformat KDoc. You can configure a keyboard shortcut if you perform
            this action frequently (go to Preferences, search for Keymap, and then
            in the Keymap search field look for "KDoc", and then double click and
            choose Add Keyboard Shortcut.

            You can also select one or more files in the
            Project View and invoke the same action to format whole files.

            ![Screenshot](screenshot.png)

            Finally, you can configure various options in the Settings panel.
            The line length settings are inherited from the IDE code style or
            from the .editorconfig files, if any. However, you can turn on
            "alternate" mode where invoking the action repeatedly will toggle
            between normal formatting and alternate formatting each time you
            invoke it. For a short comment that means toggling between a
            multi-line and a single-line comment. But for a longer comment,
            it will toggle between optimal line breaking (the default) and
            greedy line breaking, which can look better for short paragraphs.

            You can also configure whether the formatter should do more than
            formatting and actually replace markup constructs like <b>bold</b>
            with markdown markup.

            ![Screenshot](screenshot-settings.png)


            The plugin is available from the JetBrains Marketplace at
            [https://plugins.jetbrains.com/plugin/15734-kotlin-kdoc-formatter](https://plugins.jetbrains.com/plugin/15734-kotlin-kdoc-formatter)

            Gradle Plugin Usage
            -------------------
            The plugin is not yet distributed, so for now, download the zip file
            and install it somewhere, then add this to your build.gradle file:

            ```
            buildscript {
                repositories {
                    maven { url '/path/to/m2' }
                }
                dependencies {
                    classpath "com.github.tnorbye.kdoc-formatter:kdocformatter:1.1.1"
                    // (Sorry about the vanity URL --
                    // I tried to get kdoc-formatter:kdoc-formatter:1.1.1 but that
                    // didn't meet the naming requirements for publishing:
                    // https://issues.sonatype.org/browse/OSSRH-63191)
                }
            }
            plugins {
                id 'kdoc-formatter'
            }
            kdocformatter {
                options = "--single-line-comments=collapse --max-line-width=100"
            }
            ```
            Here, the [options] property lets you use any of the command line flags
            from the kdoc-formatter command.

            Building and testing
            --------------------
            To create an installation of the command line tool, run
            ```
            ./gradlew install
            ```
            The installation will be located in cli/build/install/kdocformatter.

            To create a zip, run
            ```
            ./gradlew zip
            ```
            To build the plugin, run
            ```
            ./gradlew :plugin:buildPlugin
            ```
            The plugin will be located in plugin/build/distributions/.

            To run/test the plugin in the IDE, run
            ```
            ./gradlew runIde
            ```

            To reformat the source tree run
            ```
            ./gradlew format
            ```

            To build the Gradle plugin locally:
            ```
            cd gradle-plugin
            ./gradlew publish
            ```
            This will create a Maven local repository in m2/ which you can then
            point to from your consuming projects as shown in the Gradle Plugin
            Usage section above.

            Support Javadoc?
            ----------------
            KDoc is pretty similar to javadoc and there's a good chance that most
            of this functionality would work well. However, I already use
            [google-java-formatter](https://github.com/google/google-java-format)
            to format all Java source code, which does a great job reflowing
            javadoc comments already (along with formatting the rest of the file),
            so makign this tool support Java is not needed.

            Integrate into ktlint?
            ----------------------
            I use [ktlint](https://github.com/pinterest/ktlint) to format and
            pretty-print my Kotlin source code.  However, it does not do comment
            reformatting, which means I spend time either manually reflowing
            myself when I edit comments, or worse, leave it unformatted.

            Given that I use ktlint for formatting, the Right Thing would have
            been for me to figure out how it works, and implement the
            functionality there. However, I'm busy with a million other things,
            and this was just a quick weekend -- which unfortunately satisfies my
            immediate formatting needs -- so I no longer have the same motivation
            to get ktlint to support it.
            """.trimIndent()

        val options = KDocFormattingOptions(72).apply { optimal = false }
        val reformatted = reformatFile(source, options, markdown = true)
        assertEquals(
            """
            KDoc Formatter
            ==============

            Reformats Kotlin KDoc comments, reflowing text and other cleanup, both
            via IDE plugin and command line utility.

            This tool reflows comments in KDoc; either on a file or recursively over
            nested folders, as well as an IntelliJ IDE plugin where you can reflow
            the current comment around the cursor.

            Features
            --------
            * Reflow using optimal instead of greedy algorithm (though in the IDE
              plugin you can turn on alternate formatting and invoking the action
              repeatedly alternates between the two modes.)
            * Command line script which can recursively format a whole source
              folder.
            * IDE plugin to format selected files or current comment. Preserves
              caret position in the current comment.
            * Gradle plugin to format the source folders in the current project.
            * Block tags (like @param) are separated out from the main text, and
              subsequent lines are indented. Blank spaces between doc tags are
              removed. Preformatted text (indented 4 spaces or more) is left alone.
            * Can be run in a mode where it only reformats comments that were
              touched by the current git HEAD commit, or the currently staged files.
              Can also be passed specific line ranges to limit formatting to.
            * Multiline comments that would fit on a single line are converted to a
              single line comment (configurable via options)
            * Adds hanging indents for ordered and unordered indents.
            * Cleans up the double spaces left by the IntelliJ "Convert to Kotlin"
              action right before the closing comment token.
            * Removes trailing spaces.
            * Can optionally convert various remaining HTML tags in the comments to
              the corresponding KDoc/markdown text. For example, **bold** is
              converted into **bold**, <p> is converted to a blank line,
              \<h1>Heading</h1> is converted into # Heading, and so on.
            * Support for .editorconfig configuration files to automatically pick up
              line widths. It will normally use the line width configured for Kotlin
              files, but, if Markdown (.md) files are also configured, it will use
              that width as the maximum comment width. This allows you to have code
              line widths of for example 140 but limit comments to 70 characters
              (possibly indented). For code, avoiding line breaking is helpful, but
              for text, shorter lines are better for reading.

            Command Usage
            -------------
            ```
            ${'$'} kdoc-formatter
            Usage: kdoc-formatter [options] file(s)

            Options:
              --max-line-width=<n>
                Sets the length of lines. Defaults to 72.
              --max-comment-width=<n>
                Sets the maximum width of comments. This is helpful in a codebase
                with large line lengths, such as 140 in the IntelliJ codebase. Here,
                you don't want to limit the formatter maximum line width since
                indented code still needs to be properly formatted, but you also
                don't want comments to span 100+ characters, since that's less
                readable. By default this option is not set.
              --hanging-indent=<n>
                Sets the number of spaces to use for hanging indents, e.g. second
                and subsequent lines in a bulleted list or kdoc blog tag.
              --convert-markup
                Convert unnecessary HTML tags like &lt; and &gt; into < and >
              --single-line-comments=<collapse | expand>
                With `collapse`, turns multi-line comments into a single line if it
                fits, and with `expand` it will always format commands with /** and
                */ on their own lines. The default is `collapse`.
              --overlaps-git-changes=<HEAD | staged>
                If git is on the path, and the command is invoked in a git
                repository, kdoc-formatter will invoke git to find the changes either
                in the HEAD commit or in the staged files, and will format only the
                KDoc comments that overlap these changes.
              --lines <start:end>, --line <start>
                Line range(s) to format, like 5:10 (1-based; default is all). Can be
                specified multiple times.
              --dry-run, -n
                Prints the paths of the files whose contents would change if the
                formatter were run normally.
              --quiet, -q
                Quiet mode
              --help, -help, -h
                Print this usage statement.
              @<filename>
                Read filenames from file.

            kdoc-formatter: Version 1.3
            https://github.com/tnorbye/kdoc-formatter
            ```

            IntelliJ Plugin Usage
            ---------------------
            Install the IDE plugin. Then move the caret to a KDoc comment and invoke
            Code > Reformat KDoc. You can configure a keyboard shortcut if you
            perform this action frequently (go to Preferences, search for Keymap,
            and then in the Keymap search field look for "KDoc", and then double
            click and choose Add Keyboard Shortcut.

            You can also select one or more files in the Project View and invoke the
            same action to format whole files.

            ![Screenshot](screenshot.png)

            Finally, you can configure various options in the Settings panel. The
            line length settings are inherited from the IDE code style or from the
            .editorconfig files, if any. However, you can turn on "alternate" mode
            where invoking the action repeatedly will toggle between normal
            formatting and alternate formatting each time you invoke it. For a short
            comment that means toggling between a multi-line and a single-line
            comment. But for a longer comment, it will toggle between optimal line
            breaking (the default) and greedy line breaking, which can look better
            for short paragraphs.

            You can also configure whether the formatter should do more than
            formatting and actually replace markup constructs like **bold** with
            markdown markup.

            ![Screenshot](screenshot-settings.png)

            The plugin is available from the JetBrains Marketplace at
            [https://plugins.jetbrains.com/plugin/15734-kotlin-kdoc-formatter](https://plugins.jetbrains.com/plugin/15734-kotlin-kdoc-formatter)

            Gradle Plugin Usage
            -------------------
            The plugin is not yet distributed, so for now, download the zip file and
            install it somewhere, then add this to your build.gradle file:
            ```
            buildscript {
                repositories {
                    maven { url '/path/to/m2' }
                }
                dependencies {
                    classpath "com.github.tnorbye.kdoc-formatter:kdocformatter:1.1.1"
                    // (Sorry about the vanity URL --
                    // I tried to get kdoc-formatter:kdoc-formatter:1.1.1 but that
                    // didn't meet the naming requirements for publishing:
                    // https://issues.sonatype.org/browse/OSSRH-63191)
                }
            }
            plugins {
                id 'kdoc-formatter'
            }
            kdocformatter {
                options = "--single-line-comments=collapse --max-line-width=100"
            }
            ```

            Here, the [options] property lets you use any of the command line flags
            from the kdoc-formatter command.

            Building and testing
            --------------------
            To create an installation of the command line tool, run

            ```
            ./gradlew install
            ```

            The installation will be located in cli/build/install/kdocformatter.

            To create a zip, run

            ```
            ./gradlew zip
            ```

            To build the plugin, run

            ```
            ./gradlew :plugin:buildPlugin
            ```

            The plugin will be located in plugin/build/distributions/.

            To run/test the plugin in the IDE, run

            ```
            ./gradlew runIde
            ```

            To reformat the source tree run

            ```
            ./gradlew format
            ```

            To build the Gradle plugin locally:
            ```
            cd gradle-plugin
            ./gradlew publish
            ```

            This will create a Maven local repository in m2/ which you can then
            point to from your consuming projects as shown in the Gradle Plugin
            Usage section above.

            Support Javadoc?
            ----------------
            KDoc is pretty similar to javadoc and there's a good chance that most of
            this functionality would work well. However, I already use
            [google-java-formatter](https://github.com/google/google-java-format) to
            format all Java source code, which does a great job reflowing javadoc
            comments already (along with formatting the rest of the file), so makign
            this tool support Java is not needed.

            Integrate into ktlint?
            ----------------------
            I use [ktlint](https://github.com/pinterest/ktlint) to format and
            pretty-print my Kotlin source code. However, it does not do comment
            reformatting, which means I spend time either manually reflowing myself
            when I edit comments, or worse, leave it unformatted.

            Given that I use ktlint for formatting, the Right Thing would have been
            for me to figure out how it works, and implement the functionality
            there. However, I'm busy with a million other things, and this was just
            a quick weekend -- which unfortunately satisfies my immediate formatting
            needs -- so I no longer have the same motivation to get ktlint to
            support it.
            """.trimIndent(),
            reformatted
        )
    }
}
