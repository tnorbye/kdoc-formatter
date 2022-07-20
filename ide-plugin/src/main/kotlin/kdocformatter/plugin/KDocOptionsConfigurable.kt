package kdocformatter.plugin

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls

class KDocOptionsConfigurable : SearchableConfigurable, Configurable.NoScroll {
  @Nls override fun getDisplayName() = "KDoc Formatting"

  @Suppress("SpellCheckingInspection") override fun getId() = "kdocformatter.options"
  private val formatProcessorCheckBox = JBCheckBox("Participate in IDE formatting operations")
  private val alternateCheckBox =
      JBCheckBox("Alternate line breaking algorithms when invoked repeatedly")
  private val collapseLinesCheckBox =
      JBCheckBox("Collapse short comments that fit on a single line")
  private val convertMarkupCheckBox = JBCheckBox("Convert markup like <b>bold</b> into **bold**")
  private val addPunctuationCheckBox = JBCheckBox("Add missing punctuation")
  private val lineCommentsCheckBox = JBCheckBox("Allow formatting line comments interactively")
  private val alignTableColumnsCheckBox = JBCheckBox("Align table columns")
  private val reorderKDocTagsCheckBox = JBCheckBox("Move and reorder KDoc tags")
  private val maxCommentWidthEnabledCheckBox =
      JBCheckBox("Allow max comment width to be separate from line width")

  private val state = KDocPluginOptions.instance.globalState

  override fun createComponent() = panel {
    row { formatProcessorCheckBox() }
    row { alternateCheckBox() }
    row { collapseLinesCheckBox() }
    row { convertMarkupCheckBox() }
    row { addPunctuationCheckBox() }
    row { lineCommentsCheckBox() }
    row { alignTableColumnsCheckBox() }
    row { reorderKDocTagsCheckBox() }
    row { label("") }
    row { maxCommentWidthEnabledCheckBox() }
    row {
      label(
          "When checked, comments will be limited 72 characters (or the configured Markdown line length),\n" +
              "for improved readability. Otherwise, comments will use the full available line width.",
          style = UIUtil.ComponentStyle.SMALL)
    }
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
          maxCommentWidthEnabledCheckBox.isSelected != state.maxCommentWidthEnabled

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
  }
}
