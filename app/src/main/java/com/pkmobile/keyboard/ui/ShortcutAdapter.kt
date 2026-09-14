package com.pkmobile.keyboard.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.ShortcutEntity
import com.pkmobile.keyboard.databinding.ItemShortcutBinding

class ShortcutAdapter(
    private val onItemClick: (ShortcutEntity) -> Unit,
    private val onDeleteClick: (ShortcutEntity) -> Unit,
    private val onToggleActive: (ShortcutEntity, Boolean) -> Unit
) : ListAdapter<ShortcutEntity, ShortcutAdapter.ShortcutViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShortcutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShortcutViewHolder(private val binding: ItemShortcutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShortcutEntity) {
            val context = binding.root.context

            binding.tvShortcutBadge.text = item.shortcut
            binding.tvExpansion.text = item.expansion
            binding.tvPackageBadge.text = "📁 ${item.packageName}"

            // Mode Badge
            if (item.expansionMode.equals("SPACE", ignoreCase = true)) {
                binding.tvModeBadge.text = "Spasi"
                binding.tvModeBadge.setTextColor(ContextCompat.getColor(context, R.color.candidate_text_highlight))
            } else {
                binding.tvModeBadge.text = "Instan"
                binding.tvModeBadge.setTextColor(ContextCompat.getColor(context, R.color.accent))
            }

            // Status Badge & Switch
            binding.switchShortcutActive.setOnCheckedChangeListener(null)
            binding.switchShortcutActive.isChecked = item.isActive

            if (item.isActive) {
                binding.tvStatusBadge.text = "Aktif"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent))
                binding.tvShortcutBadge.alpha = 1.0f
                binding.tvExpansion.alpha = 1.0f
            } else {
                binding.tvStatusBadge.text = "Nonaktif"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                binding.tvShortcutBadge.alpha = 0.5f
                binding.tvExpansion.alpha = 0.5f
            }

            binding.switchShortcutActive.setOnCheckedChangeListener { _, isChecked ->
                onToggleActive(item, isChecked)
            }

            binding.btnEdit.setOnClickListener {
                onItemClick(item)
            }
            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ShortcutEntity>() {
        override fun areItemsTheSame(oldItem: ShortcutEntity, newItem: ShortcutEntity): Boolean {
            return oldItem.triggerCode == newItem.triggerCode
        }

        override fun areContentsTheSame(oldItem: ShortcutEntity, newItem: ShortcutEntity): Boolean {
            return oldItem == newItem
        }
    }
}
