package ltd.evilcorp.atox.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import javax.inject.Inject
import ltd.evilcorp.atox.BootReceiver
import ltd.evilcorp.domain.tox.ProxyType

class Settings @Inject constructor(private val ctx: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    var theme: Int
        get() = preferences.getInt("theme", 0)
        set(theme) {
            preferences.edit { putInt("theme", theme) }
            AppCompatDelegate.setDefaultNightMode(theme)
        }

    var udpEnabled: Boolean
        get() = preferences.getBoolean("udp_enabled", false)
        set(enabled) = preferences.edit().putBoolean("udp_enabled", enabled).apply()

    var runAtStartup: Boolean
        get() = ctx.packageManager.getComponentEnabledSetting(
            ComponentName(ctx, BootReceiver::class.java)
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        set(runAtStartup) {
            val state = if (runAtStartup) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            ctx.packageManager.setComponentEnabledSetting(
                ComponentName(ctx, BootReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP
            )
        }

    var proxyType: ProxyType
        get() = ProxyType.values()[preferences.getInt("proxy_type", 0)]
        set(type) = preferences.edit { putInt("proxy_type", type.ordinal) }

    var proxyAddress: String
        get() = preferences.getString("proxy_address", null) ?: ""
        set(address) = preferences.edit { putString("proxy_address", address) }

    var proxyPort: Int
        get() = preferences.getInt("proxy_port", 0)
        set(port) = preferences.edit { putInt("proxy_port", port) }
}
