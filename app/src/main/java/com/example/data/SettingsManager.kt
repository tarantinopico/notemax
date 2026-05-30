package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class InterfaceDensity { COZY, DEFAULT, COMPACT }

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean("use_dynamic_color", true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _interfaceDensity = MutableStateFlow(InterfaceDensity.valueOf(prefs.getString("interface_density", InterfaceDensity.DEFAULT.name) ?: InterfaceDensity.DEFAULT.name))
    val interfaceDensity: StateFlow<InterfaceDensity> = _interfaceDensity.asStateFlow()

    private val _isGlassmorphismEnabled = MutableStateFlow(prefs.getBoolean("glassmorphism_enabled", false))
    val isGlassmorphismEnabled: StateFlow<Boolean> = _isGlassmorphismEnabled.asStateFlow()

    private val _isUiTransparencyEnabled = MutableStateFlow(prefs.getBoolean("ui_transparency_enabled", false))
    val isUiTransparencyEnabled: StateFlow<Boolean> = _isUiTransparencyEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setUseDynamicColor(useDynamic: Boolean) {
        prefs.edit().putBoolean("use_dynamic_color", useDynamic).apply()
        _useDynamicColor.value = useDynamic
    }

    fun setInterfaceDensity(density: InterfaceDensity) {
        prefs.edit().putString("interface_density", density.name).apply()
        _interfaceDensity.value = density
    }

    fun setGlassmorphismEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("glassmorphism_enabled", enabled).apply()
        _isGlassmorphismEnabled.value = enabled
    }

    fun setUiTransparencyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ui_transparency_enabled", enabled).apply()
        _isUiTransparencyEnabled.value = enabled
    }
}
