package com.nushhack26.app.ui.enrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Key
import androidx.compose.material.icons.sharp.Verified
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.rememberSignPrompt
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/** E1 Enrol: shown by AppScaffold until the phone has an identity. Needs the network once. */
@Composable
fun EnrolScreen(container: AppContainer) {
    val viewModel: EnrolViewModel = viewModel(factory = viewModelFactory { initializer { EnrolViewModel(container) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sign = rememberSignPrompt()
    EnrolContent(state) { action ->
        when (action) {
            is EnrolAction.StaffNoChanged -> viewModel.onStaffNo(action.value)
            is EnrolAction.CodeChanged -> viewModel.onCode(action.value)
            EnrolAction.CreateKey -> viewModel.createKeyAndEnrol(sign)
        }
    }
}

@Composable
fun EnrolContent(state: EnrolUiState, modifier: Modifier = Modifier, onAction: (EnrolAction) -> Unit = {}) {
    val colors = Tenet.colors
    val editable = !state.busy && state.enrolled == null
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg000)
            .imePadding()
            .padding(TenetDimens.space4),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space6),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                Text("JOIN YOUR TEAM", style = TenetType.headline, color = colors.ink)
                Text(
                    "Link this phone to you with your staff number and the one-time code from your safety officer. " +
                        "This needs the network once.",
                    style = TenetType.body,
                    color = colors.inkMuted,
                )
            }
            TenetTextField(
                label = "Staff number",
                value = state.staffNo,
                onValueChange = { onAction(EnrolAction.StaffNoChanged(it)) },
                placeholder = "R-2210",
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.testTag("staff_no"),
            )
            TenetTextField(
                label = "Enrolment code",
                value = state.code,
                onValueChange = { onAction(EnrolAction.CodeChanged(it)) },
                placeholder = "000000",
                help = "From your safety officer",
                error = if (state.wrongCode) state.error else null,
                enabled = editable,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                modifier = Modifier.testTag("enrol_code"),
            )
            if (state.error != null && !state.wrongCode) {
                Note(state.error, Icons.Sharp.Warning, colors.alertBg, colors.alert, colors.alertText, Modifier.testTag("enrol_error"))
            }
            state.enrolled?.let {
                Note(it, Icons.Sharp.Verified, colors.okBg, colors.ok, colors.ok, Modifier.testTag("enrolled"), title = "ENROLMENT ACCEPTED")
            }
        }
        PrimaryButton(
            label = when {
                state.busy -> "Checking with the server…"
                state.enrolled != null -> "Opening Tenet"
                else -> "Create my signing key"
            },
            icon = Icons.Sharp.Key,
            enabled = state.canSubmit || state.busy || state.enrolled != null,
            disabledReason = "Enter both",
            onClick = { if (state.canSubmit && !state.busy) onAction(EnrolAction.CreateKey) },
            modifier = Modifier.padding(top = TenetDimens.space4).testTag("create_key"),
        )
    }
}

@Composable
private fun Note(
    text: String,
    icon: ImageVector,
    background: Color,
    border: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = Tenet.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .border(TenetDimens.strokeControl, border, RectangleShape)
            .padding(TenetDimens.space4),
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(TenetDimens.iconMd))
        Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
            title?.let { Text(it, style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = tint) }
            Text(text, style = TenetType.body, color = colors.ink)
        }
    }
}
