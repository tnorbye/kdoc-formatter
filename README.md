KDoc Formatter
==============

Reformats Kotlin KDoc comments, reflowing text and other cleanup, both
via IDE plugin and command line utility.

This tool reflows comments in KDoc; either on a file or recursively
over nested folders, as well as an IntelliJ IDE plugin where you can
reflow the current comment around the cursor.

Features
--------

* Command line script which can recursively format a whole source
  folder

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

Usage
-----
```
$ kdoc-formatter
Usage: kdoc-formatter [options] file(s)

Options:
  --max-line-width=<n>
    Sets the length of lines. Defaults to 72.
  --single-line-comments=<collapse | expand>
    With `collapse`, turns multi-line comments into a single line if it fits, and with
    `expand` it will always format commands with /** and */ on their own lines.
    The default is `collapse`.
  --overlaps-git-changes=<HEAD | staged>
    If git is on the path, and the command is invoked in a git repository, kdoc-formatter
    will invoke git to find the changes either in the HEAD commit or in the staged files,
    and will format only the KDoc comments that overlap these changes.
  --lines <start:end>, --line <start>
    Line range(s) to format, like 5:10 (1-based; default is all). Can be specified multiple
    times.
  --dry-run, -n
    Prints the paths of the files whose contents would change if the formatter were run
    normally.
  --quiet, -q
    Quiet mode
  --help, -help, -h
    Print this usage statement.
  @<filename>
    Read filenames from file.

kdoc-formatter: Version 1.0
https://github.com/tnorbye/kdoc-formatter
```

IDE Usage
---------
Install the IDE plugin. Then move the caret to a KDoc comment and invoke
Code > Reformat KDoc. You can configure a keyboard shortcut if you perform
this action frequently. (Coming soon: ability to run this action on whole
files and directories from within the IDE; currently use the command line
as shown above to do this.)

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
point to from your consuming projects with
```
buildscript {
    repositories {
        maven { url '/path/to/m2' }
    }
    dependencies {
        classpath "kdocformatter:kdocformatter:1.0"
    }
}
plugins {
    id 'kdoc-formatter'
}
```

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
