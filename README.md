KDoc Formatter
==============

Reformats Kotlin KDoc comments, reflowing text and other cleanup, both
via IDE plugin and command line utility.

This tool reflows comments in KDoc; either on a file or recursively over
nested folders, as well as an IntelliJ IDE plugin where you can reflow
the current comment around the cursor.

Here's an example of the plugin in use, showing editing a comment and
then applying the formatting action to clean it up:

![Screenshot](cleanup.gif)

In addition to general cleanup, this is also handy when you're editing a
comment and you need to reflow the paragraph because the current line is
too long or too short:

![Screenshot](modify-line.gif)

Features
--------
* Reflow using optimal instead of greedy algorithm (though in the IDE
  plugin you can turn on alternate formatting and invoking
  the action repeatedly alternates between the two modes.)
* Command line script which can recursively format a whole source
  folder.
* IDE plugin to format selected files or current comment. Preserves
  caret position in the current comment.
  Also hooks into the IDE formatting action.
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
* Realigns table columns in Markdown tables and adds padding.
* Reorders KDoc tags into a canonical order (for example placing
* Can optionally convert various remaining HTML tags in the comments to
  the corresponding KDoc/markdown text. For example, \*\*bold**
  is converted into **bold**, \<p> is converted to a blank line,
  \<h1>Heading\</h1> is converted into # Heading, and so on.
* Support for .editorconfig configuration files to automatically pick up
  line widths. It will normally use the line width configured for
  Kotlin files, but, if Markdown (.md) files are also configured, it
  will use that width as the maximum comment width. This allows you
  to have code line widths of for example 140 but limit comments to
  70 characters (possibly indented). For code, avoiding line breaking
  is helpful, but for text, shorter lines are better for reading.

Command Usage
-------------

```
$ kdoc-formatter
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
    readable. Defaults to 72 (or max-line-width, if set lower than 72.)
  --hanging-indent=<n>
    Sets the number of spaces to use for hanging indents, e.g. second
    and subsequent lines in a bulleted list or kdoc blog tag.
  --convert-markup
    Convert unnecessary HTML tags like &lt; and &gt; into < and >
  --single-line-comments=<collapse | expand>
    With `collapse`, turns multi-line comments into a single line if it
    fits, and with `expand` it will always format commands with /** and
    */ on their own lines. The default is `collapse`.
  --align-table-columns
    Reformat tables such that the |column|separators| line up
  --no-align-table-columns
    Do not adjust formatting within table cells
  --order-doc-tags
    Move KDoc tags to the end of comments, and order them in a canonical
    order (@param before @return, and so on)
  --no-order-doc-tags
    Do not move or reorder KDoc tagså
  --overlaps-git-changes=<HEAD | staged>
    If git is on the path, and the command is invoked in a git
    repository, kdoc-formatter will invoke git to find the changes either
    in the HEAD commit or in the staged files, and will format only the
    KDoc comments that overlap these changes.
  --lines <start:end>, --line <start>
    Line range(s) to format, like 5:10 (1-based; default is all). Can be
    specified multiple times.
  --greedy
    Instead of the optimal line breaking normally used by kdoc-formatter,
    do greedy line breaking instead
  --dry-run, -n
    Prints the paths of the files whose contents would change if the
    formatter were run normally.
  --quiet, -q
    Quiet mode
  --help, -help, -h
    Print this usage statement.
  @<filename>
    Read filenames from file.

kdoc-formatter: Version 1.5.3
https://github.com/tnorbye/kdoc-formatter
```

IntelliJ Plugin Usage
---------------------
Install the IDE plugin. Open up the KDoc Formatting Options and inspect
the settings to see if you want to change any of the options. By
default, KDoc Formatter will use the line width configured for in the
Kotlin editor code style for line breaking, but it will **also** look
up the Markdown line width (typically 72) and use that as the maximum
comment width. This means that by default, comments will be at most 72
characters wide, even when using a code style which for example breaks
at 140 characters.

This is deliberate; comments are optimized for readability, and
very long lines are not very readable -- which is why books are
typically printed in portrait orientation, not landscape orientation.

The code style line width is used to make sure that when the comment is
in deeply nested code, it will still break comments into even shorter
lines such that no line goes beyond the line limit.

(Note that if your project uses .editorconfig files to specify
indentation, those will be used instead of the Codestyle settings.)

![Screenshot](screenshot-settings.png)

Other options here allow you to for example enable the option to
add punctuation where it's missing, or to turn off features such as
conversion of HTML tags into KDoc markup.

The KDoc Formatter plugin integrates into the IDE's formatting actions,
so if you reformat (for example via Code > Reformat Code), the comment
will be reformatted along with the IDE's other code formatting.

You can disable this in options, and instead explicitly invoke the
formatter using Code > Reformat KDoc. You can configure a keyboard
shortcut if you perform this action frequently (go to Preferences,
search for Keymap, and then in the Keymap search field look for "KDoc",
and then double click and choose Add Keyboard Shortcut.

![Screenshot](screenshot.png)

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
        classpath "com.github.tnorbye.kdoc-formatter:kdocformatter:1.5.3"
        // (Sorry about the vanity URL --
        // I tried to get kdoc-formatter:kdoc-formatter:1.3.2 but that
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
