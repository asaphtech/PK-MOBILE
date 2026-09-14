package com.pkmobile.keyboard.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.databinding.ItemPackageBinding

data class PackageModel(
    val name: String,
    val totalCount: Int,
    val isActive: Boolean
)

class PackageAdapter(
    private val onToggleActive: (PackageModel, Boolean) -> Unit
) : ListAdapter<PackageModel, PackageAdapter.PackageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemPackageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PackageViewHolder(private val binding: ItemPackageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackageModel) {
            binding.tvPackageName.text = item.name
            binding.tvPackageCount.text = "${item.totalCount} shortcut"

            // Unbind listener before setting checked state to avoid unwanted trigger
            binding.switchPackage.setOnCheckedChangeListener(null)
            binding.switchPackage.isChecked = item.isActive

            if (item.isActive) {
                binding.tvPackageStatusBadge.text = "Aktif (Terpasang)"
                binding.tvPackageStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent))
            } else {
                binding.tvPackageStatusBadge.text = "Nonaktif (Dilepas)"
                binding.tvPackageStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
            }

            binding.switchPackage.setOnCheckedChangeListener { _, isChecked ->
                onToggleActive(item, isChecked)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PackageModel>() {
        override fun areItemsTheSame(oldItem: PackageModel, newItem: PackageModel): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: PackageModel, newItem: PackageModel): Boolean {
            return oldItem == newItem
        }
    }
}
