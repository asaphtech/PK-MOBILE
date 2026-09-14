package com.pkmobile.keyboard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.db.ShortcutEntity
import com.pkmobile.keyboard.data.importer.ShortcutImporter
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.data.supabase.AuthService
import com.pkmobile.keyboard.data.supabase.SyncRepository
import com.pkmobile.keyboard.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Halaman Utama Aplikasi Keyboard PK MOBILE (JFN Type Master).
 * Mendukung 3 Metode Resmi Pengelolaan Shortcut:
 * 1. Synchronize Cloud (Supabase / JFN Type Master Server)
 * 2. Import File XML (Paket Berkas Shortcut XML)
 * 3. Input Manual Satuan (Form Tambah/Edit Shortcut)
 *
 * Serta fitur:
 * - Search Bar dinamis real-time (mencari trigger_code & expansion_text)
 * - Manajemen Pasang/Lepas Paket Berkas Shortcut (Switch On/Off)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ShortcutRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var authService: AuthService

    private lateinit var shortcutAdapter: ShortcutAdapter
    private lateinit var packageAdapter: PackageAdapter

    // Cache daftar lengkap seluruh shortcut untuk pencarian real-time
    private var fullShortcutList: List<ShortcutEntity> = emptyList()
    private var currentSearchQuery: String = ""

    // Callback untuk menerima file dari File Picker (konten & nama berkas)
    private var pendingFileImportCallback: ((content: String, fileName: String) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFileUri(it) }
    }

    private val directXmlPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                importXmlDirectlyFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(this)
        repository = ShortcutRepository(database.shortcutDao())
        authService = AuthService(this)
        syncRepository = SyncRepository(this, repository, authService)

        setupViews()
        setupPackageRecyclerView()
        setupShortcutRecyclerView()
        setupSearchBar()
        observeShortcuts()

        // Pembersihan otomatis database dari tag XML lama (seperti <tscut>)
        lifecycleScope.launch(Dispatchers.IO) {
            val cleaned = repository.sanitizeExistingDatabase()
            if (cleaned > 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Otomatis membersihkan $cleaned data shortcut dari tag XML.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupViews() {
        // 3 METODE RESMI SUMBER SHORTCUT:
        // Metode 1: Synchronize Cloud (Supabase Server)
        binding.btnSyncCloud.setOnClickListener {
            triggerCloudSync()
        }

        // Metode 2: Import File XML
        binding.btnImportXml.setOnClickListener {
            openXmlFilePicker()
        }

        // Tombol Impor File XML pada Header Manajemen Paket
        binding.btnImportXmlPackage.setOnClickListener {
            openXmlFilePicker()
        }

        // Metode 3: Input Manual Satuan
        binding.btnAddShortcut.setOnClickListener {
            showAddShortcutDialog()
        }

        // Opsi Tambahan: Ekspor Backup (JSON / CSV)
        binding.btnExportShortcut.setOnClickListener {
            showExportChoiceDialog()
        }

        // Tombol Pengaturan Aplikasi (Settings)
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Inisialisasi RecyclerView untuk Paket/Berkas Shortcut (Daftar Horizontal).
     */
    private fun setupPackageRecyclerView() {
        packageAdapter = PackageAdapter(
            onToggleActive = { pkg, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.setPackageActive(pkg.name, isChecked)
                    withContext(Dispatchers.Main) {
                        val status = if (isChecked) "dipasang (Aktif)" else "dilepas (Nonaktif)"
                        Toast.makeText(this@MainActivity, "Paket '${pkg.name}' $status.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvPackages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPackages.adapter = packageAdapter
    }

    /**
     * Inisialisasi RecyclerView untuk Daftar Shortcut.
     */
    private fun setupShortcutRecyclerView() {
        shortcutAdapter = ShortcutAdapter(
            onItemClick = { shortcutEntity ->
                showEditShortcutDialog(shortcutEntity)
            },
            onDeleteClick = { shortcutEntity ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.deleteShortcut(shortcutEntity)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onToggleActive = { shortcutEntity, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.setShortcutActive(shortcutEntity.triggerCode, isChecked)
                    withContext(Dispatchers.Main) {
                        val status = if (isChecked) "diaktifkan" else "dinonaktifkan"
                        Toast.makeText(this@MainActivity, "Shortcut '${shortcutEntity.shortcut}' $status.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvShortcuts.layoutManager = LinearLayoutManager(this)
        binding.rvShortcuts.adapter = shortcutAdapter
    }

    /**
     * Konfigurasi Search Bar untuk pencarian dinamis (real-time filtering).
     */
    private fun setupSearchBar() {
        binding.etSearchShortcut.doAfterTextChanged { editable ->
            currentSearchQuery = editable?.toString()?.trim() ?: ""
            binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
            applyFilter()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchShortcut.setText("")
        }
    }

    /**
     * Mengamati perubahan data Room Database secara reaktif.
     */
    private fun observeShortcuts() {
        lifecycleScope.launch {
            repository.allShortcutsFlow.collectLatest { list ->
                fullShortcutList = list

                // Update daftar paket / berkas
                updatePackageList(list)

                // Terapkan filter pencarian aktif
                applyFilter()
            }
        }
    }

    /**
     * Mengelompokkan shortcut berdasarkan nama paket/berkas untuk sakelar Pasang/Lepas.
     */
    private fun updatePackageList(list: List<ShortcutEntity>) {
        if (list.isEmpty()) {
            packageAdapter.submitList(emptyList())
            binding.layoutPackageSection.visibility = View.VISIBLE
            return
        }

        val packages = list.groupBy { it.packageName.ifBlank { "Paket Utama" } }.map { (pkgName, items) ->
            val hasActive = items.any { it.isActive }
            PackageModel(
                name = pkgName,
                totalCount = items.size,
                isActive = hasActive
            )
        }.sortedBy { it.name }

        packageAdapter.submitList(packages)
        binding.layoutPackageSection.visibility = View.VISIBLE
    }

    /**
     * Memfilter daftar shortcut berdasarkan query pencarian (trigger_code dan expansion_text).
     * Shortcut yang paketnya nonaktif (isActive == false) disembunyikan sepenuhnya dari daftar.
     */
    private fun applyFilter() {
        val query = currentSearchQuery.lowercase()
        // Sembunyikan seluruh item shortcut milik paket yang nonaktif (isActive == false)
        val activeShortcuts = fullShortcutList.filter { it.isActive }

        val filtered = if (query.isEmpty()) {
            activeShortcuts
        } else {
            activeShortcuts.filter { item ->
                item.shortcut.lowercase().contains(query) ||
                item.expansion.lowercase().contains(query) ||
                item.packageName.lowercase().contains(query)
            }
        }

        shortcutAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvEmptyShortcuts.visibility = View.VISIBLE
            binding.rvShortcuts.visibility = View.GONE
            if (currentSearchQuery.isNotEmpty()) {
                binding.tvEmptyShortcuts.text = "Tidak ditemukan shortcut yang cocok dengan '$currentSearchQuery'"
            } else if (fullShortcutList.isNotEmpty() && activeShortcuts.isEmpty()) {
                binding.tvEmptyShortcuts.text = "Seluruh paket shortcut sedang nonaktif (dilepas).\nAktifkan salah satu paket di atas untuk menampilkan shortcut."
            } else {
                binding.tvEmptyShortcuts.setText(R.string.shortcut_empty)
            }
        } else {
            binding.tvEmptyShortcuts.visibility = View.GONE
            binding.rvShortcuts.visibility = View.VISIBLE
        }

        // Header status count
        if (currentSearchQuery.isNotEmpty()) {
            binding.tvShortcutHeader.text = "Hasil Pencarian (${filtered.size}/${activeShortcuts.size})"
        } else {
            binding.tvShortcutHeader.text = "${getString(R.string.section_shortcuts)} (${activeShortcuts.size} Aktif / ${fullShortcutList.size} Total)"
        }
    }

    // =========================================================================
    // METODE 1: SYNCHRONIZE CLOUD (JFN TYPE MASTER / SUPABASE)
    // =========================================================================
    private fun triggerCloudSync() {
        binding.btnSyncCloud.isEnabled = false
        Toast.makeText(this, "Menyinkronkan data dengan Cloud JFN Type Master...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val result = syncRepository.sync()
            withContext(Dispatchers.Main) {
                binding.btnSyncCloud.isEnabled = true
                Toast.makeText(
                    this@MainActivity,
                    result.message,
                    if (result.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================================
    // METODE 2: IMPORT FILE XML
    // =========================================================================
    private fun showImportXmlDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_shortcut, null)
        val btnChooseFile = dialogView.findViewById<MaterialButton>(R.id.btn_choose_file)
        val tvFileStatus = dialogView.findViewById<TextView>(R.id.tv_file_status)
        val etRawImport = dialogView.findViewById<TextInputEditText>(R.id.et_raw_import)
        val tvPreviewCount = dialogView.findViewById<TextView>(R.id.tv_preview_count)
        val cbClearBeforeImport = dialogView.findViewById<MaterialCheckBox>(R.id.cb_clear_before_import)

        var detectedPairs = emptyList<Pair<String, String>>()
        var detectedFileName = "Paket XML"
        var parseJob: kotlinx.coroutines.Job? = null

        fun updatePreview(text: String, fileName: String = detectedFileName) {
            parseJob?.cancel()
            tvPreviewCount.text = "Menganalisis berkas XML..."
            tvPreviewCount.setTextColor(getColor(R.color.candidate_text_highlight))

            parseJob = lifecycleScope.launch(Dispatchers.Default) {
                val pairs = ShortcutImporter.parse(text)
                withContext(Dispatchers.Main) {
                    detectedPairs = pairs
                    detectedFileName = fileName
                    tvPreviewCount.text = "Pratinjau: ${pairs.size} shortcut terdeteksi dari '$fileName'"
                    if (pairs.isNotEmpty()) {
                        tvPreviewCount.setTextColor(getColor(R.color.accent))
                    } else {
                        tvPreviewCount.setTextColor(getColor(R.color.text_secondary))
                    }
                }
            }
        }

        // Live text change listener jika pengguna menempel teks XML langsung
        etRawImport.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            if (text.isNotBlank()) {
                updatePreview(text, "Tempelan Teks XML")
            }
        }

        // Callback saat user memilih file XML dari memori
        pendingFileImportCallback = { fileContent, fileName ->
            tvFileStatus.visibility = View.VISIBLE
            val sizeKb = (fileContent.length / 1024).coerceAtLeast(1)
            tvFileStatus.text = "✓ Berkas XML '$fileName' ($sizeKb KB) siap dimuat"

            if (fileContent.length < 5000) {
                etRawImport.setText(fileContent)
            } else {
                etRawImport.setText("")
                etRawImport.hint = "Berkas '$fileName' ($sizeKb KB) siap diimpor."
            }
            updatePreview(fileContent, fileName)
        }

        btnChooseFile.setOnClickListener {
            // Luncurkan pemilih berkas XML
            filePickerLauncher.launch("*/*")
        }

        val importDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Impor Paket Berkas XML")
            .setView(dialogView)
            .setPositiveButton("Pasang ke Keyboard") { dialog, _ ->
                if (detectedPairs.isNotEmpty()) {
                    val shouldClear = cbClearBeforeImport?.isChecked ?: false
                    lifecycleScope.launch(Dispatchers.IO) {
                        val count = repository.importXmlShortcuts(
                            pairs = detectedPairs,
                            fileName = detectedFileName,
                            clearExisting = shouldClear
                        )
                        withContext(Dispatchers.Main) {
                            val msg = if (shouldClear) {
                                "Database diperbarui! Berhasil memasang $count shortcut dari '$detectedFileName'."
                            } else {
                                "Berhasil memasang $count shortcut ke paket '$detectedFileName'."
                            }
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Tidak ditemukan data shortcut yang valid dalam XML.", Toast.LENGTH_SHORT).show()
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
            .create()

        importDialog.show()
    }

    /**
     * Membuka Intent File Picker khusus untuk berkas .xml (ACTION_GET_CONTENT).
     */
    private fun openXmlFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/xml", "application/xml", "text/plain"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            directXmlPickerLauncher.launch(Intent.createChooser(intent, "Pilih File XML Shortcut"))
        } catch (e: Exception) {
            filePickerLauncher.launch("*/*")
        }
    }

    /**
     * Membaca langsung berkas XML yang dipilih pengguna, mengurai seluruh pasangan trigger/expansion,
     * dan menyimpannya ke Room Database sebagai paket shortcut baru.
     */
    private fun importXmlDirectlyFromUri(uri: Uri) {
        val fileName = getFileNameFromUri(uri)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                if (content.isBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Berkas kosong atau gagal dibaca.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pairs = ShortcutImporter.parse(content)
                if (pairs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Tidak ditemukan data shortcut yang valid dalam '$fileName'.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // Ambil nama paket dari nama berkas (tanpa ekstensi .xml)
                val packageName = fileName.removeSuffix(".xml").removeSuffix(".XML").ifBlank { "Paket XML" }
                val count = repository.importXmlShortcuts(
                    pairs = pairs,
                    fileName = packageName,
                    clearExisting = false
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Berhasil mengimpor $count shortcut ke dalam paket '$packageName'.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Gagal mengimpor berkas XML: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleSelectedFileUri(uri: Uri) {
        val fileName = getFileNameFromUri(uri)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                withContext(Dispatchers.Main) {
                    if (content.isNotBlank()) {
                        pendingFileImportCallback?.invoke(content, fileName)
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal membaca berkas atau berkas kosong.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error membaca berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            val path = uri.path
            val cut = path?.lastIndexOf('/')
            result = if (path != null && cut != null && cut != -1) {
                path.substring(cut + 1)
            } else {
                path
            }
        }
        return result ?: "shortcuts.xml"
    }

    // =========================================================================
    // METODE 3: INPUT MANUAL SATUAN
    // =========================================================================
    private fun showAddShortcutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shortcut, null)
        val etShortcut = dialogView.findViewById<TextInputEditText>(R.id.et_input_shortcut)
        val etExpansion = dialogView.findViewById<TextInputEditText>(R.id.et_input_expansion)
        val rbAddModeSpace = dialogView.findViewById<RadioButton>(R.id.rb_add_mode_space)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_add)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { dialog, _ ->
                val shortcutText = etShortcut.text?.toString()?.trim() ?: ""
                val expansionText = etExpansion.text?.toString()?.trim() ?: ""
                val selectedMode = if (rbAddModeSpace.isChecked) "SPACE" else "INSTANT"

                if (shortcutText.isNotEmpty() && expansionText.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.insertShortcut(
                            shortcut = shortcutText,
                            expansion = expansionText,
                            expansionMode = selectedMode,
                            packageName = "Manual",
                            isActive = true
                        )
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
     * Dialog Edit Trigger dan Teks Ekspansi Satuan.
     */
    private fun showEditShortcutDialog(shortcut: ShortcutEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_shortcut, null)
        val etEditTrigger = dialogView.findViewById<TextInputEditText>(R.id.etEditTrigger)
        val etEditExpansion = dialogView.findViewById<TextInputEditText>(R.id.etEditExpansion)
        val rbEditModeInstant = dialogView.findViewById<RadioButton>(R.id.rb_edit_mode_instant)
        val rbEditModeSpace = dialogView.findViewById<RadioButton>(R.id.rb_edit_mode_space)
        val btnEditCancel = dialogView.findViewById<MaterialButton>(R.id.btnEditCancel)
        val btnEditSave = dialogView.findViewById<MaterialButton>(R.id.btnEditSave)

        etEditTrigger.setText(shortcut.shortcut)
        etEditExpansion.setText(shortcut.expansion)
        if (shortcut.expansionMode.equals("SPACE", ignoreCase = true)) {
            rbEditModeSpace.isChecked = true
        } else {
            rbEditModeInstant.isChecked = true
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnEditCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnEditSave.setOnClickListener {
            val rawTrigger = etEditTrigger.text?.toString() ?: ""
            val rawExpansion = etEditExpansion.text?.toString() ?: ""
            val selectedMode = if (rbEditModeSpace.isChecked) "SPACE" else "INSTANT"

            val cleanKey = ShortcutImporter.cleanTrigger(rawTrigger)
            val cleanExp = rawExpansion.trim()

            if (cleanKey.isBlank() || cleanExp.isBlank()) {
                Toast.makeText(this, "Trigger dan teks ekspansi tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEditSave.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val updatedEntity = shortcut.copy(
                        triggerCode = cleanKey.lowercase(),
                        expansionText = cleanExp,
                        expansionMode = selectedMode
                    )
                    repository.update(shortcut.triggerCode, updatedEntity)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Shortcut berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        if (dialog.isShowing && !isFinishing && !isDestroyed) {
                            dialog.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnEditSave.isEnabled = true
                        Toast.makeText(
                            this@MainActivity,
                            "Gagal memperbarui shortcut: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    // =========================================================================
    // EKSPOR BACKUP (JSON / CSV)
    // =========================================================================
    private fun showExportChoiceDialog() {
        val options = arrayOf(
            "📁 Simpan Cadangan sebagai JSON (shortcuts.json) [Direkomendasikan]",
            "📄 Simpan Cadangan sebagai CSV (shortcuts.csv)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Format Ekspor Cadangan")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportShortcuts(asJson = true)
                    1 -> exportShortcuts(asJson = false)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun exportShortcuts(asJson: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = repository.getAllList()
            if (list.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Tidak ada shortcut untuk diekspor.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val fileName = if (asJson) "shortcuts.json" else "shortcuts.csv"
            val mimeType = if (asJson) "application/json" else "text/csv"
            val fileContent = if (asJson) {
                ShortcutImporter.exportToJson(list)
            } else {
                ShortcutImporter.exportToCsv(list)
            }

            try {
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir.exists() || downloadDir.mkdirs()) {
                    val exportFile = java.io.File(downloadDir, fileName)
                    exportFile.writeText(fileContent)
                }
            } catch (_: Exception) {
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Berhasil mengekspor ${list.size} shortcut ($fileName)",
                    Toast.LENGTH_SHORT
                ).show()

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, fileContent)
                    putExtra(Intent.EXTRA_TITLE, fileName)
                    type = mimeType
                }
                val shareIntent = Intent.createChooser(sendIntent, "Ekspor / Backup Shortcut ($fileName)")
                startActivity(shareIntent)
            }
        }
    }
}
