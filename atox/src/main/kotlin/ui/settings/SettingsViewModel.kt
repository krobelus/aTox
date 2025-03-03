package ltd.evilcorp.atox.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.domain.tox.ProxyType
import ltd.evilcorp.domain.tox.SaveOptions
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.domain.tox.ToxSaveStatus
import ltd.evilcorp.domain.tox.testToxSave

enum class ProxyStatus {
    Good,
    BadPort,
    BadHost,
    BadType,
    NotFound,
}

class SettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val toxStarter: ToxStarter,
    private val tox: Tox
) : ViewModel() {
    private var restartNeeded = false

    private val _proxyStatus = MutableLiveData<ProxyStatus>()
    val proxyStatus: LiveData<ProxyStatus> get() = _proxyStatus

    private val _committed = MutableLiveData<Boolean>().apply { value = false }
    val committed: LiveData<Boolean> get() = _committed

    fun getTheme(): Int = settings.theme
    fun setTheme(theme: Int) {
        settings.theme = theme
    }

    fun getUdpEnabled(): Boolean = settings.udpEnabled
    fun setUdpEnabled(enabled: Boolean) {
        settings.udpEnabled = enabled
        restartNeeded = true
    }

    fun getRunAtStartup(): Boolean = settings.runAtStartup
    fun setRunAtStartup(enabled: Boolean) {
        settings.runAtStartup = enabled
    }

    fun commit() {
        if (!restartNeeded) {
            _committed.value = true
            return
        }

        toxStarter.stopTox()

        viewModelScope.launch {
            while (tox.started) {
                delay(200)
            }
            toxStarter.tryLoadTox()
            _committed.value = true
        }
    }

    private var checkProxyJob: Job? = null
    fun checkProxy() {
        checkProxyJob?.cancel(null)
        checkProxyJob = viewModelScope.launch(Dispatchers.IO) {
            val saveStatus = testToxSave(
                SaveOptions(saveData = null, getUdpEnabled(), getProxyType(), getProxyAddress(), getProxyPort())
            )

            val proxyStatus = when (saveStatus) {
                ToxSaveStatus.BadProxyHost -> ProxyStatus.BadHost
                ToxSaveStatus.BadProxyPort -> ProxyStatus.BadPort
                ToxSaveStatus.BadProxyType -> ProxyStatus.BadType
                ToxSaveStatus.ProxyNotFound -> ProxyStatus.NotFound
                else -> ProxyStatus.Good
            }

            _proxyStatus.postValue(proxyStatus)
        }
    }

    fun getProxyType(): ProxyType = settings.proxyType
    fun setProxyType(type: ProxyType) {
        if (type != getProxyType()) {
            settings.proxyType = type
            restartNeeded = true
            checkProxy()
        }
    }

    fun getProxyAddress(): String = settings.proxyAddress
    fun setProxyAddress(address: String) {
        if (address != getProxyAddress()) {
            settings.proxyAddress = address
            if (getProxyType() != ProxyType.None) {
                restartNeeded = true
            }
            checkProxy()
        }
    }

    fun getProxyPort(): Int = settings.proxyPort
    fun setProxyPort(port: Int) {
        if (port != getProxyPort()) {
            settings.proxyPort = port
            if (getProxyType() != ProxyType.None) {
                restartNeeded = true
            }
            checkProxy()
        }
    }
}
