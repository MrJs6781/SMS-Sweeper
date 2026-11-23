package app.smssweeper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/** Every condition here is ANDed together (see [Criteria]). */
@Composable
fun FilterForm(f: Form) {
    Panel {
        Text("کدام پیامک‌ها؟", fontWeight = FontWeight.Bold)
        ChipRow { Box.entries.forEach { FilterChip(f.box == it, { f.box = it }, { Text(it.label) }) } }
    }

    Panel {
        Text("زمان", fontWeight = FontWeight.Bold)
        ChipRow { TimeMode.entries.forEach { FilterChip(f.timeMode == it, { f.timeMode = it }, { Text(it.label) }) } }
        when (f.timeMode) {
            TimeMode.NONE -> Unit
            TimeMode.BEFORE -> {
                DateField("انتخاب تاریخ", f.d1) { f.d1 = it }
                Hint("همه‌ی پیامک‌های قبل از این روز.")
            }
            TimeMode.RANGE -> {
                DateField("از تاریخ", f.d1) { f.d1 = it }
                DateField("تا تاریخ (شامل همین روز)", f.d2) { f.d2 = it }
            }
            TimeMode.AGE -> {
                NumberField("تعداد روز", f.days) { f.days = it }
                Hint("پیامک‌هایی که از دریافت/ارسالشان بیش از این تعداد روز گذشته.")
            }
        }
    }

    Panel {
        Text("فرستنده و متن", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            f.sender, { f.sender = it }, Modifier.fillMaxWidth(),
            label = { Text("شماره یا نام فرستنده") }, singleLine = true,
        )
        OutlinedTextField(
            f.keyword, { f.keyword = it }, Modifier.fillMaxWidth(),
            label = { Text("متن پیام شامل") }, singleLine = true,
        )
        SwitchRow("استفاده از عبارت منظم (regex)", f.regex) { f.regex = it }
        Text("وضعیت خواندن")
        ChipRow { ReadState.entries.forEach { FilterChip(f.read == it, { f.read = it }, { Text(it.label) }) } }
        SwitchRow("فقط اسپم (تشخیص تبلیغات)", f.spam) { f.spam = it }
        if (f.spam) {
            Hint("پیامک‌های دریافتیِ تبلیغاتی (لغو ۱۱، تخفیف، جایزه، لینک و …). کد تأیید و رمز هرگز اسپم حساب نمی‌شوند؛ تشخیص بر پایه‌ی کلمات کلیدی است.")
        }
    }

    Panel {
        Text("تعداد", fontWeight = FontWeight.Bold)
        ChipRow { CountMode.entries.forEach { FilterChip(f.countMode == it, { f.countMode = it }, { Text(it.label) }) } }
        if (f.countMode != CountMode.NONE) {
            NumberField("تعداد", f.n) { f.n = it }
            Hint(
                if (f.countMode == CountMode.OLDEST) "فقط N پیامکِ قدیمی‌ترِ بین پیامک‌های مطابق شرط‌های بالا."
                else "از پیامک‌های مطابق شرط‌های بالا، N تای آخر نگه داشته و بقیه حذف می‌شوند."
            )
        }
    }
}
