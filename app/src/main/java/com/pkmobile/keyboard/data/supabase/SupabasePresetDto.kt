package com.pkmobile.keyboard.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object untuk pemetaan tabel 'presets' di Supabase Cloud.
 */
@Serializable
data class SupabasePresetDto(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("is_active")
    val isActive: Boolean = false,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)
