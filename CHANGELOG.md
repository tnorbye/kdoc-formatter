<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KDoc Formatter Changelog

## [1.6.6]
- IDE-only update: Marked compatible with IntelliJ IDEA 2025.1.
- Updated dependencies

## [1.6.5]
- IDE-only update: Marked compatible with IntelliJ IDEA 2024.3.
- Updated dependencies

## [1.6.4]
- Switch continuation indent from 4 to 3. (IntelliJ's Dokka preview
  treats an indent of 4 or more as preformatted text even on a continued
  line; Dokka itself (and Markdown) does not.
- Add ability to override the continuation indent in the IDE plugin
  settings.
- Don't reorder `@sample` tags (backported
  https://github.com/facebook/ktfmt/issues/406)

## [1.6.3]
- Mark plugin as compatible with K2

## [1.6.2]
- IDE plugin update only: Compatibility with IntelliJ 2024.1 EAP.

## [1.6.1]
- IDE plugin update only.

## [1.6.0]
- Updated dependencies and fixed a few minor bugs, including issue 398
  from ktfmt.

## [1.5.9]
- Compatibility with IntelliJ 2023.1

## [1.5.8]
- Fixed a number of bugs:
   - #84: Line overrun when using closed-open interval notation
   - More gracefully handle unterminated [] references (for example when
     comment is using it in things like [closed, open) intervals)
   - Recognize and convert accidentally capitalized kdoc tags like @See
   - If you have a [ref] which spans a line such that the # ends up as a
     new line, don't treat this as a "# heading".
   - Make sure we don't line break at an expression starting with ">"
     since that would turn into a quoted line.
   - If you're using optimal line breaking and there's a really long,
     unbreakable word in the paragraph, switch that paragraph over to
     greedy line breaking (to make the paragraph better balanced since
     the really long word throws the algorithm off.)
   - Fix a few scenarios where markup conversion from <p> and </p>
     wasn't converting everything.
   - Allow @property[name], not just @param[name]
   - The --hanging-indent flag now also sets the nested list indent
     (if >= 3)
- Some minor code cleanup.

## [1.5.7]
- Fixed the following bugs:
   - #76: Preserve newline style (CRLF on Windows)
   - #77: Preformatting error
   - #78: Preformatting stability
   - #79: Replace `{@param name}` with `[name]`

## [1.5.6]
- Bugfix: the IDE plugin override line width setting was not working

## [1.5.5]
- Fixed the following bugs:
   - #53: The collapse-single-line setting does not work in the IDE
   - #69: Paragraph + list (stability), variation 2
   - #70: Multi-line @link isn't converted if there's a #
   - #71: Make plugin dynamic
   - #72: @param with brackets is not supported
   - A bug where `<p>` paragraphs following a blank line would be
     deleted (without leaving a blank paragraph separator)
- Changed heuristics around optimal or greedy line breaking in list
  items and for KDoc tag and TODO-item paragraph formatting.
- The .editorconfig support is improved. It will now pick up the nearest
  applicable .editorconfig settings for a file, but any options
  explicitly set from the command line will override the .editor config.
   - Also, the "collapse documents that fit on a single line" option,
     instead of just defaulting to true, will now use the default
     specified for the equivalent setting for Java (if set), ensuring a
     more consistent codebase. (You can also set it for Kotlin using
     `ij_kotlin_doc_do_not_wrap_if_one_line`, though that option isn't
     supported in the IDE or by the Kotlin plugin currently.)
- Preliminary support for formatting line comments and block comments
  (enabled via new flags, `--include-line-comments` and
  `--include-block-comments`.)
- Misc IDE plugin improvements
- `<pre>` tags are converted into KDoc preformatted blocks

## [1.5.4]
- Fix 9 bugs filed by the ktfmt project.

## [1.5.3]
- Parameters are reordered to match the order in the corresponding
  method signature.
- In the IDE plugin, there are now options for explicitly specifying the
  line width and the comment width (and some other options cleanup).
- Some fixes to markdown wrapping

## [1.5.2]
- Updates to the IDE plugin to allow configuring maxCommentWidth
  behavior. Fixes https://github.com/tnorbye/kdoc-formatter/issues/53

## [1.5.1]
- Support for tables; by default it will realign columns and add edges,
  but this can be controlled via --align-table-columns and
  --no-align-table-columns. Horizontal padding is added inside the cells
  if there is space within the line.
- Move KDoc tags to the end of comments, and order them (e.g. @param
  before @return and so on). Can be enabled or disabled with
  --order-doc-tags and --no-order-doc-tags.
- Change default maxCommentWidth to 72 (was previously defaulting to the
  maxLineWidth.)
- Fix command line driver to properly handle nested string substitutions
  and to not get confused by single or double quotes backtick quoted
  function names.
- Fix a bug where formatting kdocs that started at the end of lines with
  code was not handled correctly

## [1.5.0]
- A number of bug fixes across the formatter based on running the
  formatter on some larger code bases and inspecting the results, as
  well as diffing the HTML output rendered by Dokka.
- Improved handling for docs with slightly off indentation (e.g. an
  extra space here and there)
- Make sure we never break lines in the middle where the next word is
  ">" (which would be interpreted as a quoted string on the new line) or
  starts with "@" (which will be interpreted as a (possibly unknown)
  kdoc tag.)
- Fix interpretation of nested preformatted text (and revert
  optimization which skipped blank lines between these)
- Don't convert @linkplain tags to KDoc references, since Dokka will not
  render these as {@linkplain}.
- Handle TODO(string), and numbered lists separated with ) instead of .
- Revert the behavior from 1.4.4 which removed blank lines before
  preformatted text where the preformatted text was implicit via
  indentation.

## [1.4.4]
- Fix bug in greedy line breaking which meant some lines were actually
  wider than allowed by the line limit
- Skip markup tag conversion for text inside `backticks` as was already
  done for preformatted text
- For lines that start with "<p>" treat these as a paragraph start, as
  was already the case for lines containing only <p> and drop these if
  markup conversion is enabled.
- Don't add a blank line between text and preformatted text if the
  preceding text ends with a colon or a comma.

## [1.4.3]
- Support for kotlinx-knit markers

## [1.4.2]
- Fix two bugs: one related to nested lists, the other to embedded
  triple-backtick strings on lines

## [1.4.1]
- Fix formatting nested bulleted lists
  (https://github.com/tnorbye/kdoc-formatter/issues/36)

## [1.4.0]
- The IntelliJ plugin now applies KDoc formatting as part of regular
  formatting (Code > Format Code), not just via an explicit action. This
  is optional.
- The markup conversion (if enabled) now converts [] and {@linkplain}
  tags to the KDoc equivalent.
- Fix bug where preformatted text immediately following a TODO comment
  would be joined into the TODO.
- Internally, updated from Java 8 and Kotlin 1.4 to Java 11 and Kotlin
  1.6, various other dependencies, fixed some deprecations, and upgraded
  the plugin build and change log build scripts.

## [1.3.3]
- Bug fix: Avoid line wrapping for text inside square brackets,
  contributed by Daniel Sturm

## [1.3.2]
- A fix for kdoc-formatter issue #23, "Empty lines in preformatted
  paragraphs", contributed by Vittorio Massaro
- A fix for a bug where in some corner cases a word could get dropped.

## [1.3.1]
- Fixes a few bugs around markup conversion not producing the right
  number of blank lines for a <p>, and adds a few more prefixes as
  non-breakable (e.g. if you have an em dash in your sentence -- like
  this -- we don't want the "--" to be placed at the beginning of a
  line.)
- Adds an --add-punctuation command line flag and IDE setting to
  optionally add closing periods on capitalized paragraphs at the end of
  the comment.
- Special cases TODO: comments (placing them in a block by themselves
  and using hanging indents).

## [1.3.0]
- Many improves to the markdown handling, such as quoted blocks,
  headers, list continuations, etc.
- Markup conversion, which until this point could convert inline tags
  such as **bold** into **bold**, etc, now handles many block level tags
  too, such as \<p>, \<h1>, etc.
- The IDE plugin can now also reformat line comments under the caret.
  (This is opt-in via options.)

## [1.2.0]
- This version adds a settings panel to the IDE plugin where you can
  configure whether the plugin will alternate formatting modes on
  repeated invocation, as well as whether single line comments should be
  collapsed and whether to convert markup like bold to bold. (Note that
  line lengths will just use the IDE code style settings or
  .editorconfig files).
- It also improves the code which preserves the caret across formatting
  actions to be a bit more accurate.

## [1.1.2]
- This version adds basic support for .editorconfig files. It will use
  the configured line width for Kotlin source files, and it will also
  pick up the Markdown line length and set that as a maximum line width.

## [1.1.1]
- Bug fix for combination of --overlaps-git-changes=staged and relative
  paths.
- Also bails quickly if the given git commit contains no Kotlin source
  files.

## [1.1.0]
- This version adds support for --max-comment-width, and for the Gradle
  plugin to be able to supply options (via kdocformatter.options =
  "--max-line-width=100 --max-comment-width=72" etc.)
- It changes the Gradle plugin group id (since the previous one was
  rejected by Sonatype) and tweaks a few minor things.

## [1.0.0]
- Command line script which can recursively format a whole source folder
- IDE plugin to format selected files or current comment. Preserves
  caret position in the current comment.
- Gradle plugin to format the source folders in the current project.
- Block tags (like @param) are separated out from the main text, and
  subsequent lines are indented. Blank spaces between doc tags are
  removed. Preformatted text (indented 4 spaces or more) is left alone.
- Can be run in a mode where it only reformats comments that were
  touched by the current git HEAD commit, or the currently staged files.
  Can also be passed specific line ranges to limit formatting to.
- Multiline comments that would fit on a single line are converted to a
  single line comment (configurable via options)
- Adds hanging indents for ordered and unordered indents.
- Cleans up the double spaces left by the IntelliJ "Convert to Kotlin"
  action right before the closing comment token.
- Removes trailing spaces.