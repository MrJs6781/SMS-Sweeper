package app.smssweeper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** First-run screen: collects every permission/role the app needs in one place. */
@Composable
fun Onboarding(
    hasRead: Boolean,
    isDefault: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        ScreenHeader(Icons.Outlined.Delete, "خوش آمدی به SMS Sweeper", "برای اینکه همه‌ی بخش‌ها بدون مشکل کار کنند، این دسترسی‌ها را یک‌بار بده.")

        Panel {
            Step(hasRead, "دسترسی به پیامک‌ها", "خواندن پیامک‌ها برای نمایش، شمارش و پیدا کردن موارد مطابق شرط‌ها.")
            Step(isDefault, "برنامه‌ی پیش‌فرض پیامک", "اندروید فقط به برنامه‌ی پیش‌فرض اجازه‌ی حذف و بازیابی می‌دهد. بعد از کارت، هر وقت خواستی برنامه‌ی قبلی را پیش‌فرض کن.")
        }

        Button(onGrant, Modifier.fillMaxWidth()) { Text("اعطای دسترسی‌ها") }

        if (isDefault && !hasRead) {
            Hint("اگر پنجره‌ی مجوز ظاهر نشد، اندروید آن را برای برنامه‌هایی که از بیرون فروشگاه نصب شده‌اند محدود کرده است. در تنظیمات برنامه، از منوی ⋮ «Allow restricted settings» را بزن و بعد مجوز «SMS» را روشن کن.")
            OutlinedButton(onOpenSettings, Modifier.fillMaxWidth()) { Text("باز کردن تنظیمات برنامه") }
        }
        if (hasRead) TextButton(onSkip, Modifier.fillMaxWidth()) { Text("فعلاً فقط مشاهده (بدون حذف)") }
    }
}

@Composable
private fun Step(done: Boolean, title: String, text: String) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top,
) {
    val icon: ImageVector = if (done) Icons.Outlined.Check else Icons.Outlined.Info
    Icon(
        icon, null,
        Modifier.padding(top = 2.dp).size(22.dp),
        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Hint(text)
    }
}
