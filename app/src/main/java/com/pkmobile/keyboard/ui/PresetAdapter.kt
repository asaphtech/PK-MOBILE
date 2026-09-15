package com.pkmobile.keyboard.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.PresetEntity
import com.pkmobile.keyboard.databinding.ItemPresetDialogBinding

class PresetAdapter(
    private val onSelectActive: (PresetEntity) -> Unit,
    private val onDelete: (PresetEntity) -> Unit
) : ListAdapter<PresetEntity, PresetAdapter.PresetViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val binding = ItemPresetDialogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PresetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PresetViewHolder(private val binding: ItemPresetDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PresetEntity) {
            binding.tvPresetName.text = item.name
            binding.tvPresetCount.text = "${item.shortcutCount} shortcut"

            // Badge Tipe Sumber (Cloud, XML, JSON, Manual)
            val sourceLabel = when (item.sourceType) {
                "CLOUD" -> "Cloud"
                "FILE_XML" -> "XML"
                "FILE_JSON" -> "JSON"
                "MANUAL" -> "Manual"
                else -> item.sourceType
            }
            binding.tvPresetSourceBadge.text = sourceLabel

            // Status Aktif / Nonaktif
            binding.rbPresetActive.isChecked = item.isActive

            if (item.isActive) {
                binding.tvPresetStatusBadge.text = "Aktif (Terpasang)"
                binding.tvPresetStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent))
                binding.cardPresetItem.strokeColor = ContextCompat.getColor(binding.root.context, R.color.accent)
            } else {
                binding.tvPresetStatusBadge.text = "Nonaktif"
                binding.tvPresetStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                binding.cardPresetItem.strokeColor = Color.parseColor("#22FFFFFF")
            }

            // Klik pada kartu atau radio button untuk mengaktifkan preset ini
            binding.cardPresetItem.setOnClickListener {
                onSelectActive(item)
            }

            binding.rbPresetActive.setOnClickListener {
                onSelectActive(item)
            }

            // Tombol Hapus Preset
            binding.btnDeletePreset.setOnClickListener {
                onDelete(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PresetEntity>() {
        override fun areItemsTheSame(oldItem: PresetEntity, newItem: PresetEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PresetEntity, newItem: PresetEntity): Boolean {
            return oldItem == newItem
        }
    }
}
