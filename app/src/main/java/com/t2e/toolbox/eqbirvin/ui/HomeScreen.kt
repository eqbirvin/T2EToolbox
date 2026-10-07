package com.t2e.toolbox.eqbirvin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToDisplayFix: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("T2E Toolbox") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDisplayFix() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Fix", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Adjust rendering resolution and density via Shizuku.")
                }
            }
        }
    }
}
