package com.example.whispry.ui.util.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Two-pane master-detail layout for Expanded width.
 *
 * One pane is given a fixed width and the other flexes to fill the rest. Callers decide
 * *whether* to use this (via [masterDetailEnabledFor]); this only lays the two panes out
 * so History and Settings don't each hand-roll the same `Row { weight(1f); width(x) }`.
 *
 * - History: flexible list on the left, fixed detail on the right
 *   (`masterPaneWidth = null`, `detailPaneWidth = 380.dp`).
 * - Settings: fixed category list on the left, flexible detail on the right
 *   (`masterPaneWidth = 300.dp`, `detailPaneWidth = null`).
 *
 * Pass `null` for the pane that should flex. If both are non-null the panes are fixed; if
 * both are null they split the width evenly. Both lambdas receive a `BoxScope` so callers
 * can align/inset their own content.
 */
@Composable
fun MasterDetailScaffold(
    master: @Composable BoxScope.() -> Unit,
    detail: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    masterPaneWidth: Dp? = null,
    detailPaneWidth: Dp? = 380.dp,
) {
    Row(modifier = modifier.fillMaxSize()) {
        val masterModifier =
            if (masterPaneWidth != null) Modifier.width(masterPaneWidth).fillMaxHeight()
            else Modifier.weight(1f).fillMaxHeight()
        val detailModifier =
            if (detailPaneWidth != null) Modifier.width(detailPaneWidth).fillMaxHeight()
            else Modifier.weight(1f).fillMaxHeight()
        Box(modifier = masterModifier, content = master)
        Box(modifier = detailModifier, content = detail)
    }
}
