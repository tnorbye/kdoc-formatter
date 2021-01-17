package kdocformatter.plugin

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import org.jetbrains.annotations.Nls

class KDocOptionsConfigurable : SearchableConfigurable, Configurable.NoScroll {
    @Nls
    override fun getDisplayName() = "KDoc Formatting"
    override fun getId() = "kdocformatter.options"
    private val alternateCheckBox = JBCheckBox("Alternate line breaking algorithms when invoked repeatedly")
    private val collapseLinesCheckBox = JBCheckBox("Collapse short comments that fit on a single line")
    private val convertMarkupCheckBox = JBCheckBox("Convert markup like <b>bold</b> into **bold**>")

    private val state = KDocPluginOptions.instance.globalState

    override fun createComponent() = panel {
        row { alternateCheckBox() }
        row { collapseLinesCheckBox() }
        row { convertMarkupCheckBox() }
    }

    override fun isModified() =
        alternateCheckBox.isSelected != state.alternateActions ||
            collapseLinesCheckBox.isSelected != state.collapseSingleLines ||
            convertMarkupCheckBox.isSelected != state.convertMarkup

    @Throws(ConfigurationException::class)
    override fun apply() {
        state.alternateActions = alternateCheckBox.isSelected
        state.collapseSingleLines = collapseLinesCheckBox.isSelected
        state.convertMarkup = convertMarkupCheckBox.isSelected
    }

    override fun reset() {
        alternateCheckBox.isSelected = state.alternateActions
        collapseLinesCheckBox.isSelected = state.collapseSingleLines
        convertMarkupCheckBox.isSelected = state.convertMarkup
    }
}
