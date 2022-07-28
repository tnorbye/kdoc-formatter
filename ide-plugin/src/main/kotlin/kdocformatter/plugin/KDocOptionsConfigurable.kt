package kdocformatter.plugin

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.enableIf
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import com.intellij.util.ui.UIUtil
import javax.swing.JSeparator
import org.jetbrains.annotations.Nls

class KDocOptionsConfigurable : SearchableConfigurable, Configurable.NoScroll {
  @Nls override fun getDisplayName() = "KDoc Formatting"

  @Suppress("SpellCheckingInspection") override fun getId() = "kdocformatter.options"
  private val formatProcessorCheckBox =
      JBCheckBox("Participate in IDE formatting operations, such as Code > Reformat Code")
  private val alternateCheckBox =
      JBCheckBox(
          "Alternate line breaking algorithms when invoked repeatedly (between greedy and optimal)")
  private val collapseLinesCheckBox =
      JBCheckBox("Collapse short comments that fit on a single line")
  private val convertMarkupCheckBox = JBCheckBox("Convert markup like <b>bold</b> into **bold**")
  private val addPunctuationCheckBox =
      JBCheckBox("Add missing punctuation, such as a period at the end of a capitalized paragraph")
  private val lineCommentsCheckBox =
      JBCheckBox("Allow formatting line comments and block comments interactively")
  private val alignTableColumnsCheckBox =
      JBCheckBox("Align table columns, ensuring that | separators line up")
  private val reorderKDocTagsCheckBox =
      JBCheckBox("Move and reorder KDoc tags to match signature order")
  private val maxCommentWidthEnabledCheckBox =
      JBCheckBox("Allow max comment width to be separate from line width")
  private val overrideLineWidthField = JBTextField("0", 4)
  private val overrideCommentWidthField = JBTextField("0", 4)

  private val state = KDocPluginOptions.instance.globalState

  override fun createComponent() = panel {
    row { collapseLinesCheckBox() }
    row { convertMarkupCheckBox() }
    row { alignTableColumnsCheckBox() }
    row { reorderKDocTagsCheckBox() }
    row { addPunctuationCheckBox() }
    separator()
    row { formatProcessorCheckBox() }
    row { alternateCheckBox() }
    row { lineCommentsCheckBox() }
    separator()
    row { maxCommentWidthEnabledCheckBox() }
    row {
      label(
          "When checked, comments will be limited 72 characters (or the configured Markdown line length),\n" +
              "for improved readability. Otherwise, comments will use the full available line width.",
          style = UIUtil.ComponentStyle.SMALL)
    }
    separator()
    row {
      label(
          "Override line widths (if blank or 0, the code style line width or .editorconfig is used) :")
    }
    row("Line Width") {
      component(overrideLineWidthField).withValidationOnInput { validateWidth(it) }
    }
    row("Comment Width") {
          component(overrideCommentWidthField).withValidationOnInput { validateWidth(it) }
        }
        .enableIf(maxCommentWidthEnabledCheckBox.selected)
  }

  private fun LayoutBuilder.separator() {
    row { component(JSeparator()).constraints(growX) }
  }

  private fun validateWidth(it: JBTextField): Nothing? {
    val value = it.int
    if (value != 0 && value !in 10..1000) {
      error("Invalid line width $value; expected value between 10 and 1000")
    }
    return null
  }

  /**
   * Property accessing a text field's value as an integer, with the
   * special support that 0 translates to blank.
   */
  private var JBTextField.int: Int
    get() = text.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
    set(value) {
      text = if (value == 0) "" else value.toString()
    }

  override fun isModified() =
      alternateCheckBox.isSelected != state.alternateActions ||
          collapseLinesCheckBox.isSelected != state.collapseSingleLines ||
          convertMarkupCheckBox.isSelected != state.convertMarkup ||
          lineCommentsCheckBox.isSelected != state.lineComments ||
          addPunctuationCheckBox.isSelected != state.addPunctuation ||
          formatProcessorCheckBox.isSelected != state.formatProcessor ||
          alignTableColumnsCheckBox.isSelected != state.alignTableColumns ||
          reorderKDocTagsCheckBox.isSelected != state.reorderDocTags ||
          maxCommentWidthEnabledCheckBox.isSelected != state.maxCommentWidthEnabled ||
          overrideLineWidthField.int != state.overrideLineWidth ||
          overrideCommentWidthField.int != state.overrideCommentWidth

  @Throws(ConfigurationException::class)
  override fun apply() {
    state.alternateActions = alternateCheckBox.isSelected
    state.collapseSingleLines = collapseLinesCheckBox.isSelected
    state.convertMarkup = convertMarkupCheckBox.isSelected
    state.lineComments = lineCommentsCheckBox.isSelected
    state.addPunctuation = addPunctuationCheckBox.isSelected
    state.formatProcessor = formatProcessorCheckBox.isSelected
    state.alignTableColumns = alignTableColumnsCheckBox.isSelected
    state.reorderDocTags = reorderKDocTagsCheckBox.isSelected
    state.maxCommentWidthEnabled = maxCommentWidthEnabledCheckBox.isSelected
    state.overrideLineWidth = overrideLineWidthField.int
    state.overrideCommentWidth = overrideCommentWidthField.int
  }

  override fun reset() {
    alternateCheckBox.isSelected = state.alternateActions
    collapseLinesCheckBox.isSelected = state.collapseSingleLines
    convertMarkupCheckBox.isSelected = state.convertMarkup
    lineCommentsCheckBox.isSelected = state.lineComments
    addPunctuationCheckBox.isSelected = state.addPunctuation
    formatProcessorCheckBox.isSelected = state.formatProcessor
    alignTableColumnsCheckBox.isSelected = state.alignTableColumns
    reorderKDocTagsCheckBox.isSelected = state.reorderDocTags
    maxCommentWidthEnabledCheckBox.isSelected = state.maxCommentWidthEnabled
    overrideLineWidthField.int = state.overrideLineWidth
    overrideCommentWidthField.int = state.overrideCommentWidth
  }
}
