<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KDoc Formatter Changelog

## [1.5.2]
- Updates to the IDE plugin to allow configuring maxCommentWidth
  behavior. Fixes https://github.com/tnorbye/kdoc-formatter/issues/53

## [1.5.1]
- Support for tables; by default it will realign columns and
  add edges, but this can be controlled via
  --align-table-columns and --no-align-table-columns.
  Horizontal padding is added inside the cells if there is
  space within the line.
- Move KDoc tags to the end of comments, and order them
  (e.g. @param before @return and so on). Can be enabled
  or disabled with --order-doc-tags and
  --no-order-doc-tags.
- Change default maxCommentWidth to 72 (was previously defaulting
  to the maxLineWidth.)
- Fix command line driver to properly handle nested
  string substitutions and to not get confused by single or
  double quotes backtick quoted function names.
- Fix a bug where formatting kdocs that started at the end of
  lines with code was not handled correctly

## [1.5.0]
- A number of bug fixes across the formatter based on running
  the formatter on some larger code bases and inspecting
  the results, as well as diffing the HTML output rendered by
  Dokka.
- Improved handling for docs with slightly off indentation
  (e.g. an extra space here and there)
- Make sure we never break lines in the middle where the
  next word is ">" (which would be interpreted as a quoted
  string on the new line) or starts with "@" (which will be
  interpreted as a (possibly unknown) kdoc tag.)
- Fix interpretation of nested preformatted text (and revert
  optimization which skipped blank lines between these)
- Don't convert @linkplain tags to KDoc references, since Dokka
  will not render these as {@linkplain}.
- Handle TODO(string), and numbered lists separated with )
  instead of .
- Revert the behavior from 1.4.4 which removed blank lines
  before preformatted text where the preformatted text was
  implicit via indentation.

## [1.4.4]
- Fix bug in greedy line breaking which meant some lines were
  actually wider than allowed by the line limit
- Skip markup tag conversion for text inside `backticks` as
  was already done for preformatted text
- For lines that start with "<p>" treat these as a paragraph
  start, as was already the case for lines containing only <p>
  and drop these if markup conversion is enabled.
- Don't add a blank line between text and preformatted text
  if the preceding text ends with a colon or a comma.

## [1.4.3]
- Support for kotlinx-knit markers

## [1.4.2]

- Fix two bugs: one related to nested lists, the other
  to embedded triple-backtick strings on lines

## [1.4.1]

- Fix formatting nested bulleted lists
  (https://github.com/tnorbye/kdoc-formatter/issues/36)

## [1.4.0]

- The IntelliJ plugin now applies KDoc formatting as part of
  regular formatting (Code > Format Code), not just via an explicit
  action. This is optional.
- The markup conversion (if enabled) now converts {@link} and {@linkplain}
  tags to the KDoc equivalent.
- Fix bug where preformatted text immediately following a TODO
  comment would be joined into the TODO.
- Internally, updated from Java 8 and Kotlin 1.4 to Java 11 and
  Kotlin 1.6, various other dependencies, fixed some deprecations,
  and upgraded the plugin build and change log build scripts.

## [1.3.3]

- Bug fix: Avoid line wrapping for text inside square brackets,
  contributed by Daniel Sturm

## [1.3.2]

- A fix for kdoc-formatter issue #23, "Empty lines in preformatted
  paragraphs", contributed by Vittorio Massaro
- A fix for a bug where in some corner cases a word could get dropped.

## [1.3.1]

- Fixes a few bugs around markup conversion not producing the right
  number of blank lines for a <p>, and adds a few more prefixes
  as non-breakable (e.g. if you have an em dash in your sentence --
  like this -- we don't want the "--" to be placed at the beginning of
  a line.)
- Adds an --add-punctuation command line flag and IDE setting to
  optionally add closing periods on capitalized paragraphs at the end
  of the comment.
- Special cases TODO: comments (placing them in a block by themselves
  and using hanging indents).

## [1.3.0]

- Many improves to the markdown handling, such as quoted blocks,
  headers, list continuations, etc.
- Markup conversion, which until this point could convert inline tags
  such as **bold** into **bold**, etc, now handles
  many block level tags too, such as \<p>, \<h1>, etc.
- The IDE plugin can now also reformat line comments under the caret.
  (This is opt-in via options.)

## [1.2.0]

- This version adds a settings panel to the IDE plugin where you can
  configure whether the plugin will alternate formatting modes on
  repeated invocation, as well as whether single line comments should
  be collapsed and whether to convert markup like bold to bold. (Note
  that line lengths will just use the IDE code style settings or
  .editorconfig files).
- It also improves the code which preserves the caret across formatting
  actions to be a bit more accurate.

## [1.1.2]

- This version adds basic support for .editorconfig files.
  It will use the configured line width for Kotlin source files, and it
  will also pick up the Markdown line length and set that as a maximum
  line width.

## [1.1.1]

- Bug fix for combination of --overlaps-git-changes=staged and relative
  paths.
- Also bails quickly if the given git commit contains no Kotlin
  source files.

## [1.1.0]

- This version adds support for --max-comment-width, and for the Gradle
  plugin to be able to supply options (via kdocformatter.options
  = "--max-line-width=100 --max-comment-width=72" etc.)
- It changes the Gradle plugin group id (since the previous one
  was rejected by Sonatype) and tweaks a few minor things.

## [1.0.0]

- Command line script which can recursively format a whole source
  folder
- IDE plugin to format selected files or current comment. Preserves
  caret position in the current comment.
- Gradle plugin to format the source folders in the current project.
- Block tags (like @param) are separated out from the main text, and
  subsequent lines are indented. Blank spaces
  between doc tags are removed. Preformatted text
  (indented 4 spaces or more) is left alone.
- Can be run in a mode where it only reformats comments that were
  touched by the current git HEAD commit, or the
  currently staged files. Can also be passed
  specific line ranges to limit formatting to.
- Multiline comments that would fit on a single line are converted to
  a single line comment (configurable via options)
- Adds hanging indents for ordered and unordered indents.
- Cleans up the double spaces left by the IntelliJ "Convert to
  Kotlin" action right before the closing comment token.
- Removes trailing spaces.
