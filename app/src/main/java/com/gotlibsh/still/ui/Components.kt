package com.gotlibsh.still.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Click / long-click without the ripple, to keep the screen calm. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.quietClickable(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

/** A plain text field with a thin underline. */
@Composable
fun StillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    fontSize: Int = 18,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    val style = TextStyle(color = StillColors.text, fontSize = fontSize.sp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(StillColors.text),
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) imeAction else ImeAction.Default),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier,
        decorationBox = { inner ->
            Column {
                Box(Modifier.padding(vertical = 6.dp)) {
                    if (value.isEmpty()) {
                        androidx.compose.material3.Text(placeholder, style = style.copy(color = StillColors.faint))
                    }
                    inner()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StillColors.faint)
                )
            }
        },
    )
}
