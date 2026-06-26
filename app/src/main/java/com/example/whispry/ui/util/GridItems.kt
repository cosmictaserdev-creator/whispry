package com.example.whispry.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Emits [items] into the surrounding `LazyColumn` as a responsive grid of [columns] cells per
 * row, without changing the scroll container. With `columns == 1` this is equivalent to a plain
 * list (one full-width cell per row), so phones are unaffected; on tablets the same call lays the
 * items out in 2 columns. Vertical gaps come from the `LazyColumn`'s own `verticalArrangement`.
 *
 * The trailing cells of a short final row are filled with weighted spacers so items keep a stable
 * width and stay left-aligned rather than stretching to fill the row.
 */
fun <T> LazyListScope.gridItems(
    items: List<T>,
    columns: Int,
    horizontalSpacing: Dp,
    itemContent: @Composable (T) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val rows = items.chunked(safeColumns)
    items(rows.size) { rowIndex ->
        val rowItems = rows[rowIndex]
        Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
            rowItems.forEach { item ->
                Box(modifier = Modifier.weight(1f)) {
                    itemContent(item)
                }
            }
            repeat(safeColumns - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
