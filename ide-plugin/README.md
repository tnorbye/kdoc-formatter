# KDoc Formatter Plugin

This plugin setup was based on
https://github.com/JetBrains/intellij-platform-plugin-template
at revision 7251596f1644f6bb5a7b985e3e8ce0614826eb43.
<!-- Plugin description -->
This plugin lets you reformat KDoc text -- meaning that it will reformat
the text and flow the text up to the line width, collapsing comments
that fit on a single line, indenting text within a block tag, etc.

By default, it integrates into the IDE's formatting action, so you can
invoke the formatter (e.g. Code | Reformat Code) and the comments will
be handled by this plugin.

There's a Settings panel which lets you configure the formatter to
decide whether you want it to reorder KDoc tags to match the signature
order, whether to align table columns, whether to convert HTML markup
into equivalent KDoc markup, etc.

More details about the features can be found at
[https://github.com/tnorbye/kdoc-formatter#kdoc-formatter](https://github.com/tnorbye/kdoc-formatter#kdoc-formatter).
<!-- Plugin description end -->