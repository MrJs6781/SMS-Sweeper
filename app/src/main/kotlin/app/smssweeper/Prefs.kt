package app.smssweeper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Tiny SharedPreferences wrapper. Lists are stored newline-separated. */
object Prefs {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("sweeper", Context.MODE_PRIVATE)

    private fun list(ctx: Context, key: String): List<String> =
        sp(ctx).getString(key, "").orEmpty().split('\n').filter { it.isNotBlank() }

    private fun put(ctx: Context, key: String, v: List<String>) =
        sp(ctx).edit().putString(key, v.joinToString("\n")).apply()

    fun lists(ctx: Context) = Lists(list(ctx, "protected"), list(ctx, "blocked"), list(ctx, "spamWords"))

    fun presets(ctx: Context): List<Preset> = runCatching {
        val a = JSONArray(sp(ctx).getString("presets", "[]"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Preset(o.getString("name"), criteriaFromJson(o.getJSONObject("criteria")))
        }
    }.getOrDefault(emptyList())

    fun savePresets(ctx: Context, ps: List<Preset>) {
        val a = JSONArray()
        ps.forEach { a.put(JSONObject().put("name", it.name).put("criteria", it.criteria.toJson())) }
        sp(ctx).edit().putString("presets", a.toString()).apply()
    }

    fun saveLists(ctx: Context, l: Lists) {
        put(ctx, "protected", l.protectedSenders)
        put(ctx, "blocked", l.blockedSenders)
        put(ctx, "spamWords", l.spamWords)
    }
}
