package com.marzec.featuretoggle

import com.marzec.cheatday.api.request.NewFeatureToggleDto
import com.marzec.cheatday.api.request.UpdateFeatureToggleDto
import com.marzec.cheatday.api.response.FeatureToggleDto

data class FeatureToggle(
    val id: Int,
    val name: String,
    val value: String
)

data class NewFeatureToggle(
    val name: String,
    val value: String
)

data class UpdateFeatureToggle(
    val name: String?,
    val value: String?
)

fun FeatureToggle.toDto() = FeatureToggleDto(
    id, name, value
)

fun NewFeatureToggle.toDto() = NewFeatureToggleDto(
    name, value
)

fun UpdateFeatureToggle.toDto() = UpdateFeatureToggleDto(
    name, value
)
