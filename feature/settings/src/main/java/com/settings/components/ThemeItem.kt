package com.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun ThemeItem(
        title: String,
        subtitle: String,
        selected: Boolean,
        onClick: () -> Unit
) {

    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                            selected = selected,
                            onClick = onClick,
                            role = Role.RadioButton
                    )
                    .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                    )
    ) {

        androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                    modifier = Modifier.weight(1f)
            ) {

                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                )

                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                    selected = selected,
                    onClick = null
            )
        }
    }
}