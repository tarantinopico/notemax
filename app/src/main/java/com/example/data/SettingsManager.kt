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
}
