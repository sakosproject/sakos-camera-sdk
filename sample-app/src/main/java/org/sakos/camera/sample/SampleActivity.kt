package org.sakos.camera.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampleApp()
        }
    }
}

@Composable
private fun SampleApp() {
    var state by remember { mutableStateOf(SampleUiState()) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "SakOS Camera SDK",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Managed capture sample",
                    style = MaterialTheme.typography.titleMedium,
                )
                ModeSelector(state.mode) { selected ->
                    state = SampleUiReducer.selectMode(state, selected)
                }
                CaptureStatus(state)
                ApprovedMediaPanel(state.approvedMediaCount)
            }
        }
    }
}

@Composable
private fun ModeSelector(
    selected: SampleCaptureMode,
    onSelected: (SampleCaptureMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SampleModeButton("Photo", selected == SampleCaptureMode.Photo) { onSelected(SampleCaptureMode.Photo) }
        SampleModeButton("Video", selected == SampleCaptureMode.Video) { onSelected(SampleCaptureMode.Video) }
    }
}

@Composable
private fun SampleModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun CaptureStatus(state: SampleUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${state.mode.name.lowercase().replaceFirstChar { it.uppercase() }} capture", style = MaterialTheme.typography.titleMedium)
            Text(state.availabilityMessage, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = {}, enabled = state.availability == SampleCaptureAvailability.Ready) {
                Text("Capture ${state.mode.name.lowercase()}")
            }
        }
    }
}

@Composable
private fun ApprovedMediaPanel(approvedMediaCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Approved media", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Text(
                if (approvedMediaCount == 0) {
                    "No approved media yet. Blocked, unresolved, failed, and cancelled captures are not shown here."
                } else {
                    "$approvedMediaCount approved item(s) are available to the host-owned viewer."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
