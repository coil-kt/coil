/**
 * Copyright 2022 Savvas Dalkitsis
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
package coil.sample

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Modified from: https://github.com/savvasdalkitsis/lazy-staggered-grid */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyStaggeredGrid(
    columnCount: Int,
    states: List<LazyListState> = List(columnCount) { rememberLazyListState() },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyStaggeredGridScope.() -> Unit,
) {
    check(columnCount == states.size) {
        "Invalid number of lazy list states. Expected: $columnCount. Actual: ${states.size}"
    }
    val scope = rememberCoroutineScope()

    val scrollConnections = List(columnCount) { index ->
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    scope.launch {
                        for (stateIndex in states.indices) {
                            if (stateIndex != index) {
                                states[stateIndex].scrollBy(-delta)
                            }
                        }
                    }
                    return Offset.Zero
                }

            }
        }
    }

    val gridScope = RealLazyStaggeredGridScope(columnCount).apply(content)

    Row {
        for (index in 0 until columnCount) {
            CompositionLocalProvider(
                LocalOverScrollConfiguration provides null
            ) {
                LazyColumn(
                    contentPadding = contentPadding,
                    state = states[index],
                    modifier = Modifier
                        .nestedScroll(scrollConnections[index])
                        .weight(1f)
                ) {
                    val items = gridScope.items[index]
                    for (itemIndex in items.indices) {
                        val (key, itemContent) = items[itemIndex]
                        item(key) { itemContent() }
                    }
                }
            }
        }
    }
}

interface LazyStaggeredGridScope {
    fun item(key: Any? = null, content: @Composable () -> Unit)
}

inline fun <T> LazyStaggeredGridScope.items(
    items: List<T>,
    crossinline content: @Composable (T) -> Unit
) {
    for (item in items) {
        item { content(item) }
    }
}

private class RealLazyStaggeredGridScope(private val columnCount: Int) : LazyStaggeredGridScope {

    val items = Array(columnCount) { ArrayList<Pair<Any?, @Composable () -> Unit>>() }
    var currentIndex = 0

    override fun item(key: Any?, content: @Composable () -> Unit) {
        items[currentIndex % columnCount] += key to content
        currentIndex += 1
    }
}
