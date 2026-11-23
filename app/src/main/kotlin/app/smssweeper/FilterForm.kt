package app.smssweeper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Every condition here is ANDed together (see [Criteria]). Common options first, rare ones folded. */
@Composable
fun FilterForm(f: Form) {
    var more by rememberSaveable { mutableStateOf(false) }
    val line = MaterialTheme.colorScheme.outlineVariant

    Panel {
        SectionLabel("نوع پیامک")
        ChipRow { Box.entries.forEach { FilterChip(f.box == it, { f.box = it }, { Text(it.label) }) } }

        HorizontalDivider(color = line)
        SectionLabel("زمان")
        ChipRow { TimeMode.entries.forEach { FilterChip(f.timeMode == it, { f.timeMode = it }, { Text(it.label) }) } }
        when (f.timeMode) {
            TimeMode.NONE -> Unit
            TimeMode.BEFORE -> {
                DateField("انتخاب تاریخ", f.d1) { f.d1 = it }
                Hint("فقط مکالماتی که همه‌ی پیام‌هایشان قبل از این روز است. مکالمه‌ای که بعد از این تاریخ هم پیام دارد، دست‌نخورده می‌ماند.")
            }
            TimeMode.RANGE -> {
                DateField("از تاریخ", f.d1) { f.d1 = it }
                DateField("تا تاریخ (شامل همین روز)", f.d2) { f.d2 = it }
                Hint("فقط مکالماتی که همه‌ی پیام‌هایشان داخل این بازه است.")
            }
            TimeMode.AGE -> {
                NumberField("چند روز گذشته باشد؟", f.days) { f.days = it }
                Hint("مکالماتی که آخرین پیامشان بیش از این تعداد روز پیش بوده.")
            }
        }

        HorizontalDivider(color = line)
        SectionLabel("فرستنده و متن")
        OutlinedTextField(
            f.sender, { f.sender = it }, Modifier.fillMaxWidth(),
            label = { Text("شماره یا نام فرستنده") }, singleLine = true,
        )
        OutlinedTextField(
            f.keyword, { f.keyword = it }, Modifier.fillMaxWidth(),
            label = { Text("متن پیام شامل") }, singleLine = true,
        )
        SwitchRow("فقط اسپم (تبلیغات)", f.spam) { f.spam = it }
        if (f.spam) Hint("کد تأیید و رمز هرگز اسپم حساب نمی‌شوند. تشخیص بر پایه‌ی کلمات کلیدی است؛ قبل از حذف نمونه‌ها را ببین.")

        HorizontalDivider(color = line)
        TextButton({ more = !more }) {
            Text("گزینه‌های بیشتر")
            Icon(
                if (more) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null,
                Modifier.padding(start = 4.dp).size(18.dp),
            )
        }
        if (more) {
            SectionLabel("وضعیت خواندن")
            ChipRow { ReadState.entries.forEach { FilterChip(f.read == it, { f.read = it }, { Text(it.label) }) } }
            SwitchRow("عبارت منظم (regex) در فرستنده و متن", f.regex) { f.regex = it }
            SectionLabel("محدودیت تعداد")
            ChipRow { CountMode.entries.forEach { FilterChip(f.countMode == it, { f.countMode = it }, { Text(it.label) }) } }
            if (f.countMode != CountMode.NONE) {
                NumberField("تعداد", f.n) { f.n = it }
                Hint(
                    if (f.countMode == CountMode.OLDEST) "فقط N پیامکِ قدیمی‌ترِ بین پیامک‌های مطابق شرط‌ها."
                    else "از پیامک‌های مطابق شرط‌ها، N تای آخر نگه داشته و بقیه حذف می‌شوند."
                )
            }
        }
    }
}
