package kdocformatter.plugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import kdocformatter.KDocFormatter
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtPsiFactory

class KDocPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        if (!KDocPluginOptions.instance.globalState.formatProcessor) {
            return source
        }

        val kdoc = source as? KDoc ?: return source

        // TODO: Consult options to see whether we want this to participate in formatting
        val file = source.containingFile
        val indent = getIndent(file, kdoc.startOffset)
        val original = kdoc.text
        val options = getKDocFormattingOptions(file, source, false)
        val formatted = KDocFormatter(options).reformatComment(original, indent)
        return if (formatted != original) {
            val newComment = KtPsiFactory(source.project).createComment(formatted)
            return source.replace(newComment)
        } else {
            source
        }
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        // Format all top-level comments in this range
        for (element in PsiTreeUtil.findChildrenOfType(source, KDoc::class.java)) {
            if (rangeToReformat.intersects(element.textRange)) {
                processElement(element, settings)
            }
        }
        return rangeToReformat
    }

    private fun getIndent(file: PsiFile, offset: Int): String {
        val documentText = file.text
        var curr = offset - 1
        while (curr >= 0) {
            val c = documentText[curr]
            if (c == '\n') {
                break
            } else if (!c.isWhitespace()) {
                // No indent
                curr = offset - 1
                break
            } else {
                curr--
            }
        }
        return documentText.substring(curr + 1, offset)
    }
}
