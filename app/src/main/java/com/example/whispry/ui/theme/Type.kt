package com.example.whispry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.whispry.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFont = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold)
)

val PlayfairFont = FontFamily(
    Font(googleFont = GoogleFont("Playfair Display"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Playfair Display"), fontProvider = provider, weight = FontWeight.Bold)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = Color(0x99FFFFFF)
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = Color(0x99FFFFFF)
    ),
    titleLarge = TextStyle(
        fontFamily = PlayfairFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = Color.White
    ),
    titleMedium = TextStyle(
        fontFamily = PlayfairFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = Color.White
    ),
    labelSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = Color(0x61FFFFFF)
    ),
    labelLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = Color(0x99FFFFFF)
    )
)
