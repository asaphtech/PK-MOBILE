package com.pkmobile.keyboard.data.supabase

import com.pkmobile.keyboard.data.db.ShortcutEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object untuk pemetaan tabel 'shortcuts' di Supabase.
 */
@Serializable
data class SupabaseShortcutDto(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("shortcut")
    val shortcut: String,

    @SerialName("expansion")
    val expansion: String,

    @SerialName("expansion_mode")
    val expansionMode: String = "INSTANT",

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toEntity(): ShortcutEntity {
        return ShortcutEntity(
            shortcut = shortcut,
            expansion = expansion,
            expansionMode = expansionMode
        )
    }

    companion object {
        fun fromEntity(entity: ShortcutEntity, userId: String? = null): SupabaseShortcutDto {
            return SupabaseShortcutDto(
                shortcut = entity.shortcut,
                expansion = entity.expansion,
                expansionMode = entity.expansionMode,
                userId = userId
            )
        }
    }
}
