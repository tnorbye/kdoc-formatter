package kdocformatter.plugin

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.DocumentUtil
import com.intellij.util.ThrowableRunnable
import kdocformatter.KDocFormatter
import kdocformatter.KDocFormatter.Companion.findSamePosition
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
            val document = editor.document
            val file = documentManager.getPsiFile(document) ?: return
            val currentCaret = editor.caretModel.currentCaret
            val oldCaretOffset = currentCaret.offset
            val element = file.findElementAt(oldCaretOffset) ?: return
            val kdoc = PsiTreeUtil.getParentOfType(element, PsiComment::class.java) ?: return
            if (!isKDoc(kdoc)) {
                return
            }
            val commentText = kdoc.text
            val options = getOptions(file, kdoc)
            val startOffset = kdoc.startOffset
            val indent = DocumentUtil.getIndent(document, startOffset)
            val updated = KDocFormatter(options).reformatComment(commentText, indent.toString())
            // Attempt to preserve the caret position
            val newDelta = findSamePosition(commentText, oldCaretOffset - startOffset, updated)
            WriteCommandAction.writeCommandAction(project, file).withName("Format KDoc").run(
                ThrowableRunnable {
                    document.replaceString(startOffset, kdoc.endOffset, updated)
                    documentManager.commitAllDocuments()
                }
            )
            val newCaretOffset = startOffset + newDelta
            if (newCaretOffset != oldCaretOffset) {
                editor.caretModel.currentCaret.moveToOffset(newCaretOffset)
            }
            return
        }

        val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext) ?: return
        if (files.isEmpty()) {
            return
        }
        val formatFiles = if (files.any { it.isKotlinFile() })
            files.toList()
        else if (files.size == 1 && files[0].isDirectory)
            files[0].children.filter { it.isKotlinFile() }
        else {
            return
        }
        val operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(formatFiles)
        if (!operationStatus.hasReadonlyFiles()) {
            for (file in formatFiles) {
                if (!file.isKotlinFile()) {
                    continue
                }
                val psiFile = PsiManager.getInstance(project).findFile(file) ?: continue
                val comments = PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java).filter { isKDoc(it) }
                    .sortedByDescending { it.startOffset }
                if (comments.isEmpty()) {
                    continue
                }
                val document = documentManager.getDocument(psiFile) ?: continue
                WriteCommandAction.writeCommandAction(project, psiFile).withName("Format KDoc").run(
                    ThrowableRunnable {
                        for (kdoc in comments) {
                            if (!isKDoc(kdoc)) {
                                continue
                            }

                            val commentText = kdoc.text
                            val startOffset = kdoc.startOffset
                            val indent = DocumentUtil.getIndent(document, startOffset)
                            val options = getOptions(psiFile, kdoc)
                            val updated = KDocFormatter(options).reformatComment(commentText, indent.toString())
                            document.replaceString(startOffset, kdoc.endOffset, updated)
                        }
                    }
                )
            }
            documentManager.commitAllDocuments()
        }
    }

    private fun VirtualFile.isKotlinFile(): Boolean {
        return name.endsWith(".kt")
    }

    private fun isKDoc(kdoc: PsiComment): Boolean {
        // TODO: Depend on Kotlin plugin such that I can directly check for
        // org.jetbrains.kotlin.kdoc.psi.api.KDoc
        return kdoc.javaClass.name.contains("KDoc")
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

        val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext) ?: return false
        if (files.isEmpty()) {
            return false
        }
        if (files.all { !it.isDirectory } && files.any { it.isKotlinFile() }) {
            return true
        } else if (files.size == 1 && files[0].isDirectory && files[0].children.any { it.isKotlinFile() }) {
            return true
        }
        return false
    }
}
