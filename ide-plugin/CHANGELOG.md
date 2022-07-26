<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KDoc Formatter Plugin Changelog

## [1.5.4]
- Fix 9 bugs filed by the ktfmt project.

## [1.5.3]
- @param tags are reordered to match the parameter order in the
  corresponding method signature.
- There are now options for explicitly specifying the line width and the
  comment width which overrides the inferred
  width from code styles or .editorconfig files.
- Some reorganization of the options along with updates labels to
  clarify what they mean.

## [1.5.2]
- Adds a new option which lets you turn off the concept of a separate
  maximum comment width from the maximum line width. By
  default, comments are limited to 72 characters wide (or
  more accurately the configured width for Markdown files),
  which leads to more readable text. However, if you really
  want the full line width to be used, uncheck the "Allow
  max comment width to be separate from line width" setting.
- Fixes bug to ensure the line width and max comment width are properly
  read from the IDE environment settings.

## [1.5.1]
- Updated formatter with many bug fixes, as well as improved support for
  formatting tables as well as reordering KDoc tags. There
  are new options controlling both of these behaviors.
- Removed Kotlin logo from the IDE plugin icon

## [1.4.1]
- Fix formatting nested bulleted lists
  (https://github.com/tnorbye/kdoc-formatter/issues/36)

## [1.4.0]
- The KDoc formatter now participates in regular IDE source code
  formatting (e.g. Code > Reformat Code).
  This can be turned off via a setting.
- The markup conversion (if enabled) now converts [] and {@linkplain}
  tags to the KDoc equivalent.
- Fix bug where preformatted text immediately following a TODO comment
  would be joined into the TODO.

## [1.3.3]
- Don't break lines inside link text which will include the comment
  asterisk
- Allow formatting during indexing

## [1.3.2]
- Bugfixes and update deprecated API usage for 2021.2 compatibility

## [1.3.1]
- Fixes a few bugs around markup conversion not producing the right
  number of blank lines for a <p>, and adds a few more
  prefixes as non-breakable (e.g. if you have an em
  dash in your sentence -- like this -- we don't want
  the "--" to be placed at the beginning of a line.)
- Adds an --add-punctuation command line flag and IDE setting to
  optionally add closing periods on capitalized
  paragraphs at the end of the comment.
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
- IDE settings panel
- Ability to alternate formatting between greedy and optimal line
  breaking when invoked repeatedly (and for short comments,
  alternating between single line and multiple lines.)

## [1.1.2]
- Basic support for <code>.editorconfig</code> files.

## [1.1.0]
- Support for setting maximum comment width (capped by the maximum line
  width).

## [1.0.0]
- Initial version