package kdocformatter.plugin

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.DocumentUtil
import com.intellij.util.ThrowableRunnable
import kdocformatter.KDocFormatter
import kdocformatter.KDocFormattingOptions
import org.jetbrains.annotations.Nullable

class ReformatKDocAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val dataContext = event.dataContext
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitAllDocuments()
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = documentManager.getPsiFile(editor.document) ?: return
            val currentCaret = editor.caretModel.currentCaret
            val element = file.findElementAt(currentCaret.offset) ?: return
            val kdoc = PsiTreeUtil.getParentOfType(element, PsiComment::class.java) ?: return
            val commentText = kdoc.text
            val options = getOptions(file, kdoc)
            val indent = DocumentUtil.getIndent(editor.document, kdoc.startOffset)
            val updated = KDocFormatter(options).reformatComment(commentText, indent.toString())
            WriteCommandAction.writeCommandAction(project, file).withName("Format KDoc").run(
                ThrowableRunnable {
                    editor.document.replaceString(kdoc.startOffset, kdoc.endOffset, updated)
                    documentManager.commitAllDocuments()
                }
            )
        }
    }

    private fun getOptions(
        file: @Nullable PsiFile,
        kdoc: @Nullable PsiComment
    ): KDocFormattingOptions {
        var width = CodeStyle.getLanguageSettings(file, kdoc.language).RIGHT_MARGIN
        if (width <= 0) {
            width = KDocFormattingOptions().lineWidth
        }
        val options = KDocFormattingOptions(lineWidth = width)
        options.tabWidth = CodeStyle.getIndentOptions(file).TAB_SIZE
        return options
    }

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val available = isActionAvailable(event)
        if (event.isFromContextMenu) {
            presentation.isEnabledAndVisible = available
        } else {
            presentation.isEnabled = available
        }
    }

    private fun isActionAvailable(event: AnActionEvent): Boolean {
        val dataContext = event.dataContext
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return false
            file.virtualFile ?: return false
            val currentCaret = editor.caretModel.currentCaret
            val element = file.findElementAt(currentCaret.offset) ?: return false
            PsiTreeUtil.getParentOfType(element, PsiComment::class.java) ?: return false
            return true
        }
        return false
    }
}
