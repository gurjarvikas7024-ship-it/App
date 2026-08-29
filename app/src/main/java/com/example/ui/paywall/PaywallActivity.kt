package com.example.ui.paywall

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

class PaywallActivity : AppCompatActivity() {

    private val masterSecretKey = "MP2026PRO"
    private val upiId = "7024991656@ybl"
    private val whatsappNumber = "917024991656"

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)

        val ivQrCode = findViewById<ImageView>(R.id.ivQrCode)
        val btnCopyUpi = findViewById<Button>(R.id.btnCopyUpi)
        val btnWhatsappShare = findViewById<Button>(R.id.btnWhatsappShare)
        val btnEnterKey = findViewById<Button>(R.id.btnEnterKey)

        // Generate and display crisp scannable UPI QR Code
        val upiUri = "upi://pay?pa=$upiId&pn=MemoryPlus&am=399&cu=INR&tn=MemoryPlusLifetimePro"
        try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(upiUri, BarcodeFormat.QR_CODE, 600, 600, hints)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ivQrCode.setOnClickListener {
            // Allow opening directly in UPI app when tapped on phone
            try {
                val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                startActivity(Intent.createChooser(upiIntent, "Pay ₹399 with UPI App"))
            } catch (e: Exception) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("UPI ID", upiId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
            }
        }

        btnCopyUpi.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UPI ID", upiId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
        }

        btnWhatsappShare.setOnClickListener {
            val message = "Hello Memory Plus Team,\nMaine ₹399 Lifetime Membership pay kar diya hai. Ye raha payment screenshot.\nKripya mera Activation Key bhejein."
            val url = "https://api.whatsapp.com/send?phone=$whatsappNumber&text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        btnEnterKey.setOnClickListener {
            showActivationDialog()
        }
    }

    private fun showActivationDialog() {
        val input = EditText(this)
        input.hint = "Activation Key daalein"
        input.setSingleLine()

        MaterialAlertDialogBuilder(this)
            .setTitle("🔑 Enter Activation Key")
            .setMessage("WhatsApp par mila Activation Code yahan paste karein:")
            .setView(input)
            .setPositiveButton("Activate") { _, _ ->
                val enteredCode = input.text.toString().trim()
                if (enteredCode.equals(masterSecretKey, ignoreCase = true)) {
                    val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_pro_unlocked", true).apply()
                    PreferenceManager.setProUnlocked(this, true)

                    MaterialAlertDialogBuilder(this)
                        .setTitle("🎉 Lifetime Pro Activated!")
                        .setMessage("Memory Plus Lifetime Membership unlock ho chuki hai! Ab aap unlimited voice & photo reminders set kar sakte hain.")
                        .setCancelable(false)
                        .setPositiveButton("Start Using App") { dialog, _ ->
                            dialog.dismiss()
                            val mainIntent = Intent(this, MainActivity::class.java)
                            mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(mainIntent)
                            finish()
                        }
                        .show()
                } else {
                    Toast.makeText(this, "Galat Key! WhatsApp par payment screenshot bhej kar sahi key prapt karein.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
