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

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.UiDslUnnamedConfigurable
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.ui.dsl.builder.*
import javax.swing.text.JTextComponent
import kotlin.reflect.KMutableProperty0
import org.jetbrains.annotations.Nls

class KDocOptionsConfigurable :
    UiDslUnnamedConfigurable.Simple(), SearchableConfigurable, Configurable.NoScroll {
  @Nls override fun getDisplayName() = "KDoc Formatting"

  @Suppress("SpellCheckingInspection") override fun getId() = "kdocformatter.options"

  private val state = KDocPluginOptions.instance.globalState

  override fun Panel.createContent() {
    panel {
      row {
        checkBox("Collapse short comments that fit on a single line")
            .bindSelected(state::collapseSingleLines)
      }
      row {
        checkBox("Convert markup like <b>bold</b> into **bold**").bindSelected(state::convertMarkup)
      }
      row {
        checkBox("Align table columns, ensuring that | separators line up")
            .bindSelected(state::alignTableColumns)
      }
      row {
        checkBox("Move and reorder KDoc tags to match signature order")
            .bindSelected(state::reorderDocTags)
      }
      row {
        checkBox("Add missing punctuation, such as a period at the end of a capitalized paragraph")
            .bindSelected(state::addPunctuation)
      }
      separator()
      row {
        checkBox("Participate in IDE formatting operations, such as Code > Reformat Code")
            .bindSelected(state::formatProcessor)
      }
      row {
        checkBox(
                "Alternate line breaking algorithms when invoked repeatedly (between greedy and optimal)")
            .bindSelected(state::alternateActions)
      }
      row {
        checkBox("Allow formatting line comments and block comments interactively")
            .bindSelected(state::lineComments)
      }
      separator()
      row {
        checkBox("Allow max comment width to be separate from line width")
            .bindSelected(state::maxCommentWidthEnabled)
      }
      row {
        comment(
            "When checked, comments will be limited 72 characters (or the configured Markdown line length),\n" +
                "for improved readability. Otherwise, comments will use the full available line width.",
        )
      }
      separator()
      row {
        label(
            "Override line widths (if blank or 0, the code style line width or .editorconfig is used):")
      }

      row("Line Width") {
        // Not using intTextField(range = 10..1000) because we want to allow blanks
        textField().bindWidth(state::overrideLineWidth).columns(4)
      }

      row("Comment Width") {
        textField()
            .bindWidth(state::overrideCommentWidth)
            .columns(4)
            .trimmedTextValidation(widthValidator)
      }

      separator()
      row { label("Override continuation indent (@param lists, etc); leave blank to use default:") }

      row("Continuation Indentation") {
        textField()
            .bindWidth(state::overrideHangingIndent, -1)
            .columns(4)
            .trimmedTextValidation(indentValidator)
      }
    }
  }

  private fun <T : JTextComponent> Cell<T>.bindWidth(
      prop: KMutableProperty0<Int>,
      useDefault: Int = 0
  ): Cell<T> {
    return bindWidth(prop.toMutableProperty(), useDefault)
  }

  // Like bindIntText, but treats blank as [default]
  private fun <T : JTextComponent> Cell<T>.bindWidth(
      prop: MutableProperty<Int>,
      useDefault: Int = 0
  ): Cell<T> {
    return bindText(
        getter = {
          val value = prop.get()
          if (value == useDefault) "" else value.toString()
        },
        setter = { value: String ->
          if (value.isEmpty()) {
            prop.set(useDefault)
          } else {
            val v = value.toIntOrNull()
            if (v != null) {
              prop.set(v)
            }
          }
        })
  }

  private val widthValidator: DialogValidation.WithParameter<() -> String> =
      validationErrorIf<String>("Field must be empty or an integer in the range 10 to 1000") {
        val value = it
        value.isNotEmpty() && value.any { digit -> !digit.isDigit() } ||
            (value.toIntOrNull() == null || value.toInt() < 10)
      }

  private val indentValidator: DialogValidation.WithParameter<() -> String> =
      validationErrorIf<String>("Field must be empty or an integer in the range 0 to 12") {
        val value = it
        value.isNotEmpty() && value.any { digit -> !digit.isDigit() } ||
            (value.toIntOrNull() == null || value.toInt() > 12)
      }
}
