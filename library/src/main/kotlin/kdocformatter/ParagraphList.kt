package kdocformatter

class ParagraphList(
    private val paragraphs: List<Paragraph>,
    private val options: KDocFormattingOptions
) : Iterable<Paragraph> {
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

            if (!inPreformat) {
                var cleaned = paragraph.text
                if (options.convertMarkup && (cleaned.contains("<") || cleaned.contains(">"))) {
                    cleaned = cleaned.replace("<b>", "**").replace("</b>", "**")
                        .replace("<i>", "*").replace("</i>", "*")
                }
                if (options.convertMarkup && cleaned.contains("&")) {
                    cleaned = cleaned.replace("&lt;", "<").replace("&LT;", "<")
                        // TODO: <b> and </b> ?
                        .replace("&gt;", ">").replace("&GT;", ">")
                }

                paragraph.text = cleaned
            }

            prev = paragraph
        }
    }

    fun isSingleParagraph() = paragraphs.size == 1
    override fun iterator(): Iterator<Paragraph> = paragraphs.iterator()
}
