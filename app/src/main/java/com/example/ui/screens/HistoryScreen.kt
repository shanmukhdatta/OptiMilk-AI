package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MeasurementEntity
import com.example.ui.HistoryFilter
import com.example.ui.OptiMilkFormatters
import com.example.ui.OptiMilkUiState
import com.example.ui.theme.BorderSage
import com.example.ui.theme.ChannelBlueAccent
import com.example.ui.theme.ChannelClearAccent
import com.example.ui.theme.ChannelGreenAccent
import com.example.ui.theme.ChannelRedAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMintTint
import com.example.ui.theme.TextPrimaryInk
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningBorder
import com.example.ui.theme.WarningSoftBg

@Composable
fun HistoryScreen(
    uiState: OptiMilkUiState,
    onFilterChange: (HistoryFilter) -> Unit,
    onSelectItem: (MeasurementEntity?) -> Unit,
    onDeleteItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteItem by remember { mutableStateOf<MeasurementEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Measurement History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimaryInk,
                    )
                    Text(
                        text = "Locally saved milk colorimeter predictions and raw TCS34725 channels",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryFilter.entries.forEach { filter ->
                            val selected = uiState.historyFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { onFilterChange(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MintLight,
                                    selectedLabelColor = EmeraldDark,
                                    containerColor = PureWhite,
                                    labelColor = TextSecondarySlate,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderSage,
                                    selectedBorderColor = MintBorder,
                                ),
                            )
                        }
                    }
                }
            }

            val itemsList = uiState.filteredHistoryItems
            if (itemsList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceMintTint,
                        border = BorderStroke(1.dp, BorderSage),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MintLight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                text = "No Saved Measurements",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryInk,
                            )
                            Text(
                                text = "Run a measurement from the Home dashboard and tap Save to store sample records on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = itemsList,
                    key = { it.id },
                ) { entry ->
                    HistoryReadingCard(
                        item = entry,
                        onClick = { onSelectItem(entry) },
                        onDeleteClick = { pendingDeleteItem = entry },
                    )
                }
            }
        }
    }

    // Detail View Modal
    uiState.selectedHistoryItem?.let { item ->
        AlertDialog(
            onDismissRequest = { onSelectItem(null) },
            containerColor = PureWhite,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.sampleName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryInk,
                    )
                    if (item.isDemo) {
                        DemoTag()
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MintLight,
                        border = BorderStroke(1.dp, MintBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "PREDICTED CONCENTRATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldDark,
                            )
                            Text(
                                text = "${OptiMilkFormatters.formatConcentration(item.concentration)} ${item.unit}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    DetailLine("Timestamp", OptiMilkFormatters.formatDateTime(item.timestampMillis))
                    DetailLine("Source", item.deviceName)
                    DetailLine("Red Channel", item.red.toString())
                    DetailLine("Green Channel", item.green.toString())
                    DetailLine("Blue Channel", item.blue.toString())
                    DetailLine("Clear Channel", item.clear.toString())
                }
            },
            confirmButton = {
                TextButton(onClick = { onSelectItem(null) }) {
                    Text("Close", color = EmeraldPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteItem(item.id)
                    },
                ) {
                    Text("Delete", color = ErrorCrimson)
                }
            },
        )
    }

    // Delete Confirmation Dialog
    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            containerColor = PureWhite,
            title = { Text("Delete Measurement?", color = TextPrimaryInk) },
            text = {
                Text(
                    "Remove '${item.sampleName}' (${OptiMilkFormatters.formatConcentration(item.concentration)} ${item.unit}) from local history?",
                    color = TextSecondarySlate,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(item.id)
                        pendingDeleteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorCrimson,
                        contentColor = PureWhite,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingDeleteItem = null },
                    border = BorderStroke(1.dp, BorderSage),
                ) {
                    Text("Cancel", color = TextPrimaryInk)
                }
            },
        )
    }
}

@Composable
private fun HistoryReadingCard(
    item: MeasurementEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderSage),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_card_${item.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = item.sampleName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.isDemo) {
                            DemoTag()
                        }
                    }
                    Text(
                        text = OptiMilkFormatters.formatDateTime(item.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MintLight,
                        border = BorderStroke(1.dp, MintBorder),
                    ) {
                        Text(
                            text = "${OptiMilkFormatters.formatConcentration(item.concentration)} ${item.unit}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = EmeraldDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_history_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete ${item.sampleName}",
                            tint = TextSecondarySlate,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MiniChannelBadge("R", item.red, ChannelRedAccent, Modifier.weight(1f))
                MiniChannelBadge("G", item.green, ChannelGreenAccent, Modifier.weight(1f))
                MiniChannelBadge("B", item.blue, ChannelBlueAccent, Modifier.weight(1f))
                MiniChannelBadge("C", item.clear, ChannelClearAccent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniChannelBadge(
    prefix: String,
    value: Int,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceMintTint,
        border = BorderStroke(1.dp, BorderSage),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$prefix:$value",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = TextPrimaryInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DemoTag() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = WarningSoftBg,
        border = BorderStroke(1.dp, WarningBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Science,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = "DEMO",
                style = MaterialTheme.typography.labelSmall,
                color = WarningAmber,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondarySlate,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = TextPrimaryInk,
        )
    }
}
