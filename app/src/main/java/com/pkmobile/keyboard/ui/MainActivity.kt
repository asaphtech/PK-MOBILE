package com.pkmobile.keyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ShortcutRepository
    private lateinit var adapter: ShortcutAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(this)
        repository = ShortcutRepository(database.shortcutDao())

        setupViews()
        setupRecyclerView()
        observeShortcuts()
    }

    private fun setupViews() {
        // Tombol 1: Buka pengaturan sistem untuk menyalakan Keyboard
        binding.btnEnableIme.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // Tombol 2: Buka dialog pemilih input method aktif
        binding.btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }

        // Tombol Tambah Shortcut
        binding.btnAddShortcut.setOnClickListener {
            showAddShortcutDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ShortcutAdapter { shortcutEntity ->
            // Hapus shortcut
            lifecycleScope.launch(Dispatchers.IO) {
                repository.deleteShortcut(shortcutEntity)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvShortcuts.layoutManager = LinearLayoutManager(this)
        binding.rvShortcuts.adapter = adapter
    }

    private fun observeShortcuts() {
        lifecycleScope.launch {
            repository.allShortcutsFlow.collectLatest { list ->
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.tvEmptyShortcuts.visibility = View.VISIBLE
                    binding.rvShortcuts.visibility = View.GONE
                } else {
                    binding.tvEmptyShortcuts.visibility = View.GONE
                    binding.rvShortcuts.visibility = View.VISIBLE
                }
                binding.tvShortcutHeader.text = "${getString(R.string.section_shortcuts)} (${list.size})"
            }
        }
    }

    private fun showAddShortcutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shortcut, null)
        val etShortcut = dialogView.findViewById<TextInputEditText>(R.id.et_input_shortcut)
        val etExpansion = dialogView.findViewById<TextInputEditText>(R.id.et_input_expansion)

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { dialog, _ ->
                val shortcutText = etShortcut.text?.toString()?.trim() ?: ""
                val expansionText = etExpansion.text?.toString()?.trim() ?: ""

                if (shortcutText.isNotEmpty() && expansionText.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.insertShortcut(shortcutText, expansionText)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Kata kunci dan pengganti tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
