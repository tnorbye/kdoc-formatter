package kdocformatter.plugin

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class KDocOptionsConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return KDocOptionsConfigurable()
    }
}
