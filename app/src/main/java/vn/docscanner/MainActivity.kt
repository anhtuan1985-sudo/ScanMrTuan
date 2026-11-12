package vn.docscanner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private val files = mutableListOf<File>()
    private val adapter = ScanAdapter()

    // Nhận kết quả từ màn hình quét (chụp -> tự căn lề -> nhiều trang -> PDF)
    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val pdfUri = scan?.pdf?.uri
                if (pdfUri != null) {
                    savePdf(pdfUri)
                } else {
                    toast("Không nhận được file PDF từ trình quét")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        emptyView = findViewById(R.id.emptyView)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> showActions(files[position]) }

        findViewById<MaterialButton>(R.id.btnScan).setOnClickListener { startScan() }

        refresh()
    }

    private fun startScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)                       // cho phép chọn ảnh từ thư viện
            .setPageLimit(30)                                    // tối đa 30 trang / file
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { sender ->
                scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener { e ->
                toast("Không mở được trình quét: ${e.message}")
            }
    }

    private fun scansDir(): File = File(filesDir, "scans").apply { mkdirs() }

    private fun savePdf(source: Uri) {
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val target = File(scansDir(), "Scan_$stamp.pdf")
            contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Không đọc được file PDF")
            refresh()
            toast("Đã lưu ${target.name}")
            showActions(target)
        } catch (e: Exception) {
            toast("Lỗi khi lưu PDF: ${e.message}")
        }
    }

    private fun refresh() {
        files.clear()
        val list = scansDir().listFiles()
            ?.filter { it.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        files.addAll(list)
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showActions(file: File) {
        val items = arrayOf(
            "Gửi qua Zalo",
            "Gửi qua Messenger",
            "Gửi qua SMS / tin nhắn",
            "Chia sẻ qua ứng dụng khác...",
            "Mở file PDF",
            "Xóa"
        )
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> share(file, "com.zing.zalo", "Zalo")
                    1 -> share(file, "com.facebook.orca", "Messenger")
                    2 -> share(file, Telephony.Sms.getDefaultSmsPackage(this), "ứng dụng tin nhắn")
                    3 -> share(file, null, null)
                    4 -> openPdf(file)
                    5 -> confirmDelete(file)
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun share(file: File, pkg: String?, appName: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uriFor(file))
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (pkg != null) setPackage(pkg)
        }
        try {
            if (pkg == null) {
                startActivity(Intent.createChooser(intent, "Chia sẻ file PDF"))
            } else {
                startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            toast("Chưa cài đặt ${appName ?: "ứng dụng này"} — hãy chọn \"Chia sẻ qua ứng dụng khác\"")
        }
    }

    private fun openPdf(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(file), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast("Không có ứng dụng đọc PDF trên máy")
        }
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Xóa tài liệu?")
            .setMessage(file.name)
            .setPositiveButton("Xóa") { _, _ ->
                file.delete()
                refresh()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun formatSize(bytes: Long): String =
        if (bytes < 1024 * 1024) String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)

    private inner class ScanAdapter : BaseAdapter() {
        override fun getCount() = files.size
        override fun getItem(position: Int) = files[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_scan, parent, false)
            val file = files[position]
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(file.lastModified()))
            view.findViewById<TextView>(R.id.title).text = file.name
            view.findViewById<TextView>(R.id.subtitle).text = "$date  •  ${formatSize(file.length())}"
            return view
        }
    }
}
