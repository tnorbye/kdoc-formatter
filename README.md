KDoc Formatter
==============
Reformats Kotlin KDoc comments, reflowing text and other cleanup,
both via IDE plugin and command line utility.

This tool reflows comments in KDoc; either on a file or
recursively over nested folders, as well as an IntelliJ
IDE plugin where you can reflow the current comment around
the cursor.

Features
--------
* Command line script which can recursively format whole source folder
* IDE plugin to format selected files or current comment
* Block tags (like @param) are separated out from the main text,
  and subsequent lines are indented. Blank spaces between doc tags
  are removed. Preformatted text (indented 4 spaces or more) is left
  alone.
* Multiline comments that would fit on a single line are converted
  to a single line comment (configurable via options)
* Cleans up for the double spaces  left by the IntelliJ Convert to
  Kotlin action right before  the closing comment token.
* Removes trailing spaces.

Usage
-----
```
$ kdoc-formatter
Usage: kdoc-formatter [options] file(s)

Options:
  --line-width
    Sets the length of lines. Defaults to 72.
  --single-line-comments
    Turns multi-line comments into a single lint if it fits.
  --single-line-comments
    Always creates multi-line comments, even for comments that would fit on a single line.
  --dry-run, -n
    Prints the paths of the files whose contents would change if the formatter were run normally.
  --help, -help, -h
    Print this usage statement.
  @<filename>
    Read filenames from file.

kdoc-formatter: Version 1.0-SNAPSHOT
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
./gradlew install
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

Integrate into ktlint?
----------------------
I use [ktlint](https://github.com/pinterest/ktlint) 
to format and pretty-print my Kotlin source code.  However,
it does not do comment reformatting, which means I spend time either
manually reflowing myself when I edit comments, or worse, leave it
unformatted.

Given that I use ktlint for formatting, the Right Thing would have
been for me to figure out how it works, and implement the
functionality there. However, I'm busy with a million other things,
and this was just a quick Saturday morning project -- which
unfortunately satisfies my immediate formatting needs -- so I no longer have
the same motivation to get ktlint to support it. (Also, take a look at
the code; it's pretty quick and dirty so would probably need a
fair bit of work before getting it accepted into ktlint.)
