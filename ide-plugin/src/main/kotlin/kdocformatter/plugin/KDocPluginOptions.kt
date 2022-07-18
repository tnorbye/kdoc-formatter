/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kdocformatter.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "KDocFormatter", storages = [Storage("kdocFormatter.xml")])
class KDocPluginOptions : PersistentStateComponent<KDocPluginOptions.ComponentState> {
  var globalState = GlobalState()
    private set

  override fun getState(): ComponentState {
    val state = ComponentState()
    state.state = globalState
    return state
  }

  override fun loadState(state: ComponentState) {
    globalState = state.state
  }

  class ComponentState {
    var state = GlobalState()
  }

  class GlobalState {
    var alternateActions = false
    var collapseSingleLines = true
    var convertMarkup = false
    var lineComments = false
    var addPunctuation = false
    var formatProcessor = true
    var alignTableColumns = true
    var reorderDocTags = true
  }

  companion object {
    val instance: KDocPluginOptions
      get() = ApplicationManager.getApplication().getService(KDocPluginOptions::class.java)
  }
}
