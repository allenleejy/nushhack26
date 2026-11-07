package com.nushhack26.app.ui.enrol

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/**
 * design/components/TextField for values a person compares (staff numbers, codes): the label
 * above a square 64dp box, bold uppercase mono, a 4dp signal bar along the bottom while
 * focused, and the help line below, which becomes the reason on error.
 */
@Composable
fun TenetTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    help: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = Tenet.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = error != null
    val labelColor = when {
        isError -> colors.alertText
        focused -> colors.signalText
        else -> colors.inkMuted
    }
    val valueStyle = TenetType.record.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
        Text(label.uppercase(), style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = labelColor)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TenetDimens.fieldHeight)
                .drawWithContent {
                    drawContent()
                    if (focused && !isError) {
                        val bar = 4.dp.toPx()
                        drawRect(colors.signal, topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
                    }
                },
            enabled = enabled,
            singleLine = true,
            textStyle = valueStyle,
            placeholder = placeholder?.let { { Text(it.uppercase(), style = valueStyle, color = colors.inkDisabled) } },
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            interactionSource = interaction,
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                errorTextColor = colors.ink,
                focusedContainerColor = colors.bg100,
                unfocusedContainerColor = colors.bg100,
                errorContainerColor = colors.alertBg,
                disabledContainerColor = colors.bg200,
                focusedBorderColor = colors.signalEdge,
                unfocusedBorderColor = colors.lineStrong,
                errorBorderColor = colors.alert,
                cursorColor = colors.signalText,
                errorCursorColor = colors.alertText,
            ),
        )
        val helpText = error ?: help
        if (helpText != null) {
            Text(
                helpText,
                style = TenetType.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = if (isError) colors.alertText else colors.inkMuted,
            )
        }
    }
}

/**
 * Draws a staff number stored raw (`R2210`) as `R-2210`: the hyphen appears as soon as the
 * letter is typed, and one backspace from `R-` removes the letter. Original offsets 0, 1, 2
 * and 5 map to 0, 2, 3 and 6 in the transformed text, and back.
 */
internal val StaffNoTransformation = VisualTransformation { text ->
    val raw = text.text
    val transformed = if (raw.isEmpty()) raw else raw.first() + "-" + raw.drop(1)
    TransformedText(
        AnnotatedString(transformed),
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset == 0) 0 else offset + 1
            override fun transformedToOriginal(offset: Int) = if (offset <= 1) offset else offset - 1
        },
    )
}

