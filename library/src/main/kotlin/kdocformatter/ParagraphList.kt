package kdocformatter

class ParagraphList(val paragraphs: List<Paragraph>) : Iterable<Paragraph> {
    init {
        var prev: Paragraph? = null
        var inPreformat = false
        for (paragraph in paragraphs) {
            paragraph.preformatted = inPreformat || paragraph.preformatted
            paragraph.separate = when {
                prev == null -> false
                // Don't separate kdoc tags, except for the first one
                paragraph.isBlockTag() -> !prev.isBlockTag()
                paragraph.preformatted -> !prev.preformatted
                paragraph.listItem -> false
                else -> true
            }
            if (paragraph.listItem) {
                paragraph.hangingIndent = getIndent(paragraph.text.indexOf(' ') + 1)
            }
            if (paragraph.text.startsWith("```")) {
                if (!inPreformat) {
                    paragraph.preformatted = true
                }
                inPreformat = !inPreformat
            } else if (paragraph.text.startsWith("<pre>", ignoreCase = true)) {
                paragraph.preformatted = true
                inPreformat = true
            } else if (paragraph.text.startsWith("</pre>", ignoreCase = true)) {
                paragraph.preformatted = true
                inPreformat = false
            }
            prev = paragraph
        }
    }

    fun isSingleParagraph() = paragraphs.size == 1
    override fun iterator(): Iterator<Paragraph> = paragraphs.iterator()
}
