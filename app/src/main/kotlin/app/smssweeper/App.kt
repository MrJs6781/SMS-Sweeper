package app.smssweeper

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class Dest(val label: String, val icon: ImageVector) {
    CLEAN("پاکسازی", Icons.Outlined.Delete),
    SENDERS("فرستنده‌ها", Icons.Outlined.Person),
    STATS("آمار", BarChartIcon),
    RECOVER("بازیابی", Icons.Outlined.Refresh),
    SETTINGS("تنظیمات", Icons.Outlined.Settings),
}

@Composable
fun App(vm: AppVm) {
    val ctx = LocalContext.current

    // ---- default-SMS-app / permission state ----
    fun checkDefault() = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    fun checkRead() = ctx.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    var isDefault by remember { mutableStateOf(checkDefault()) }
    var hasRead by remember { mutableStateOf(checkRead()) }
    LifecycleResumeEffect(Unit) {
        isDefault = checkDefault()
        hasRead = checkRead()
        onPauseOrDispose { }
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = checkDefault()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasRead = it }
    LaunchedEffect(isDefault) {
        if (isDefault && !checkRead()) permLauncher.launch(Manifest.permission.READ_SMS)
    }
    LaunchedEffect(hasRead) { if (hasRead && vm.all == null) vm.refresh() }

    var dest by rememberSaveable { mutableStateOf(Dest.CLEAN) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach {
                    NavigationBarItem(
                        selected = dest == it,
                        onClick = { dest = it; vm.message = null },
                        icon = { Icon(it.icon, null) },
                        label = { Text(it.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusBanner(
                isDefault,
                onSet = {
                    val rm = ctx.getSystemService(RoleManager::class.java)
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                },
                onChange = { ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) },
            )
            when (dest) {
                Dest.CLEAN -> CleanScreen(vm, isDefault)
                Dest.SENDERS -> SendersScreen(vm, isDefault)
                Dest.STATS -> StatsScreen(vm)
                Dest.RECOVER -> RecoverScreen(vm, isDefault)
                Dest.SETTINGS -> SettingsScreen(vm)
            }
            vm.message?.let { msg ->
                Panel {
                    Text(msg, fontWeight = FontWeight.Medium)
                    vm.lastBatch?.let { b ->
                        OutlinedButton({ vm.restoreBatch(b) }, enabled = isDefault && !vm.busy) { Text("بازگردانی همین حذف") }
                    }
                }
            }
        }
    }
}

/** One slim line: can this app delete right now? */
@Composable
private fun StatusBanner(isDefault: Boolean, onSet: () -> Unit, onChange: () -> Unit) = Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (isDefault) Icons.Outlined.Check else Icons.Outlined.Warning, null,
            Modifier.size(18.dp),
            tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(
            if (isDefault) "پیش‌فرض پیامک: فعال" else "برای حذف، اپ باید پیش‌فرض پیامک باشد",
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(if (isDefault) onChange else onSet) { Text(if (isDefault) "تغییر" else "تنظیم") }
    }
}
