Version 1.3
===========

This version contains the following improvements:

* Many improves to the markdown handling, such as quoted
  blocks, headers, list continuations, etc.

* Markup conversion, which until this point could convert inline tags
  such as <b>bold</b> into **bold**, etc, now handles many block level
  tags too, such as <p>, <h1>, etc.

* The IDE plugin can now also reformat line comments under the
  caret. (This is opt-in via options.)

Version 1.2
===========

This version adds a settings panel to the IDE plugin where you can
configure whether the plugin will alternate formatting modes on
repeated invocation, as well as whether single line comments should be
collapsed and whether to convert markup like bold to bold. (Note that
line lengths will just use the IDE code style settings or
.editorconfig files).

It also improves the code which preserves the caret across formatting
actions to be a bit more accurate.

Version 1.1.2
=============

This version adds basic support for .editorconfig files.

It will use the configured line width for Kotlin source files, and it
will also pick up the Markdown line length and set that as a maximum
line width.

Version 1.1.1
=============

Bug fix for combination of --overlaps-git-changes=staged and relative
paths. Also bails quickly if the given git commit contains no Kotlin
source files.

Version 1.1
===========

This version adds support for --max-comment-width, and for the Gradle
plugin to be able to supply options (via kdocformatter.options =
"--max-line-width=100 --max-comment-width=72" etc.) It changes the
Gradle plugin group id (since the previous one was rejected by
Sonatype) and tweaks a few minor things.

Version 1.0
===========

* Command line script which can recursively format a whole source
  folder

* IDE plugin to format selected files or current comment. Preserves
  caret position in the current comment.

* Gradle plugin to format the source folders in the current project.

* Block tags (like @param) are separated out from the main text, and
  subsequent lines are indented. Blank spaces between doc tags are
  removed. Preformatted text (indented 4 spaces or more) is left
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

