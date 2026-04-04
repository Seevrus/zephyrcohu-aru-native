package com.zephyr.boreal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.zephyr.boreal.R

/**
 * Google Fonts provider configuration.
 */
val provider =
  GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
  )

/**
 * Font Families
 */
val NunitoSansFont = GoogleFont("Nunito Sans")
val NunitoSansFamily =
  FontFamily(
    Font(googleFont = NunitoSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NunitoSansFont, fontProvider = provider, weight = FontWeight.Bold),
  )

val RobotoFont = GoogleFont("Roboto")
val RobotoFamily =
  FontFamily(
    Font(googleFont = RobotoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = RobotoFont, fontProvider = provider, weight = FontWeight.Bold),
  )

/**
 * Boreal Typography definitions.
 */
val BorealTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = NunitoSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BorealFontSizes.Title,
        color = BorealColors.White,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = NunitoSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BorealFontSizes.Subtitle,
        color = BorealColors.White,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = NunitoSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = BorealFontSizes.Body,
        color = BorealColors.White,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BorealFontSizes.Body,
        color = BorealColors.Blue200,
      ),
    labelSmall =
      TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BorealFontSizes.SmallText,
      ),
  )
