package com.pkmobile.keyboard.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pkmobile.keyboard.data.db.ShortcutEntity
import com.pkmobile.keyboard.databinding.ItemShortcutBinding

class ShortcutAdapter(
    private val onItemClick: (ShortcutEntity) -> Unit,
    private val onDeleteClick: (ShortcutEntity) -> Unit
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
            binding.tvShortcutBadge.text = item.shortcut
            binding.tvExpansion.text = item.expansion
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
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShortcutEntity, newItem: ShortcutEntity): Boolean {
            return oldItem == newItem
        }
    }
}
