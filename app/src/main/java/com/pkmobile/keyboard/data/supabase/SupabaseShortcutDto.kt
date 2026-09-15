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

    @SerialName("trigger_code")
    val triggerCode: String? = "",

    @SerialName("expansion_text")
    val expansionText: String? = "",

    @SerialName("category")
    val category: String? = "General",

    @SerialName("expansion_mode")
    val expansionMode: String? = "INSTANT",

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toEntity(): ShortcutEntity {
        val cleanTrigger = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(triggerCode ?: "").lowercase()
        val cleanExp = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTextFormatting(expansionText ?: "")
        return ShortcutEntity(
            triggerCode = cleanTrigger,
            expansionText = cleanExp,
            category = category?.ifBlank { "General" } ?: "General",
            expansionMode = expansionMode?.ifBlank { "INSTANT" } ?: "INSTANT",
            packageName = category?.ifBlank { "General" } ?: "General",
            isActive = true
        )
    }

    companion object {
        fun fromEntity(entity: ShortcutEntity, userId: String? = null): SupabaseShortcutDto {
            return SupabaseShortcutDto(
                triggerCode = entity.triggerCode,
                expansionText = entity.expansionText,
                category = entity.category?.ifBlank { "General" } ?: "General",
                expansionMode = entity.expansionMode.ifBlank { "INSTANT" },
                userId = userId
            )
        }
    }
}
