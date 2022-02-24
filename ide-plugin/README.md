# KDoc Formatter Plugin

This plugin setup was based on https://github.com/JetBrains/intellij-platform-plugin-template
at revision 7251596f1644f6bb5a7b985e3e8ce0614826eb43.

<!-- Plugin description -->
This plugin lets you reformat KDoc text -- meaning that it will reformat the
text and flow the text up to the line width, collapsing comments that
fit on a single line, indenting text within a block tag, etc.

There are two usage modes. First, it can reformat the current comment
around the caret position. Open a Kotlin file, navigate to the KDoc
comment (e.g. <code>/** My Comment */</code>), and then invoke Code | Reformat KDoc.

The second mode lets you reformat all the comments in one or more Kotlin
source files. For this, navigate to the Projects view and select one or
more source files, and again invoke Code | Reformat KDoc.

More details about the features can be found at
[https://github.com/tnorbye/kdoc-formatter#kdoc-formatter](https://github.com/tnorbye/kdoc-formatter#kdoc-formatter).

You can create a shortcut and assign it to this action if you use it
frequently. On Mac for example, open the Preferences dialog, search for
Keymap, then in the Keymap search field search for "KDoc", and double click
on the action to choose "Add Shortcut", then choose the shortcut you want.
For me, formatting the whole file is assigned to Cmd-Opt-L, so I've assigned
Reformat KDoc to Cmd-Shift-L.
<!-- Plugin description end -->
