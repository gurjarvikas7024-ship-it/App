package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Sky Blue Theme Palette
val SkyBlueContainer = Color(0xFFE0F2FE)       // #E0F2FE Primary Container
val OceanBlueAccent = Color(0xFF0284C7)        // #0284C7 Primary Accent / Buttons
val OceanBlueDark = Color(0xFF0369A1)          // Darker variant for active/pressed
val CleanPureWhite = Color(0xFFFFFFFF)         // #FFFFFF Background & Card Surface
val DeepSlateNavy = Color(0xFF0F172A)          // #0F172A Primary Text & Icons
val SlateMutedText = Color(0xFF64748B)         // Secondary Text
val SkyBorderColor = Color(0xFFBAE6FD)         // Soft Border
val LightSurfaceBg = Color(0xFFF8FAFC)         // Subtle page background

// Status Accents
val SuccessGreen = Color(0xFF16A34A)
val SuccessGreenBg = Color(0xFFDCFCE7)
val WarningAmber = Color(0xFFD97706)
val WarningAmberBg = Color(0xFFFEF3C7)
val UrgentRed = Color(0xFFDC2626)
val UrgentRedBg = Color(0xFFFEE2E2)

// Category Colors
val CategoryStudy = Color(0xFF0284C7)
val CategoryMedicine = Color(0xFFDC2626)
val CategoryOffice = Color(0xFF0D9488)
val CategoryMeeting = Color(0xFF9333EA)
val CategoryShopping = Color(0xFFD97706)
val CategoryBirthday = Color(0xFFDB2777)
val CategoryExercise = Color(0xFF16A34A)
val CategoryWater = Color(0xFF0891B2)
val CategoryPrayer = Color(0xFF4F46E5)
val CategoryPersonal = Color(0xFF7C3AED)

// Theme Compatibility Mapping (Ensures all UI elements adopt Sky Blue & White theme)
val AppBackgroundDark = LightSurfaceBg
val AppCardDark = CleanPureWhite
val AppCardBorderDark = SkyBorderColor
val OrangeAccent = OceanBlueAccent
val OrangeBannerBg = SkyBlueContainer
val OrangeBannerBorder = SkyBorderColor
val GreenAccent = SuccessGreen
val YellowPendingText = WarningAmber
val YellowPendingBg = WarningAmberBg
val RedAccentBar = UrgentRed
val TextPrimary = DeepSlateNavy
val TextSecondary = SlateMutedText
val BluePrimary = OceanBlueAccent
val BlueDark = OceanBlueDark
val BlueLight = SkyBlueContainer

val AmberAccent = OceanBlueAccent
val IndigoPrimary = OceanBlueAccent
val IndigoDark = OceanBlueDark
val IndigoLight = SkyBlueContainer
