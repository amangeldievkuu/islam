package com.sunnahcompanion.app.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.trigger.driving.PairedDevice
import com.sunnahcompanion.app.trigger.schedule.ReminderType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showCarPicker by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    // Each feature asks only for what it needs, together with notifications.
    fun onResult(onGranted: () -> Unit): (Map<String, Boolean>) -> Unit = { results ->
        viewModel.refreshPermissions()
        if (results.values.all { it }) onGranted() else viewModel.onPermissionDenied()
    }

    val leavingHomeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult { viewModel.setLeavingHomeEnabled(true) }(r)
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult { viewModel.onUseCurrentLocation() }(r)
    }
    // Android requires background location to be requested on its own, after foreground access.
    val backgroundLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshPermissions()
        if (!granted) viewModel.onPermissionDenied()
    }
    val drivingLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult { viewModel.setDrivingEnabled(true) }(r)
    }
    val bluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshPermissions()
        if (granted) {
            viewModel.loadPairedDevices()
            showCarPicker = true
        } else {
            viewModel.onPermissionDenied()
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult { }(r)
    }
    val remindersLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult { viewModel.setRemindersEnabled(true) }(r)
    }

    val messageText = uiState.message?.let { stringResource(it.textRes) }
    val openSettingsLabel = stringResource(R.string.settings_open_system_settings)
    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = messageText.orEmpty(),
            actionLabel = if (message == SettingsMessage.PERMISSION_DENIED) openSettingsLabel else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        }
        viewModel.onMessageShown()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.settings_title)) }, scrollBehavior = scrollBehavior)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val prefs = uiState.preferences
        if (prefs == null) {
            CircularProgressIndicator(Modifier.padding(innerPadding).padding(24.dp))
            return@Scaffold
        }
        val perms = uiState.permissions
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!perms.notifications) {
                item(key = "notifications") {
                    ActionRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.settings_notifications_off),
                        body = stringResource(R.string.settings_notifications_off_body),
                        action = stringResource(R.string.settings_allow),
                        onAction = { notificationLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) },
                        warning = true,
                    )
                }
            }

            item(key = "h_home") { SectionHeader(stringResource(R.string.settings_section_leaving_home)) }
            item(key = "leaving_home") {
                SwitchRow(
                    icon = Icons.Rounded.Home,
                    title = stringResource(R.string.settings_leaving_home),
                    body = stringResource(R.string.settings_leaving_home_body),
                    checked = prefs.leavingHomeEnabled,
                    onCheckedChange = { enable ->
                        if (enable && !(perms.fineLocation && perms.notifications)) {
                            leavingHomeLauncher.launch(FOREGROUND_LOCATION + Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setLeavingHomeEnabled(enable)
                        }
                    },
                )
            }
            item(key = "home_location") {
                val home = prefs.homeLocation
                ActionRow(
                    icon = Icons.Rounded.MyLocation,
                    title = stringResource(R.string.settings_home_location),
                    body = if (home == null) {
                        stringResource(R.string.settings_home_not_set)
                    } else {
                        stringResource(R.string.settings_home_set, home.latitude, home.longitude)
                    },
                    action = stringResource(R.string.settings_use_current_location),
                    loading = uiState.isLocating,
                    onAction = {
                        if (perms.fineLocation) viewModel.onUseCurrentLocation() else locationLauncher.launch(FOREGROUND_LOCATION)
                    },
                )
            }
            if (prefs.leavingHomeEnabled && !perms.backgroundLocation) {
                item(key = "background_location") {
                    ActionRow(
                        icon = Icons.Rounded.Warning,
                        title = stringResource(R.string.settings_background_location),
                        body = stringResource(R.string.settings_background_location_body),
                        action = stringResource(R.string.settings_allow),
                        warning = true,
                        onAction = {
                            if (perms.fineLocation) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } else {
                                locationLauncher.launch(FOREGROUND_LOCATION)
                            }
                        },
                    )
                }
            }
            uiState.status.geofenceError?.takeIf { prefs.leavingHomeEnabled && perms.backgroundLocation }?.let { error ->
                item(key = "geofence_error") { ErrorText(error) }
            }

            item(key = "h_driving") { SectionHeader(stringResource(R.string.settings_section_driving)) }
            item(key = "driving") {
                SwitchRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = stringResource(R.string.settings_driving),
                    body = stringResource(R.string.settings_driving_body),
                    checked = prefs.drivingEnabled,
                    onCheckedChange = { enable ->
                        if (enable && !(perms.activityRecognition && perms.notifications)) {
                            drivingLauncher.launch(
                                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS),
                            )
                        } else {
                            viewModel.setDrivingEnabled(enable)
                        }
                    },
                )
            }
            item(key = "car_bluetooth") {
                val count = prefs.carDeviceAddresses.size
                ActionRow(
                    icon = Icons.Rounded.Bluetooth,
                    title = stringResource(R.string.settings_car_bluetooth),
                    body = if (count == 0) {
                        stringResource(R.string.settings_car_bluetooth_none)
                    } else {
                        pluralStringResource(R.plurals.settings_car_bluetooth_count, count, count)
                    },
                    action = stringResource(R.string.settings_choose),
                    onAction = {
                        if (perms.bluetoothConnect) {
                            viewModel.loadPairedDevices()
                            showCarPicker = true
                        } else {
                            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    },
                )
            }
            uiState.status.drivingError?.takeIf { prefs.drivingEnabled }?.let { error ->
                item(key = "driving_error") { ErrorText(error) }
            }

            item(key = "h_reminders") { SectionHeader(stringResource(R.string.settings_section_reminders)) }
            item(key = "reminders") {
                SwitchRow(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.settings_reminders),
                    body = reminderSummary(uiState),
                    checked = prefs.remindersEnabled,
                    onCheckedChange = { enable ->
                        if (enable && !perms.notifications) {
                            remindersLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        } else {
                            viewModel.setRemindersEnabled(enable)
                        }
                    },
                )
            }

            item(key = "h_reading") { SectionHeader(stringResource(R.string.settings_section_reading)) }
            item(key = "learning") {
                SwitchRow(
                    icon = Icons.Rounded.School,
                    title = stringResource(R.string.reader_learning_mode),
                    body = stringResource(R.string.settings_learning_body),
                    checked = prefs.learningMode,
                    onCheckedChange = viewModel::setLearningMode,
                )
            }
            item(key = "arabic_size") {
                ArabicSizeRow(scale = prefs.arabicTextScale, onScaleChange = viewModel::setArabicTextScale)
            }
            item(key = "dynamic_color") {
                SwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    body = stringResource(R.string.settings_dynamic_color_body),
                    checked = prefs.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            item(key = "h_about") { SectionHeader(stringResource(R.string.settings_section_about)) }
            item(key = "attribution") {
                Text(
                    text = stringResource(R.string.quran_text_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (showCarPicker) {
        CarPickerDialog(
            devices = uiState.pairedDevices,
            selected = uiState.preferences?.carDeviceAddresses.orEmpty(),
            onConfirm = {
                viewModel.setCarDevices(it)
                showCarPicker = false
            },
            onDismiss = { showCarPicker = false },
        )
    }
}

private val FOREGROUND_LOCATION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private val SettingsMessage.textRes: Int
    get() = when (this) {
        SettingsMessage.LOCATION_UNAVAILABLE -> R.string.settings_location_unavailable
        SettingsMessage.HOME_SAVED -> R.string.settings_home_saved
        SettingsMessage.PERMISSION_DENIED -> R.string.settings_permission_denied
    }

@Composable
private fun reminderSummary(state: SettingsUiState): String {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    fun ZonedDateTime?.fmt() = this?.format(formatter) ?: "—"
    val times = state.reminderTimes
    val summary = stringResource(
        R.string.settings_reminders_body,
        times[ReminderType.MORNING_ADHKAR].fmt(),
        times[ReminderType.EVENING_ADHKAR].fmt(),
        times[ReminderType.BEFORE_SLEEP].fmt(),
    )
    return if (state.reminderTimesEstimated) summary + "\n" + stringResource(R.string.settings_reminders_estimated) else summary
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    warning: Boolean = false,
    loading: Boolean = false,
) {
    ListItem(
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = { Text(title) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body)
                if (loading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    FilledTonalButton(onClick = onAction) { Text(action) }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ArabicSizeRow(scale: Float, onScaleChange: (Float) -> Unit) {
    // Local state while dragging; persisted once the gesture ends.
    var value by remember(scale) { mutableFloatStateOf(scale) }
    ListItem(
        leadingContent = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.settings_arabic_size, (value * 100).roundToInt())) },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onScaleChange(value) },
                valueRange = 0.8f..1.6f,
                steps = 7,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun CarPickerDialog(
    devices: List<PairedDevice>,
    selected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_car_picker_title)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.settings_car_picker_empty))
            } else {
                LazyColumn {
                    items(devices, key = { it.address }) { device ->
                        val isChecked = device.address in chosen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(value = isChecked, role = Role.Checkbox) {
                                    chosen = if (it) chosen + device.address else chosen - device.address
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = null)
                            Text(device.name, Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(chosen) }) { Text(stringResource(R.string.settings_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}
