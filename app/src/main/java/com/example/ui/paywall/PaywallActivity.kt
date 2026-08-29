package com.example.ui.paywall

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.PreferenceManager
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.MyApplicationTheme
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class PaywallActivity : ComponentActivity() {

    companion object {
        const val UPI_PA = "7024991656@apl"
        const val UPI_PN = "vikas g"
        const val UPI_AM = "399"
        const val UPI_CU = "INR"
        const val UPI_TN = "Memory Plus Pro Lifetime"
        const val MASTER_SECRET_KEY = "MP2026PRO"
        const val SHARED_PREFS_FILE = "MemoryPlusPrefs"
        const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"

        fun getUpiUriString(): String {
            return "upi://pay?pa=$UPI_PA" +
                    "&pn=" + Uri.encode(UPI_PN) +
                    "&am=$UPI_AM" +
                    "&cu=$UPI_CU" +
                    "&tn=" + Uri.encode(UPI_TN)
        }

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    // 1. One-Tap UPI Activity Result Handler
    private val upiLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = result.data?.getStringExtra("response") ?: ""
        Log.d("PaywallActivity", "UPI Response: $response | resultCode: ${result.resultCode}")

        val isSuccess = response.contains("status=SUCCESS", ignoreCase = true) ||
                response.contains("status=SUBMITTED", ignoreCase = true) ||
                response.contains("Status=SUCCESS", ignoreCase = true)

        if (isSuccess) {
            showSuccessDialogAndUnlock()
        } else {
            Toast.makeText(this, "Payment cancelled or incomplete.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val upiUriString = getUpiUriString()
        val qrBitmap = generateQrBitmap(upiUriString, 512)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
                    qrBitmap = qrBitmap,
                    upiId = UPI_PA,
                    onCopyUpiId = {
                        copyUpiIdToClipboard(UPI_PA)
                    },
                    onPayClicked = {
                        launchUpiPaymentIntent()
                    },
                    onEnterKeyClicked = {
                        showSecretKeyDialog()
                    }
                )
            }
        }
    }

    /**
     * Generate 512x512 QR Code Bitmap at runtime
     */
    private fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, size, size)
        } catch (e: Exception) {
            Log.e("PaywallActivity", "Failed to generate QR code", e)
            null
        }
    }

    /**
     * Copy UPI ID to device clipboard
     */
    private fun copyUpiIdToClipboard(upiId: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", upiId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
    }

    /**
     * One-Tap UPI Intent Launcher
     */
    private fun launchUpiPaymentIntent() {
        try {
            val uri = Uri.parse(getUpiUriString())
            val upiIntent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(upiIntent, "Pay ₹399 via UPI")

            if (upiIntent.resolveActivity(packageManager) != null || chooser.resolveActivity(packageManager) != null) {
                upiLauncher.launch(chooser)
            } else {
                try {
                    upiLauncher.launch(upiIntent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No supported UPI App (PhonePe, GPay, Paytm, BHIM) found on device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PaywallActivity", "Error starting UPI intent", e)
            Toast.makeText(this, "Failed to launch UPI payment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Payment Success Dialog & Pro Unlock
     */
    private fun showSuccessDialogAndUnlock() {
        // Set SharedPreferences is_pro_unlocked = true
        val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PRO_UNLOCKED, true).apply()
        PreferenceManager.setProUnlocked(this, true)

        // Display non-cancelable Material AlertDialog
        AlertDialog.Builder(this)
            .setTitle("🎉 Payment Successful!")
            .setMessage("Payment of ₹399 received.\n\n🔑 Your Lifetime Key: $MASTER_SECRET_KEY\n\nMemory Plus Pro is now permanently unlocked on this device!")
            .setCancelable(false)
            .setPositiveButton("Continue to App") { dialog, _ ->
                dialog.dismiss()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .show()
    }

    /**
     * Fallback Secret Key Dialog: Unlocks with "MP2026PRO"
     */
    private fun showSecretKeyDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter Pro Key (e.g. MP2026PRO)"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Enter Pro Key")
            .setMessage("If you paid manually via QR code or have an offline activation key, enter it below:")
            .setView(input)
            .setPositiveButton("Activate") { dialog, _ ->
                val key = input.text.toString().trim()
                if (key.equals(MASTER_SECRET_KEY, ignoreCase = true) || PreferenceManager.verifyAndUnlockSecretKey(this, key)) {
                    val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_IS_PRO_UNLOCKED, true).apply()
                    PreferenceManager.setProUnlocked(this, true)

                    Toast.makeText(this, "Pro Lifetime Activated!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid Activation Key", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

@Composable
fun PaywallScreenContent(
    qrBitmap: Bitmap?,
    upiId: String,
    onCopyUpiId: () -> Unit,
    onPayClicked: () -> Unit,
    onEnterKeyClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Top Badge: "🔥 2 FREE REMINDERS USED — UPGRADE TO PRO"
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEA580C)
            ) {
                Text(
                    text = "🔥 2 FREE REMINDERS USED — UPGRADE TO PRO",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lock Graphic Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AmberAccent, Color(0xFFFF6F00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Icon",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = "Memory Plus Pro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Scan QR or tap below to unlock unlimited voice reminders, alarms & edits forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Dynamic QR Code Card (220dp x 220dp with white background padding)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Price Tag
                    Text(
                        text = "₹399 One-Time Lifetime",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Scan with any UPI App (PhonePe / GPay / Paytm)",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // QR Code Frame
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI Payment QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // UPI ID with Copy Action
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.clickable { onCopyUpiId() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UPI ID: $upiId",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy UPI ID",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Feature Highlights
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaywallFeatureRow("Unlimited Alarms, Edits & Voice Notes")
                    PaywallFeatureRow("1-Tap AI Natural Voice Input")
                    PaywallFeatureRow("Battery-Optimized 0s Delay Alarms")
                    PaywallFeatureRow("Lifetime Access (100% Instant Auto-Unlock)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Primary Emerald Green Button: "Pay ₹399 via UPI (PhonePe / GPay / Paytm)"
            Button(
                onClick = onPayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay ₹399 via UPI (PhonePe / GPay / Paytm)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Secondary Button: "Already Paid? Enter Secret Key"
            TextButton(
                onClick = onEnterKeyClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Already Paid? Enter Secret Key",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Direct UPI Instant Verification • Lifetime Access",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaywallFeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF1F5F9)
        )
    }
}
