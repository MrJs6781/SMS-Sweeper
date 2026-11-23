package app.smssweeper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TimeMode(val label: String) {
    NONE("همه‌ی زمان‌ها"), BEFORE("قبل از تاریخ"), RANGE("بازه تاریخی"), AGE("قدیمی‌تر از … روز")
}

enum class CountMode(val label: String) {
    NONE("بدون محدودیت"), OLDEST("قدیمی‌ترین‌ها"), KEEP("نگه داشتن آخرین‌ها")
}

/** Editable filter form. [criteria] is null while the form is incomplete or invalid. */
class Form {
    var box by mutableStateOf(Box.ALL)
    var timeMode by mutableStateOf(TimeMode.NONE)
    var d1 by mutableStateOf<Long?>(null)
    var d2 by mutableStateOf<Long?>(null)
    var days by mutableStateOf("")
    var sender by mutableStateOf("")
    var keyword by mutableStateOf("")
    var regex by mutableStateOf(false)
    var read by mutableStateOf(ReadState.ANY)
    var spam by mutableStateOf(false)
    var countMode by mutableStateOf(CountMode.NONE)
    var n by mutableStateOf("")

    private fun build() = Criteria(
        box = box,
        before = if (timeMode == TimeMode.BEFORE) d1 else null,
        from = if (timeMode == TimeMode.RANGE) d1 else null,
        to = if (timeMode == TimeMode.RANGE) d2?.let(::dayAfter) else null,
        olderThanDays = if (timeMode == TimeMode.AGE) digits(days) else null,
        sender = sender,
        keyword = keyword,
        regex = regex,
        read = read,
        spam = spam,
        oldestN = if (countMode == CountMode.OLDEST) digits(n) else null,
        keepLatestN = if (countMode == CountMode.KEEP) digits(n) else null,
    )

    /** Why the form can't run yet, or null when it can. */
    val problem: String?
        get() {
            val a = d1
            val b = d2
            return when {
                timeMode == TimeMode.BEFORE && a == null -> "تاریخ را انتخاب کن."
                timeMode == TimeMode.RANGE && (a == null || b == null) -> "تاریخ شروع و پایان را انتخاب کن."
                timeMode == TimeMode.RANGE && b!! < a!! -> "تاریخ پایان باید بعد از شروع باشد."
                timeMode == TimeMode.AGE && (digits(days) ?: 0) <= 0 -> "تعداد روز را وارد کن."
                countMode != CountMode.NONE && (digits(n) ?: 0) <= 0 -> "تعداد را وارد کن."
                regex && !(validRegex(sender) && validRegex(keyword)) -> "عبارت منظم (regex) نامعتبر است."
                !build().hasCondition -> "حداقل یک شرط تعیین کن."
                else -> null
            }
        }

    val criteria: Criteria? get() = if (problem == null) build() else null
}
