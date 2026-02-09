package com.safeguard.sos.domain.model

import androidx.annotation.DrawableRes

data class SafetyTip(
    val id: String,
    val title: String,
    val content: String,
    @DrawableRes val iconRes: Int
)