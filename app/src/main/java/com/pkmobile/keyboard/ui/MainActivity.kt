package com.pkmobile.keyboard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.importer.ShortcutImporter
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

    // Callback untuk menerima file dari File Picker (XML, CSV, TXT, JSON dari Perfect Keyboard)
    private var pendingFileImportCallback: ((String) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFileUri(it) }
    }

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

        // Tombol Tambah Shortcut Manual
        binding.btnAddShortcut.setOnClickListener {
            showAddShortcutDialog()
        }

        // Tombol Migrasi / Import dari Perfect Keyboard
        binding.btnImportShortcut.setOnClickListener {
            showImportDialog()
        }

        // Tombol Ekspor Backup Shortcut
        binding.btnExportShortcut.setOnClickListener {
            exportShortcuts()
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

    /**
     * Dialog untuk migrasi/impor shortcut dari Perfect Keyboard.
     * Pengguna bisa memilih berkas ekspor (.xml, .csv, .txt, .backup) atau menempelkan teks langsung.
     */
    private fun showImportDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_shortcut, null)
        val btnChooseFile = dialogView.findViewById<MaterialButton>(R.id.btn_choose_file)
        val tvFileStatus = dialogView.findViewById<TextView>(R.id.tv_file_status)
        val etRawImport = dialogView.findViewById<TextInputEditText>(R.id.et_raw_import)
        val tvPreviewCount = dialogView.findViewById<TextView>(R.id.tv_preview_count)

        var detectedPairs = emptyList<Pair<String, String>>()

        fun updatePreview(text: String) {
            detectedPairs = ShortcutImporter.parse(text)
            tvPreviewCount.text = "Pratinjau: ${detectedPairs.size} shortcut terdeteksi"
            if (detectedPairs.isNotEmpty()) {
                tvPreviewCount.setTextColor(getColor(R.color.accent))
            } else {
                tvPreviewCount.setTextColor(getColor(R.color.text_secondary))
            }
        }

        // Live text change listener pada text field
        etRawImport.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            updatePreview(text)
        }

        // Callback saat user memilih file dari memory
        pendingFileImportCallback = { fileContent ->
            etRawImport.setText(fileContent)
            tvFileStatus.visibility = View.VISIBLE
            tvFileStatus.text = "✓ Berkas berhasil dimuat"
            updatePreview(fileContent)
        }

        btnChooseFile.setOnClickListener {
            // Luncurkan pemilih file berkas
            filePickerLauncher.launch("*/*")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Mulai Import") { dialog, _ ->
                if (detectedPairs.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val count = repository.importShortcuts(detectedPairs)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.toast_imported, count),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.toast_no_shortcut_found, Toast.LENGTH_SHORT).show()
                }
                pendingFileImportCallback = null
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                pendingFileImportCallback = null
                dialog.dismiss()
            }
            .setOnDismissListener {
                pendingFileImportCallback = null
            }
            .show()
    }

    private fun handleSelectedFileUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                withContext(Dispatchers.Main) {
                    if (content.isNotBlank()) {
                        pendingFileImportCallback?.invoke(content)
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal membaca berkas atau berkas kosong.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error membaca file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Membagikan / mengekspor seluruh shortcut ke format CSV standar via Android Share Sheet.
     */
    private fun exportShortcuts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = repository.getAllList()
            if (list.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Tidak ada shortcut untuk diekspor.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val csvContent = ShortcutImporter.exportToCsv(list)
            withContext(Dispatchers.Main) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csvContent)
                    putExtra(Intent.EXTRA_TITLE, "Backup_Shortcut_Keyboard.csv")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Ekspor / Backup Shortcut")
                startActivity(shareIntent)
            }
        }
    }
}
