package com.example.calc

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class ZoomableImageView(context: Context) : androidx.appcompat.widget.AppCompatImageView(context) {
    private val matrix = android.graphics.Matrix()
    private var mode = 0
    private val savedMatrix = android.graphics.Matrix()
    private val start = android.graphics.PointF()
    private var oldDist = 1f

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        super.onTouchEvent(event)
        performClick()
        when (event.action and android.view.MotionEvent.ACTION_MASK) {
            android.view.MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = 1
            }
            android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(start, event)
                    mode = 2
                }
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_POINTER_UP -> {
                mode = 0
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (mode == 1) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(event.x - start.x, event.y - start.y)
                } else if (mode == 2) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, start.x, start.y)
                    }
                }
            }
        }
        imageMatrix = matrix
        return true
    }

    private fun spacing(event: android.view.MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: android.graphics.PointF, event: android.view.MotionEvent) {
        if (event.pointerCount < 2) return
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2f, y / 2f)
    }
}

class VaultActivity : AppCompatActivity() {

    private val photoPaths = mutableListOf<String>()
    private val documentPaths = mutableListOf<String>()
    private val lockedApps = mutableListOf<String>()
    private val secretNotes = mutableListOf<String>()

    private lateinit var tvPhotos: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvApps: TextView
    private lateinit var tvDocuments: TextView

    private var activeGalleryAdapter: BaseAdapter? = null
    private var activeGalleryTitle: TextView? = null

    private val pickMultiplePhotosLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            var addedCount = 0
            for (uri in uris) {
                val savedPath = saveUriToInternalStorage(uri, "vault_photos", "photo_${System.currentTimeMillis()}_$addedCount.jpg")
                if (savedPath != null) {
                    photoPaths.add(savedPath)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                updateCounts()
                activeGalleryAdapter?.notifyDataSetChanged()
                activeGalleryTitle?.text = "Photos (${photoPaths.size})"
                Toast.makeText(this, "Added $addedCount photos to Photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val savedPath = saveUriToInternalStorage(it, "vault_docs", "doc_${System.currentTimeMillis()}")
            if (savedPath != null) {
                documentPaths.add(savedPath)
                updateCounts()
                Toast.makeText(this, "Document added to Documents", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)

        tvPhotos = findViewById(R.id.tvPhotos)
        tvNotes = findViewById(R.id.tvNotes)
        tvApps = findViewById(R.id.tvApps)
        tvDocuments = findViewById(R.id.tvDocuments)

        val btnChangePin = findViewById<Button>(R.id.btnChangePin)
        val btnAddItem = findViewById<Button>(R.id.btnAddItem)

        btnChangePin.setOnClickListener { showChangePinDialog() }
        btnAddItem.setOnClickListener { showAddCategoryChoiceDialog() }

        tvPhotos.setOnClickListener { showPhotoGalleryDialog() }
        tvNotes.setOnClickListener { showManageNotesDialog() }
        tvApps.setOnClickListener { showAppsGridDialog() }
        tvDocuments.setOnClickListener { showDocumentsGridDialog() }

        updateCounts()
    }

    private fun saveUriToInternalStorage(uri: Uri, folderName: String, fileName: String): String? {
        return try {
            val dir = File(filesDir, folderName)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateCounts() {
        tvPhotos.text = "Photos (${photoPaths.size})"
        tvNotes.text = "Secret Note (${secretNotes.size})"
        tvApps.text = "Apps (${lockedApps.size})"
        tvDocuments.text = "Documents (${documentPaths.size})"
    }

    private fun showAddCategoryChoiceDialog() {
        val options = arrayOf("Add Photos (Gallery - Multi)", "Add Document", "Select App to Lock", "Lock All Apps", "Add Secret Note")
        AlertDialog.Builder(this)
            .setTitle("Add Item")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMultiplePhotosLauncher.launch("image/*")
                    1 -> pickDocumentLauncher.launch("*/*")
                    2 -> showInstalledAppsDialog()
                    3 -> lockAllInstalledApps()
                    4 -> showAddNoteDialog()
                }
            }
            .show()
    }

    // 1. Photos Gallery Grid Dialog matching reference image layout & spacing
    private fun showPhotoGalleryDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_gallery)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val gridView = dialog.findViewById<GridView>(R.id.gridView)
        val btnAdd = dialog.findViewById<Button>(R.id.btnAdd)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        tvTitle.text = "Photos (${photoPaths.size})"
        btnAdd.text = "Add Photos"

        activeGalleryTitle = tvTitle

        val adapter = object : BaseAdapter() {
            override fun getCount() = photoPaths.size
            override fun getItem(position: Int) = photoPaths[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val imageView = (convertView as? ImageView) ?: ImageView(this@VaultActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 320)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                imageView.setImageURI(Uri.parse(photoPaths[position]))
                return imageView
            }
        }
        activeGalleryAdapter = adapter
        gridView.adapter = adapter

        gridView.setOnItemClickListener { _, _, position, _ ->
            val path = photoPaths[position]
            val previewImage = ZoomableImageView(this).apply {
                setImageURI(Uri.parse(path))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1100)
            }
            AlertDialog.Builder(this)
                .setTitle("Private Photo #${position + 1} (Pinch to zoom)")
                .setView(previewImage)
                .setPositiveButton("Delete Photo") { _, _ ->
                    try { File(path).delete() } catch (_: Exception) {}
                    photoPaths.removeAt(position)
                    updateCounts()
                    tvTitle.text = "Photos (${photoPaths.size})"
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
        }

        btnAdd.setOnClickListener {
            pickMultiplePhotosLauncher.launch("image/*")
        }
        btnClose.setOnClickListener {
            activeGalleryAdapter = null
            activeGalleryTitle = null
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            activeGalleryAdapter = null
            activeGalleryTitle = null
        }

        dialog.show()
    }

    // 2. Apps Grid Dialog
    private fun showAppsGridDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_gallery)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val gridView = dialog.findViewById<GridView>(R.id.gridView)
        val btnAdd = dialog.findViewById<Button>(R.id.btnAdd)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        tvTitle.text = "Apps (${lockedApps.size})"
        btnAdd.text = "Lock App"

        val pm = packageManager
        val adapter = object : BaseAdapter() {
            override fun getCount() = lockedApps.size
            override fun getItem(position: Int) = lockedApps[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val pkgName = lockedApps[position]
                val layout = (convertView as? LinearLayout) ?: LinearLayout(this@VaultActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(12, 12, 12, 12)
                    setBackgroundColor(0xFF1E1E1E.toInt())
                }

                layout.removeAllViews()
                var appIcon: Drawable? = null
                var appLabel = pkgName

                try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    appIcon = pm.getApplicationIcon(appInfo)
                    appLabel = pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {}

                val iconView = ImageView(this@VaultActivity).apply {
                    if (appIcon != null) setImageDrawable(appIcon) else setImageResource(android.R.drawable.sym_def_app_icon)
                    layoutParams = LinearLayout.LayoutParams(130, 130)
                }
                val textView = TextView(this@VaultActivity).apply {
                    text = appLabel
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    maxLines = 1
                }

                layout.addView(iconView)
                layout.addView(textView)
                return layout
            }
        }
        gridView.adapter = adapter

        gridView.setOnItemClickListener { _, _, position, _ ->
            val pkgName = lockedApps[position]
            var appLabel = pkgName
            try {
                appLabel = pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0)).toString()
            } catch (_: Exception) {}

            AlertDialog.Builder(this)
                .setTitle(appLabel)
                .setMessage("Open or unlock this app?")
                .setPositiveButton("Open App") { _, _ ->
                    try {
                        val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        } else {
                            Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        Toast.makeText(this, "Error opening app", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Unlock App") { _, _ ->
                    lockedApps.removeAt(position)
                    updateCounts()
                    tvTitle.text = "Apps (${lockedApps.size})"
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "App unlocked", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }

        btnAdd.setOnClickListener {
            showInstalledAppsDialog()
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // 3. Documents Grid Dialog
    private fun showDocumentsGridDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_gallery)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val gridView = dialog.findViewById<GridView>(R.id.gridView)
        val btnAdd = dialog.findViewById<Button>(R.id.btnAdd)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        tvTitle.text = "Documents (${documentPaths.size})"
        btnAdd.text = "Add Document"

        val adapter = object : BaseAdapter() {
            override fun getCount() = documentPaths.size
            override fun getItem(position: Int) = documentPaths[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val path = documentPaths[position]
                val fileName = File(path).name

                val layout = (convertView as? LinearLayout) ?: LinearLayout(this@VaultActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(12, 12, 12, 12)
                    setBackgroundColor(0xFF1E1E1E.toInt())
                }
                layout.removeAllViews()

                val iconView = ImageView(this@VaultActivity).apply {
                    setImageResource(android.R.drawable.ic_menu_send)
                    layoutParams = LinearLayout.LayoutParams(110, 110)
                }
                val textView = TextView(this@VaultActivity).apply {
                    text = fileName
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    maxLines = 2
                }

                layout.addView(iconView)
                layout.addView(textView)
                return layout
            }
        }
        gridView.adapter = adapter

        gridView.setOnItemClickListener { _, _, position, _ ->
            val path = documentPaths[position]
            val fileName = File(path).name
            AlertDialog.Builder(this)
                .setTitle(fileName)
                .setMessage("Path: $path")
                .setPositiveButton("Open Document") { _, _ ->
                    try {
                        val file = File(path)
                        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(this, "No application found to open this document", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("Remove") { _, _ ->
                    try { File(path).delete() } catch (_: Exception) {}
                    documentPaths.removeAt(position)
                    updateCounts()
                    tvTitle.text = "Documents (${documentPaths.size})"
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "Document removed", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
        }

        btnAdd.setOnClickListener {
            pickDocumentLauncher.launch("*/*")
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showManageNotesDialog() {
        if (secretNotes.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Secret Note")
                .setMessage("No secret notes saved yet.")
                .setPositiveButton("Add Note") { _, _ -> showAddNoteDialog() }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        val displayNotes = secretNotes.mapIndexed { index, note ->
            val preview = if (note.length > 30) note.substring(0, 30) + "..." else note
            "Note #${index + 1}: $preview"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Secret Note (${secretNotes.size})")
            .setItems(displayNotes) { _, which ->
                AlertDialog.Builder(this)
                    .setTitle("Secret Note #${which + 1}")
                    .setMessage(secretNotes[which])
                    .setPositiveButton("Delete") { _, _ ->
                        secretNotes.removeAt(which)
                        updateCounts()
                        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
                        showManageNotesDialog()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
            .setPositiveButton("Add Note") { _, _ -> showAddNoteDialog() }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun showAddNoteDialog() {
        val input = EditText(this).apply {
            hint = "Type your secret note here..."
        }
        AlertDialog.Builder(this)
            .setTitle("Add Secret Note")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isNotEmpty()) {
                    secretNotes.add(note)
                    updateCounts()
                    Toast.makeText(this, "Secret note saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showInstalledAppsDialog() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { it.loadLabel(pm).toString() }

        val appNames = packages.map { it.loadLabel(pm).toString() }.toTypedArray()
        val packageNames = packages.map { it.packageName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select App to Lock")
            .setItems(appNames) { _, which ->
                val pkg = packageNames[which]
                if (!lockedApps.contains(pkg)) {
                    lockedApps.add(pkg)
                    updateCounts()
                    Toast.makeText(this, "${appNames[which]} added to Locked Apps", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${appNames[which]} is already locked", Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton("Lock All Apps") { _, _ ->
                lockAllInstalledApps()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun lockAllInstalledApps() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

        var addedCount = 0
        for (app in packages) {
            if (!lockedApps.contains(app.packageName)) {
                lockedApps.add(app.packageName)
                addedCount++
            }
        }
        updateCounts()
        Toast.makeText(this, "Added $addedCount apps to Locked Apps", Toast.LENGTH_SHORT).show()
    }

    private fun showChangePinDialog() {
        val input = EditText(this).apply {
            hint = "Enter new 4+ digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Change Vault PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPin = input.text.toString().trim()
                if (newPin.isNotEmpty()) {
                    val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    prefs.edit().putString(KEY_PIN, newPin).apply()
                    Toast.makeText(this, "Vault PIN updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val PREF_NAME = "CalcVaultPrefs"
        const val KEY_PIN = "vault_pin"

        fun getPin(context: Context): String {
            val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_PIN, "1234") ?: "1234"
        }
    }
}
