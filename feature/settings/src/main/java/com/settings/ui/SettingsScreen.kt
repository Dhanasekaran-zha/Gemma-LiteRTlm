package com.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.model.ThemeMode
import com.settings.components.SettingsSectionTitle
import com.settings.components.ThemeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        onBackClick: () -> Unit,
        viewModel: SettingsViewModel = hiltViewModel()
) {

    val selectedTheme by viewModel.themeMode.collectAsState()

    Scaffold(
            topBar = {

                CenterAlignedTopAppBar(
                        title = {
                            Text(
                                    text = "Settings"
                            )
                        },

                        navigationIcon = {

                            IconButton(
                                    onClick = onBackClick
                            ) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { paddingValues ->

        Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
        ) {

            LazyColumn(
                    modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),

                    contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 20.dp
                    ),

                    verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                item {

                    SettingsSectionTitle(
                            title = "Appearance"
                    )

                    Card(
                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(
                                    containerColor =
                                            MaterialTheme.colorScheme.surfaceContainer
                            ),

                            shape = MaterialTheme.shapes.large
                    ) {

                        ThemeItem(
                                title = "System Default",
                                subtitle = "Follow your device theme",
                                selected = selectedTheme == ThemeMode.SYSTEM,
                                onClick = {
                                    viewModel.updateTheme(ThemeMode.SYSTEM)
                                }
                        )

                        HorizontalDivider()

                        ThemeItem(
                                title = "Light Mode",
                                subtitle = "Use light appearance",
                                selected = selectedTheme == ThemeMode.LIGHT,
                                onClick = {
                                    viewModel.updateTheme(ThemeMode.LIGHT)
                                }
                        )

                        HorizontalDivider()

                        ThemeItem(
                                title = "Dark Mode",
                                subtitle = "Use dark appearance",
                                selected = selectedTheme == ThemeMode.DARK,
                                onClick = {
                                    viewModel.updateTheme(ThemeMode.DARK)
                                }
                        )
                    }
                }
            }
        }
    }
}