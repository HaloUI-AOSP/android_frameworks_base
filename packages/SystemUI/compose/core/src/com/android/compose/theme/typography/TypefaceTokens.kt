/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalTextApi::class)

package com.android.compose.theme.typography

import android.content.Context
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

internal class TypefaceTokens(typefaceNames: TypefaceNames) {
    companion object {
        val WeightMedium = FontWeight.Medium
        val WeightRegular = FontWeight.Normal
    }

    private val brandFont = DeviceFontFamilyName(typefaceNames.brand)
    private val plainFont = DeviceFontFamilyName(typefaceNames.plain)

    // Google Sans Flex emphasized styles
    private val displayLargeEmphasizedFont =
        DeviceFontFamilyName(typefaceNames.displayLargeEmphasized)
    private val displayMediumEmphasizedFont =
        DeviceFontFamilyName(typefaceNames.displayMediumEmphasized)
    private val displaySmallEmphasizedFont =
        DeviceFontFamilyName(typefaceNames.displaySmallEmphasized)
    private val headlineLargeEmphasizedFont =
        DeviceFontFamilyName(typefaceNames.headlineLargeEmphasized)
    private val headlineMediumEmphasizedFont =
        DeviceFontFamilyName(typefaceNames.headlineMediumEmphasized)
    private val headlineSmallEmphasizedFont =
        DeviceFontFamilyName(typefaceNames.headlineSmallEmphasized)
    private val titleLargeEmphasizedFont = DeviceFontFamilyName(typefaceNames.titleLargeEmphasized)
    private val titleMediumEmphasizedFont = DeviceFontFamilyName(typefaceNames.titleMediumEmphasized)
    private val titleSmallEmphasizedFont = DeviceFontFamilyName(typefaceNames.titleSmallEmphasized)
    private val bodyLargeEmphasizedFont = DeviceFontFamilyName(typefaceNames.bodyLargeEmphasized)
    private val bodyMediumEmphasizedFont = DeviceFontFamilyName(typefaceNames.bodyMediumEmphasized)
    private val bodySmallEmphasizedFont = DeviceFontFamilyName(typefaceNames.bodySmallEmphasized)
    private val labelLargeEmphasizedFont = DeviceFontFamilyName(typefaceNames.labelLargeEmphasized)
    private val labelMediumEmphasizedFont = DeviceFontFamilyName(typefaceNames.labelMediumEmphasized)
    private val labelSmallEmphasizedFont = DeviceFontFamilyName(typefaceNames.labelSmallEmphasized)

    val brand =
        FontFamily(
            Font(brandFont, weight = WeightMedium),
            Font(brandFont, weight = WeightRegular),
        )
    val plain =
        FontFamily(
            Font(plainFont, weight = WeightMedium),
            Font(plainFont, weight = WeightRegular),
        )

    val displayLargeEmphasized = FontFamily(Font(displayLargeEmphasizedFont))
    val displayMediumEmphasized = FontFamily(Font(displayMediumEmphasizedFont))
    val displaySmallEmphasized = FontFamily(Font(displaySmallEmphasizedFont))
    val headlineLargeEmphasized = FontFamily(Font(headlineLargeEmphasizedFont))
    val headlineMediumEmphasized = FontFamily(Font(headlineMediumEmphasizedFont))
    val headlineSmallEmphasized = FontFamily(Font(headlineSmallEmphasizedFont))
    val titleLargeEmphasized = FontFamily(Font(titleLargeEmphasizedFont))
    val titleMediumEmphasized = FontFamily(Font(titleMediumEmphasizedFont))
    val titleSmallEmphasized = FontFamily(Font(titleSmallEmphasizedFont))
    val bodyLargeEmphasized = FontFamily(Font(bodyLargeEmphasizedFont))
    val bodyMediumEmphasized = FontFamily(Font(bodyMediumEmphasizedFont))
    val bodySmallEmphasized = FontFamily(Font(bodySmallEmphasizedFont))
    val labelLargeEmphasized = FontFamily(Font(labelLargeEmphasizedFont))
    val labelMediumEmphasized = FontFamily(Font(labelMediumEmphasizedFont))
    val labelSmallEmphasized = FontFamily(Font(labelSmallEmphasizedFont))
}

internal data class TypefaceNames
private constructor(
    val brand: String,
    val plain: String,
    val displayLargeEmphasized: String,
    val displayMediumEmphasized: String,
    val displaySmallEmphasized: String,
    val headlineLargeEmphasized: String,
    val headlineMediumEmphasized: String,
    val headlineSmallEmphasized: String,
    val titleLargeEmphasized: String,
    val titleMediumEmphasized: String,
    val titleSmallEmphasized: String,
    val bodyLargeEmphasized: String,
    val bodyMediumEmphasized: String,
    val bodySmallEmphasized: String,
    val labelLargeEmphasized: String,
    val labelMediumEmphasized: String,
    val labelSmallEmphasized: String,
) {
    private enum class Config(val configName: String, val default: String) {
        Brand("config_headlineFontFamily", "sans-serif"),
        Plain("config_bodyFontFamily", "sans-serif"),
        DisplayLargeEmphasized(
            "config_displayLargeEmphasizedFontFamily",
            "variable-display-large-emphasized",
        ),
        DisplayMediumEmphasized(
            "config_displayMediumEmphasizedFontFamily",
            "variable-display-medium-emphasized",
        ),
        DisplaySmallEmphasized(
            "config_displaySmallEmphasizedFontFamily",
            "variable-display-small-emphasized",
        ),
        HeadlineLargeEmphasized(
            "config_headlineLargeEmphasizedFontFamily",
            "variable-headline-large-emphasized",
        ),
        HeadlineMediumEmphasized(
            "config_headlineMediumEmphasizedFontFamily",
            "variable-headline-medium-emphasized",
        ),
        HeadlineSmallEmphasized(
            "config_headlineSmallEmphasizedFontFamily",
            "variable-headline-small-emphasized",
        ),
        TitleLargeEmphasized(
            "config_titleLargeEmphasizedFontFamily",
            "variable-title-large-emphasized",
        ),
        TitleMediumEmphasized(
            "config_titleMediumEmphasizedFontFamily",
            "variable-title-medium-emphasized",
        ),
        TitleSmallEmphasized(
            "config_titleSmallEmphasizedFontFamily",
            "variable-title-small-emphasized",
        ),
        BodyLargeEmphasized(
            "config_bodyLargeEmphasizedFontFamily",
            "variable-body-large-emphasized",
        ),
        BodyMediumEmphasized(
            "config_bodyMediumEmphasizedFontFamily",
            "variable-body-medium-emphasized",
        ),
        BodySmallEmphasized(
            "config_bodySmallEmphasizedFontFamily",
            "variable-body-small-emphasized",
        ),
        LabelLargeEmphasized(
            "config_labelLargeEmphasizedFontFamily",
            "variable-label-large-emphasized",
        ),
        LabelMediumEmphasized(
            "config_labelMediumEmphasizedFontFamily",
            "variable-label-medium-emphasized",
        ),
        LabelSmallEmphasized(
            "config_labelSmallEmphasizedFontFamily",
            "variable-label-small-emphasized",
        ),
    }

    companion object {
        fun get(context: Context): TypefaceNames {
            return TypefaceNames(
                brand = getTypefaceName(context, Config.Brand),
                plain = getTypefaceName(context, Config.Plain),
                displayLargeEmphasized = getTypefaceName(context, Config.DisplayLargeEmphasized),
                displayMediumEmphasized = getTypefaceName(context, Config.DisplayMediumEmphasized),
                displaySmallEmphasized = getTypefaceName(context, Config.DisplaySmallEmphasized),
                headlineLargeEmphasized = getTypefaceName(context, Config.HeadlineLargeEmphasized),
                headlineMediumEmphasized = getTypefaceName(context, Config.HeadlineMediumEmphasized),
                headlineSmallEmphasized = getTypefaceName(context, Config.HeadlineSmallEmphasized),
                titleLargeEmphasized = getTypefaceName(context, Config.TitleLargeEmphasized),
                titleMediumEmphasized = getTypefaceName(context, Config.TitleMediumEmphasized),
                titleSmallEmphasized = getTypefaceName(context, Config.TitleSmallEmphasized),
                bodyLargeEmphasized = getTypefaceName(context, Config.BodyLargeEmphasized),
                bodyMediumEmphasized = getTypefaceName(context, Config.BodyMediumEmphasized),
                bodySmallEmphasized = getTypefaceName(context, Config.BodySmallEmphasized),
                labelLargeEmphasized = getTypefaceName(context, Config.LabelLargeEmphasized),
                labelMediumEmphasized = getTypefaceName(context, Config.LabelMediumEmphasized),
                labelSmallEmphasized = getTypefaceName(context, Config.LabelSmallEmphasized),
            )
        }

        private fun getTypefaceName(context: Context, config: Config): String {
            return context
                .getString(context.resources.getIdentifier(config.configName, "string", "android"))
                .takeIf { it.isNotEmpty() }
                ?: config.default
        }
    }
}
