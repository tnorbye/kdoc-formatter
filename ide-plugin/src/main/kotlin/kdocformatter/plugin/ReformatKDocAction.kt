/*
 * Copyright (c) Tor Norbye.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kdocformatter.plugin

import com.facebook.ktfmt.kdoc.CommentType
import com.facebook.ktfmt.kdoc.FormattingTask
import com.facebook.ktfmt.kdoc.KDocFormatter
import com.facebook.ktfmt.kdoc.KDocFormattingOptions
import com.facebook.ktfmt.kdoc.computeIndents
import com.facebook.ktfmt.kdoc.findSamePosition
import com.facebook.ktfmt.kdoc.isLineComment
import com.intellij.application.options.CodeStyle
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.ThrowableRunnable
import kotlin.math.min
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class ReformatKDocAction : AnAction(), DumbAware {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

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
      val kdoc = findComment(file, oldCaretOffset) ?: return
      var commentText = kdoc.text
      val startOffset: Int
      val endOffset: Int
      if (kdoc is KDoc) {
        // pass
        startOffset = kdoc.startOffset
        endOffset = kdoc.endOffset
      } else if (KDocPluginOptions.instance.globalState.lineComments && isLineComment(kdoc)) {
        // We need to collect all contiguous line comments and replace the whole
        // region.
        val comments = getCommentBlock(kdoc)
        startOffset = comments.first().startOffset
        endOffset = comments.last().endOffset
        commentText = getComment(comments)
      } else if (KDocPluginOptions.instance.globalState.lineComments && isBlockComment(kdoc)) {
        startOffset = kdoc.startOffset
        endOffset = kdoc.endOffset
      } else {
        return
      }

      val newAnchor = getAnchor(file, startOffset)
      if (KDocPluginOptions.instance.globalState.alternateActions) {
        val prevAlternate = alternate
        alternate = false
        if (newAnchor == anchor) {
          alternate = !prevAlternate
          WindowManager.getInstance().getStatusBar(project).info =
              if (alternate) {
                "Alternate KDoc formatting"
              } else {
                "Standard KDoc formatting"
              }
        }
      }
      anchor = newAnchor

      val options = createFormattingOptions(file, kdoc, alternate)
      val task = createFormattingTask(kdoc, commentText, document, startOffset, options)
      val updated = KDocFormatter(options).reformatComment(task)
      // Attempt to preserve the caret position
      val newDelta = findSamePosition(commentText, oldCaretOffset - startOffset, updated)
      WriteCommandAction.writeCommandAction(project, file)
          .withName("Format KDoc")
          .run(
              ThrowableRunnable {
                document.replaceString(startOffset, endOffset, updated)
                documentManager.commitAllDocuments()
              })
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
    val formatFiles =
        if (files.any { it.isKotlinFile() }) {
          files.toList()
        } else if (files.size == 1 && files[0].isDirectory) {
          files[0].children.filter { it.isKotlinFile() }
        } else {
          return
        }
    val operationStatus =
        ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(formatFiles)
    if (!operationStatus.hasReadonlyFiles()) {
      for (file in formatFiles) {
        if (!file.isKotlinFile()) {
          continue
        }
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: continue
        val comments =
            PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java)
                .filterIsInstance<KDoc>()
                .sortedByDescending { it.startOffset }
        if (comments.isEmpty()) {
          continue
        }
        val document = documentManager.getDocument(psiFile) ?: continue
        WriteCommandAction.writeCommandAction(project, psiFile)
            .withName("Format KDoc")
            .run(
                ThrowableRunnable {
                  for (kdoc in comments) {
                    val commentText = kdoc.text
                    val startOffset = kdoc.startOffset
                    val options = createFormattingOptions(psiFile, kdoc, alternate)
                    val task =
                        createFormattingTask(kdoc, commentText, document, startOffset, options)
                    val updated = KDocFormatter(options).reformatComment(task)
                    document.replaceString(startOffset, kdoc.endOffset, updated)
                  }
                })
      }
      documentManager.commitAllDocuments()
    }
  }

  private fun getComment(comments: List<PsiComment>): String {
    val sb = StringBuilder()
    for (comment in comments) {
      sb.append(comment.text)
      sb.append('\n')
    }
    return sb.toString()
  }

  private fun getCommentBlock(middle: PsiComment): List<PsiComment> {
    val start = findEnd(middle, forward = false)
    val end = findEnd(middle, forward = true)
    if (start === end) {
      return listOf(middle)
    }
    val comments = mutableListOf<PsiComment>()
    var curr: PsiElement? = start
    while (curr != null && curr !== end) {
      if (curr is PsiComment) {
        comments.add(curr)
      }
      curr = curr.nextSibling
    }
    comments.add(end)
    return comments
  }

  private fun findEnd(middle: PsiComment, forward: Boolean): PsiComment {
    var end = middle
    var curr: PsiElement? = middle
    while (curr != null) {
      if (curr is PsiComment) {
        val text = curr.text
        if (!text.isLineComment()) {
          break
        }
        end = curr
      } else if (curr is PsiWhiteSpace) {
        // Two newlines means there's an empty string in between; we shouldn't let
        // line comments jump across blank lines
        val text = curr.text
        val newline = text.indexOf('\n')
        if (newline != -1) {
          if (text.indexOf('\n', newline + 1) != -1) {
            break
          }
        }
      } else {
        break
      }
      if (forward) {
        var c = curr
        while (c != null) {
          val next = c.nextSibling
          if (next != null) {
            curr = next
            break
          }
          c = c.parent
          if (c == null) {
            curr = null
          }
        }
      } else {
        curr = curr.prevSibling
      }
    }
    return end
  }

  private var anchor = 0
  private var alternate = false

  private fun getAnchor(file: PsiFile, startOffset: Int): Int {
    return file.virtualFile.path.hashCode() + startOffset
  }

  private fun VirtualFile.isKotlinFile(): Boolean {
    return name.endsWith(".kt")
  }

  private fun isLineComment(comment: PsiComment): Boolean {
    return comment.tokenType == KtTokens.EOL_COMMENT
  }

  private fun isBlockComment(comment: PsiComment): Boolean {
    return comment.tokenType == KtTokens.BLOCK_COMMENT
  }

  override fun update(event: AnActionEvent) {
    val presentation = event.presentation
    val type = getApplicableCommentType(event)
    val available = type != CommentType.NONE
    if (ActionPlaces.isPopupPlace(event.place)) {
      presentation.isEnabledAndVisible = available
    } else {
      presentation.isEnabled = available
    }
    presentation.text =
        when (type) {
          CommentType.NONE -> return
          CommentType.LINE_COMMENT -> "Reformat Line Comment"
          CommentType.BLOCK_COMMENT -> "Reformat Block Comment"
          CommentType.KDOC -> "Reformat KDoc"
          CommentType.FILE -> "Reformat KDoc Files"
        }
  }

  private enum class CommentType {
    NONE,
    BLOCK_COMMENT,
    LINE_COMMENT,
    KDOC,
    FILE
  }

  private fun findComment(file: PsiFile, caretOffset: Int): PsiComment? {
    val element = file.findElementAt(caretOffset) ?: return null
    val prev = element.prevSibling
    return if (element is PsiWhiteSpace &&
        prev is PsiComment &&
        element.textRange.startOffset == caretOffset) {
      // When reformatting line-comment blocks, the caret may be right at the
      // end of a line; findElementAt will point to the separating newline whitespace;
      // the comment is not its parent.
      prev
    } else {
      PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false)
    }
  }

  private fun getApplicableCommentType(event: AnActionEvent): CommentType {
    val dataContext = event.dataContext
    val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return CommentType.NONE

    val editor = CommonDataKeys.EDITOR.getData(dataContext)
    if (editor != null) {
      val file =
          PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
              ?: return CommentType.NONE
      file.virtualFile ?: return CommentType.NONE
      val currentCaret = editor.caretModel.currentCaret
      val comment = findComment(file, currentCaret.offset) ?: return CommentType.NONE
      if (comment is KDoc) {
        return CommentType.KDOC
      } else if (KDocPluginOptions.instance.globalState.lineComments && isLineComment(comment)) {
        return CommentType.LINE_COMMENT
      } else if (KDocPluginOptions.instance.globalState.lineComments && isBlockComment(comment)) {
        return CommentType.BLOCK_COMMENT
      }
      return CommentType.NONE
    }

    val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext) ?: return CommentType.NONE
    if (files.isEmpty()) {
      return CommentType.NONE
    }
    if (files.all { !it.isDirectory } && files.any { it.isKotlinFile() }) {
      return CommentType.FILE
    } else if (files.size == 1 &&
        files[0].isDirectory &&
        files[0].children.any { it.isKotlinFile() }) {
      return CommentType.FILE
    }
    return CommentType.NONE
  }
}

fun createFormattingTask(
    kdoc: PsiComment,
    comment: String,
    indent: String,
    secondaryIndent: String,
    options: KDocFormattingOptions
): FormattingTask {
  val task = FormattingTask(options, comment, indent, secondaryIndent)
  if (task.type == CommentType.KDOC && options.orderDocTags) {
    val parent = kdoc.parent
    if (parent is KtCallableDeclaration) {
      task.orderedParameterNames = parent.valueParameters.mapNotNull { it.name }.toList()
    }
  }

  return task
}

fun createFormattingTask(
    kdoc: PsiComment,
    comment: String,
    document: Document,
    startOffset: Int,
    options: KDocFormattingOptions
): FormattingTask {
  val (indent, secondaryIndent) =
      computeIndents(startOffset, { offset -> document.charsSequence[offset] }, document.textLength)
  return createFormattingTask(kdoc, comment, indent, secondaryIndent, options)
}

fun createFormattingTask(
    kdoc: PsiComment,
    comment: String,
    options: KDocFormattingOptions
): FormattingTask {
  val startOffset = kdoc.startOffset
  val text = kdoc.containingFile.text
  val (indent, secondaryIndent) =
      computeIndents(startOffset, { offset -> text[offset] }, text.length)
  return createFormattingTask(kdoc, comment, indent, secondaryIndent, options)
}

fun createFormattingOptions(
    file: PsiFile,
    kdoc: PsiComment,
    alternate: Boolean
): KDocFormattingOptions {
  val lineWidth = getLineWidth(file, kdoc)
  val commentWidth = getCommentWidth(file)
  val configOptions = KDocFormattingOptions(lineWidth, min(commentWidth, lineWidth))

  val state = KDocPluginOptions.instance.globalState
  with(configOptions) {
    this.alternate = alternate
    tabWidth = CodeStyle.getIndentOptions(file).TAB_SIZE
    collapseSingleLine = state.collapseSingleLines
    convertMarkup = state.convertMarkup
    addPunctuation = state.addPunctuation
    alignTableColumns = state.alignTableColumns
    orderDocTags = state.reorderDocTags
    if (state.overrideLineWidth > 0) {
      maxLineWidth = state.overrideLineWidth
    }
    if (state.overrideCommentWidth > 0) {
      maxCommentWidth = state.overrideCommentWidth
    }
    if (state.overrideHangingIndent >= 0) {
      hangingIndent = state.overrideHangingIndent
    }
    if (!state.maxCommentWidthEnabled) {
      maxCommentWidth = maxLineWidth
    }
  }
  return configOptions
}

private fun getLineWidth(file: PsiFile, kdoc: PsiComment): Int {
  val kdocLineWidth = CodeStyle.getLanguageSettings(file, kdoc.language).RIGHT_MARGIN
  if (kdocLineWidth > 0) {
    return kdocLineWidth
  }
  val kotlinSettings = CodeStyle.getLanguageSettings(file, KotlinLanguage.INSTANCE)
  val kotlinLineWidth = kotlinSettings.RIGHT_MARGIN
  if (kotlinLineWidth > 0) {
    return kotlinLineWidth
  }
  val rootLineWidth = kotlinSettings.rootSettings.defaultRightMargin
  if (rootLineWidth > 0) {
    return rootLineWidth
  }

  // Just return the default in kdoc formatter
  return KDocFormattingOptions().maxLineWidth
}

private fun getCommentWidth(file: PsiFile): Int {
  val markdownLanguage = Language.findLanguageByID("Markdown")
  if (markdownLanguage != null) {
    val maxCommentWidth = CodeStyle.getLanguageSettings(file, markdownLanguage).RIGHT_MARGIN
    if (maxCommentWidth > 0) {
      return maxCommentWidth
    }
  }

  // Just return the default in kdoc formatter
  return KDocFormattingOptions().maxCommentWidth
}
