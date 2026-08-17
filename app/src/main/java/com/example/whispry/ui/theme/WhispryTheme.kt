// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

object WhispryTokens {
    val Black = Color(0xFF000000)
    val DeepVoid = Color(0xFF08080F)        // background base
    val SurfaceGlass = Color(0x0FFFFFFF)    // 6% white — glass card fill
    val GlassBorder = Color(0x1AFFFFFF)     // 10% white — card border

    // Translucent elevated panel fill. Replaces the old opaque 0xFF1C1C1E so the shared
    // accent-glow background bleeds through and content panels feel like one glass system
    // across every tab (instead of separate flat black boxes). ~82% alpha = readable but alive.
    val SurfaceElevated = Color(0xD11C1C1E)
    
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x99FFFFFF)   // 60% white
    val TextTertiary = Color(0x61FFFFFF)    // 38% white
    
    val SuccessGreen = Color(0xFF34C759)
    val ErrorSoft = Color(0xFFFF6B6B)

    // Spacing
    val SpacingBase = 4.dp
    
    // Radii
    val RadiusSmall = 12.dp
    val RadiusMedium = 20.dp
    val RadiusLarge = 28.dp
    val RadiusPill = 999.dp
}


enum class AccentPreset(val label: String, val mainColor: Color, val softColor: Color, val glowColor: Color) {

    // ── Original 8 ────────────────────────────────────────────────────────────

    Purple(
        "Royal Purple",
        Color(0xFF7B6BFF),
        Color(0xFFB09FFF),
        Color(0x407B6BFF)
    ),
    Blue(
        "Ocean Blue",
        Color(0xFF4B9FFF),
        Color(0xFF9FD0FF),
        Color(0x404B9FFF)
    ),
    Rose(
        "Sunset Rose",
        Color(0xFFFF6B9D),
        Color(0xFFFFB2CC),
        Color(0x40FF6B9D)
    ),
    Emerald(
        "Emerald Isle",
        Color(0xFF34C759),
        Color(0xFF92E6A7),
        Color(0x4034C759)
    ),
    Amber(
        "Vibrant Amber",
        Color(0xFFFFB340),
        Color(0xFFFFD999),
        Color(0x40FFB340)
    ),
    Cyan(
        "Electric Cyan",
        Color(0xFF00E5FF),
        Color(0xFF80F3FF),
        Color(0x4000E5FF)
    ),
    Crimson(
        "Crimson Red",
        Color(0xFFFF2D55),
        Color(0xFFFF859D),
        Color(0x40FF2D55)
    ),
    Slate(
        "Modern Slate",
        Color(0xFF8E8E93),
        Color(0xFFC7C7CC),
        Color(0x408E8E93)
    ),

    // ── Batch 2 ───────────────────────────────────────────────────────────────

    Coral(
        "Living Coral",
        Color(0xFFFF6B6B),
        Color(0xFFFFADAD),
        Color(0x40FF6B6B)
    ),
    Mint(
        "Glacial Mint",
        Color(0xFF00C9A7),
        Color(0xFF7FFFD4),
        Color(0x4000C9A7)
    ),
    Violet(
        "Neon Violet",
        Color(0xFFBF5AF2),
        Color(0xFFDFABFF),
        Color(0x40BF5AF2)
    ),
    Gold(
        "Solar Gold",
        Color(0xFFFFD60A),
        Color(0xFFFFEA70),
        Color(0x40FFD60A)
    ),
    Indigo(
        "Deep Indigo",
        Color(0xFF5856D6),
        Color(0xFFA5A4F0),
        Color(0x405856D6)
    ),
    Peach(
        "Warm Peach",
        Color(0xFFFF9F6B),
        Color(0xFFFFCCA8),
        Color(0x40FF9F6B)
    ),
    Teal(
        "Arctic Teal",
        Color(0xFF30B0C7),
        Color(0xFF8ADDE8),
        Color(0x4030B0C7)
    ),
    Lime(
        "Acid Lime",
        Color(0xFFB2FF00),
        Color(0xFFDCFF80),
        Color(0x40B2FF00)
    ),
    Blush(
        "Dusty Blush",
        Color(0xFFFF8FAB),
        Color(0xFFFFBDD0),
        Color(0x40FF8FAB)
    ),
    Sky(
        "Powder Sky",
        Color(0xFF40C8E0),
        Color(0xFF96E8F5),
        Color(0x4040C8E0)
    ),
    Lavender(
        "Soft Lavender",
        Color(0xFFAA8FFF),
        Color(0xFFD4C3FF),
        Color(0x40AA8FFF)
    ),
    Tangerine(
        "Neon Tangerine",
        Color(0xFFFF8C00),
        Color(0xFFFFBE6B),
        Color(0x40FF8C00)
    ),
    Sage(
        "Forest Sage",
        Color(0xFF6DB07A),
        Color(0xFFADD5B4),
        Color(0x406DB07A)
    ),
    IceWhite(
        "Ice White",
        Color(0xFFE8F4FF),
        Color(0xFFF4FAFF),
        Color(0x40B8D9FF)
    ),

    // ── Batch 3 · Gemstones ───────────────────────────────────────────────────

    Sapphire(
        "Deep Sapphire",
        Color(0xFF0F52BA),
        Color(0xFF6B9FE4),
        Color(0x400F52BA)
    ),
    Ruby(
        "Burning Ruby",
        Color(0xFFE0115F),
        Color(0xFFF08080),
        Color(0x40E0115F)
    ),
    Topaz(
        "Golden Topaz",
        Color(0xFFFFC200),
        Color(0xFFFFE066),
        Color(0x40FFC200)
    ),
    Opal(
        "Mystic Opal",
        Color(0xFFA8D8EA),
        Color(0xFFD6EEF8),
        Color(0x40A8D8EA)
    ),
    Amethyst(
        "Dark Amethyst",
        Color(0xFF9B59B6),
        Color(0xFFD2A8E0),
        Color(0x409B59B6)
    ),
    Jade(
        "Imperial Jade",
        Color(0xFF00A86B),
        Color(0xFF71D9AE),
        Color(0x4000A86B)
    ),

    // ── Batch 3 · Aurora & Cosmic ─────────────────────────────────────────────

    AuroraPink(
        "Aurora Pink",
        Color(0xFFFF61D8),
        Color(0xFFFFB3ED),
        Color(0x40FF61D8)
    ),
    AuroraGreen(
        "Aurora Green",
        Color(0xFF00FF87),
        Color(0xFF80FFCC),
        Color(0x4000FF87)
    ),
    Nebula(
        "Nebula Dream",
        Color(0xFF8A2BE2),
        Color(0xFFBD80FF),
        Color(0x408A2BE2)
    ),
    Starlight(
        "Starlight",
        Color(0xFFB0C4DE),
        Color(0xFFDCE8F5),
        Color(0x40B0C4DE)
    ),
    Supernova(
        "Supernova",
        Color(0xFFFF4500),
        Color(0xFFFF9070),
        Color(0x40FF4500)
    ),
    CosmicBlue(
        "Cosmic Blue",
        Color(0xFF1B2CC1),
        Color(0xFF7B8FFF),
        Color(0x401B2CC1)
    ),

    // ── Batch 3 · Luxury & Mood ───────────────────────────────────────────────

    Champagne(
        "Champagne",
        Color(0xFFF7E7CE),
        Color(0xFFFAF2E4),
        Color(0x40D4A853)
    ),
    RoseGold(
        "Rose Gold",
        Color(0xFFE8A598),
        Color(0xFFF4D0CB),
        Color(0x40E8A598)
    ),
    Obsidian(
        "Obsidian",
        Color(0xFF4A4A6A),
        Color(0xFF8888AA),
        Color(0x404A4A6A)
    ),
    Platinum(
        "Platinum",
        Color(0xFFD5D8DC),
        Color(0xFFEEF0F2),
        Color(0x40D5D8DC)
    ),
    Mahogany(
        "Dark Mahogany",
        Color(0xFFC04000),
        Color(0xFFE8936B),
        Color(0x40C04000)
    ),
    Velvet(
        "Velvet Night",
        Color(0xFF6A0572),
        Color(0xFFB565BF),
        Color(0x406A0572)
    ),

    // ── Batch 3 · Nature & Elements ───────────────────────────────────────────

    Magma(
        "Molten Magma",
        Color(0xFFFF3D00),
        Color(0xFFFF9E80),
        Color(0x40FF3D00)
    ),
    DeepSea(
        "Deep Sea",
        Color(0xFF006994),
        Color(0xFF5BBCD6),
        Color(0x40006994)
    ),
    Blossom(
        "Cherry Blossom",
        Color(0xFFFFB7C5),
        Color(0xFFFFDDE4),
        Color(0x40FFB7C5)
    ),
    Dusk(
        "Desert Dusk",
        Color(0xFFE07B54),
        Color(0xFFF0B898),
        Color(0x40E07B54)
    )
}

/** Prefix marking a persisted accent value as a user-picked custom color rather than an [AccentPreset] name — serialized as "CUSTOM:AARRGGBB". */
private const val CUSTOM_ACCENT_PREFIX = "CUSTOM:"

/** The three colors [WhispryTheme] needs, whether sourced from a curated [AccentPreset] or a custom pick. */
data class AccentColorSet(val mainColor: Color, val softColor: Color, val glowColor: Color)

private val AccentPreset.colorSet: AccentColorSet
    get() = AccentColorSet(mainColor, softColor, glowColor)

fun serializeCustomAccent(color: Color): String =
    CUSTOM_ACCENT_PREFIX + color.toArgb().toUInt().toString(16).padStart(8, '0')

fun isCustomAccent(value: String?): Boolean = value != null && value.startsWith(CUSTOM_ACCENT_PREFIX)

/** Null when [value] isn't a custom accent or fails to parse. */
fun parseCustomAccentColor(value: String?): Color? {
    if (!isCustomAccent(value)) return null
    val argb = value!!.removePrefix(CUSTOM_ACCENT_PREFIX).toULongOrNull(16) ?: return null
    return Color(argb.toInt())
}

/** Resolves a persisted accent value — an [AccentPreset] name or a "CUSTOM:AARRGGBB" hex from
 *  the full color picker — into the colors [WhispryTheme] needs. Soft/glow are derived from the
 *  custom main color the same way the presets do it: a lighten toward white, a 25%-alpha glow. */
fun resolveAccentColors(value: String?): AccentColorSet {
    parseCustomAccentColor(value)?.let { main ->
        return AccentColorSet(
            mainColor = main,
            softColor = lerp(main, Color.White, 0.35f),
            glowColor = main.copy(alpha = 0.25f)
        )
    }
    return (AccentPreset.entries.find { it.name == value } ?: AccentPreset.Purple).colorSet
}

@Stable
class WhispryColors(
    accent: Color,
    accentSoft: Color,
    accentGlow: Color,
    isDark: Boolean
) {
    var accent by mutableStateOf(accent)
        private set
    var accentSoft by mutableStateOf(accentSoft)
        private set
    var accentGlow by mutableStateOf(accentGlow)
        private set
    var isDark by mutableStateOf(isDark)
        private set

    fun update(other: WhispryColors) {
        accent = other.accent
        accentSoft = other.accentSoft
        accentGlow = other.accentGlow
        isDark = other.isDark
    }

    fun copy(
        accent: Color = this.accent,
        accentSoft: Color = this.accentSoft,
        accentGlow: Color = this.accentGlow,
        isDark: Boolean = this.isDark
    ): WhispryColors = WhispryColors(accent, accentSoft, accentGlow, isDark)
}

val LocalWhispryColors = staticCompositionLocalOf {
    WhispryColors(
        accent = Color(0xFF7B6BFF),
        accentSoft = Color(0xFFB09FFF),
        accentGlow = Color(0x40826BFF),
        isDark = true
    )
}

@Composable
fun WhispryTheme(
    accentColors: AccentColorSet = AccentPreset.Purple.colorSet,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = remember {
        WhispryColors(
            accent = accentColors.mainColor,
            accentSoft = accentColors.softColor,
            accentGlow = accentColors.glowColor,
            isDark = darkTheme
        )
    }

    SideEffect {
        colors.update(
            WhispryColors(
                accent = accentColors.mainColor,
                accentSoft = accentColors.softColor,
                accentGlow = accentColors.glowColor,
                isDark = darkTheme
            )
        )
    }

    val colorScheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = Color.Black,
        secondary = colors.accentSoft,
        background = WhispryTokens.DeepVoid,
        surface = WhispryTokens.DeepVoid,
    )

    CompositionLocalProvider(LocalWhispryColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

object WhispryTheme {
    val colors: WhispryColors
        @Composable
        get() = LocalWhispryColors.current
}
