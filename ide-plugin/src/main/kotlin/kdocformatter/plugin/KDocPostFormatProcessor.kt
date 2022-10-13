/*
 * Copyright (C) 2022 Tor Norbye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kdocformatter.plugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.util.PsiTreeUtil
import kdocformatter.KDocFormatter
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtPsiFactory

class KDocPostFormatProcessor : PostFormatProcessor {
  override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
    if (!KDocPluginOptions.instance.globalState.formatProcessor) {
      return source
    }

    val kdoc = source as? KDoc ?: return source

    // TODO: Consult options to see whether we want this to participate in
    //     formatting
    val file = source.containingFile
    val original = kdoc.text
    val options = createFormattingOptions(file, source, false)
    val task = createFormattingTask(source, original, options)
    val formatted = KDocFormatter(options).reformatComment(task)
    return if (formatted != original) {
      val newComment = KtPsiFactory(source.project).createComment(formatted)
      return source.replace(newComment)
    } else {
      source
    }
  }

  override fun processText(
      source: PsiFile,
      rangeToReformat: TextRange,
      settings: CodeStyleSettings
  ): TextRange {
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
