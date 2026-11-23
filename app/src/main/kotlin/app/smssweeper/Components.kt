package app.smssweeper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Flat, bordered container. The only "card" style in the app. */
@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Same look as [Panel] but without inner padding, for lists with their own row padding. */
@Composable
fun ListPanel(content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
) { Column(content = content) }

/** Top of every screen: what this section is and what it does. */
@Composable
fun ScreenHeader(icon: ImageVector, title: String, subtitle: String) = Row(
    Modifier.fillMaxWidth().padding(top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Hint(subtitle)
    }
}

@Composable
fun SectionLabel(text: String) = Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun Hint(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Horizontally scrollable row, for chips. */
@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) = Row(
    Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    content = content,
)

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(label, Modifier.weight(1f))
    Switch(checked, onChange)
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(
    value, onChange, Modifier.fillMaxWidth(),
    label = { Text(label) }, singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
)

@Composable
fun DateField(label: String, value: Long?, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.DateRange, null, Modifier.size(18.dp))
        Text(if (value == null) label else "${jalali(value)}  (${iso(value)})", Modifier.padding(start = 8.dp))
    }
    if (open) {
        JalaliPickerDialog(value, onDismiss = { open = false }) {
            onPick(it)
            open = false
        }
    }
}

/** Explains whether a deletion can be undone, for the final confirmation step. */
fun backupNote(vm: AppVm) =
    if (vm.backupOn) "یک نسخه‌ی پشتیبان تا ${vm.retention} روز در بخش «بازیابی» نگه‌داری می‌شود."
    else "پشتیبان‌گیری خاموش است؛ بعد از این کار راه بازگشتی وجود ندارد."

/**
 * Two-step confirmation for anything destructive: a first dialog explains the action,
 * a second one asks for the final, explicit go-ahead. Show it only while an action is pending.
 */
@Composable
fun TwoStepConfirm(
    title: String,
    message: String,
    finalMessage: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    if (step == 1) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = { TextButton({ step = 2 }) { Text("ادامه") } },
            dismissButton = { TextButton(onDismiss) { Text("انصراف") } },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأیید نهایی") },
            text = { Text(finalMessage) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(confirmLabel) }
            },
            dismissButton = { TextButton(onDismiss) { Text("انصراف") } },
        )
    }
}
