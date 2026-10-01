package com.amoledwatchfaces.solarpath.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.amoledwatchfaces.solarpath.R

@Composable
fun TextInputDialog(
    showDialog: Boolean,
    inputLabel: String = "",
    initialValue: () -> String = { "" },
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit = {},
    dismissDialog: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val transformationSpec = rememberTransformationSpec()
    var text by rememberSaveable { mutableStateOf(initialValue()) }

    AlertDialog(
        visible = showDialog,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary,
                text = stringResource(R.string.search),
                style = MaterialTheme.typography.labelMedium
            )
        },
        transformationSpec = transformationSpec,
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    onSubmit(text)
                    dismissDialog()
                },
                content = {
                    Icon(Icons.Default.Search, "Submit")
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    onCancel()
                    dismissDialog()
                }
            )
        },
        onDismissRequest = {
            onCancel()
            dismissDialog()
        }
    ) {
        item {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = {},
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content?.invoke(this)
                    val keyboardController = LocalSoftwareKeyboardController.current
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(text = inputLabel, color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onAny = { keyboardController?.hide() }),
                        singleLine = true,
                        shape = RoundedCornerShape(TextFieldDefaults.MinHeight / 2),
                        modifier = Modifier.fillMaxSize(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            selectionColors = TextSelectionColors(
                                backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0f),
                                handleColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                }
            }
        }
    }
}
