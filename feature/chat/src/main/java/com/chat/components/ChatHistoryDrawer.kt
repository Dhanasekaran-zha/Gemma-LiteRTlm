package com.chat.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatHistoryDrawer(){

    ModalDrawerSheet {
        Text(
            text = "Menu",
            modifier = Modifier.padding(16.dp)
        )

        NavigationDrawerItem(
            label = { Text("Home") },
            selected = true,
            onClick = {  }
        )

        NavigationDrawerItem(
            label = { Text("Profile") },
            selected = false,
            onClick = { }
        )

    }


}