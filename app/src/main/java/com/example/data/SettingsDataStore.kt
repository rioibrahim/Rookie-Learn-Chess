package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rookieDataStore: DataStore<Preferences> by preferencesDataStore(name = "rookie_settings")

enum class AnimationSpeed(val index: Int, val label: String, val durationMs: Int) {
  SLOW(0, "Slow", 400),
  NORMAL(1, "Normal", 240),
  FAST(2, "Fast", 140);

  companion object {
    fun fromIndex(idx: Int): AnimationSpeed = entries.find { it.index == idx } ?: NORMAL
  }
}

enum class AppThemeMode(val index: Int, val label: String) {
  DARK(0, "Dark"),
  LIGHT(1, "Light"),
  SYSTEM(2, "System");

  companion object {
    fun fromIndex(idx: Int): AppThemeMode = entries.find { it.index == idx } ?: DARK
  }
}

data class UserSettings(
  val animationsEnabled: Boolean = true,
  val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
  val captureEffects: Boolean = true,
  val highlightsEnabled: Boolean = true,
  val arrowsEnabled: Boolean = true,
  val clearDrawingsOnMove: Boolean = true,
  val reducedMotion: Boolean = false,
  val muted: Boolean = false,
  val volume: Float = 0.6f,
  val beginnerMode: Boolean = true,
  val themeMode: AppThemeMode = AppThemeMode.DARK,
) {
  val effectiveAnimationDurationMs: Int
    get() = if (animationsEnabled && !reducedMotion) animationSpeed.durationMs else 0
}

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val ANIMATIONS = booleanPreferencesKey("animations")
    val ANIM_SPEED = intPreferencesKey("anim_speed")
    val CAPTURE_FX = booleanPreferencesKey("capture_fx")
    val HIGHLIGHTS = booleanPreferencesKey("highlights")
    val ARROWS = booleanPreferencesKey("arrows")
    val CLEAR_ON_MOVE = booleanPreferencesKey("clear_on_move")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val MUTED = booleanPreferencesKey("muted")
    val VOLUME = floatPreferencesKey("volume")
    val BEGINNER_MODE = booleanPreferencesKey("beginner_mode")
    val THEME_MODE = intPreferencesKey("theme_mode")
  }

  val settingsFlow: Flow<UserSettings> = context.rookieDataStore.data.map { prefs ->
    UserSettings(
      animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
      animationSpeed = AnimationSpeed.fromIndex(prefs[Keys.ANIM_SPEED] ?: AnimationSpeed.NORMAL.index),
      captureEffects = prefs[Keys.CAPTURE_FX] ?: true,
      highlightsEnabled = prefs[Keys.HIGHLIGHTS] ?: true,
      arrowsEnabled = prefs[Keys.ARROWS] ?: true,
      clearDrawingsOnMove = prefs[Keys.CLEAR_ON_MOVE] ?: true,
      reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
      muted = prefs[Keys.MUTED] ?: false,
      volume = (prefs[Keys.VOLUME] ?: 0.6f).coerceIn(0f, 1f),
      beginnerMode = prefs[Keys.BEGINNER_MODE] ?: true,
      themeMode = AppThemeMode.fromIndex(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.index),
    )
  }

  suspend fun update(transform: (UserSettings) -> UserSettings) {
    context.rookieDataStore.edit { prefs ->
      val current = UserSettings(
        animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
        animationSpeed = AnimationSpeed.fromIndex(prefs[Keys.ANIM_SPEED] ?: AnimationSpeed.NORMAL.index),
        captureEffects = prefs[Keys.CAPTURE_FX] ?: true,
        highlightsEnabled = prefs[Keys.HIGHLIGHTS] ?: true,
        arrowsEnabled = prefs[Keys.ARROWS] ?: true,
        clearDrawingsOnMove = prefs[Keys.CLEAR_ON_MOVE] ?: true,
        reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
        muted = prefs[Keys.MUTED] ?: false,
        volume = (prefs[Keys.VOLUME] ?: 0.6f).coerceIn(0f, 1f),
        beginnerMode = prefs[Keys.BEGINNER_MODE] ?: true,
        themeMode = AppThemeMode.fromIndex(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.index),
      )
      val next = transform(current)
      prefs[Keys.ANIMATIONS] = next.animationsEnabled
      prefs[Keys.ANIM_SPEED] = next.animationSpeed.index
      prefs[Keys.CAPTURE_FX] = next.captureEffects
      prefs[Keys.HIGHLIGHTS] = next.highlightsEnabled
      prefs[Keys.ARROWS] = next.arrowsEnabled
      prefs[Keys.CLEAR_ON_MOVE] = next.clearDrawingsOnMove
      prefs[Keys.REDUCED_MOTION] = next.reducedMotion
      prefs[Keys.MUTED] = next.muted
      prefs[Keys.VOLUME] = next.volume.coerceIn(0f, 1f)
      prefs[Keys.BEGINNER_MODE] = next.beginnerMode
      prefs[Keys.THEME_MODE] = next.themeMode.index
    }
  }
}
