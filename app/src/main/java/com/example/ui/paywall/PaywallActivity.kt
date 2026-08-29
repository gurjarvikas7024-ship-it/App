package com.example.ui.paywall

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.PreferenceManager
import com.example.ui.theme.MyApplicationTheme

class PaywallActivity : ComponentActivity() {

    companion object {
        const val UPI_ID = "7024991656@ybl"
        const val DEAL_PRICE = "₹399"
        const val REGULAR_PRICE = "₹1,200 / Year"
        const val WHATSAPP_TARGET = "917024991656"
        const val SECRET_MASTER_KEY = "MP2026PRO"
        const val SHARED_PREFS_FILE = "MemoryPlusPrefs"
        const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"
        const val KEY_REMINDER_COUNT = "reminder_count"

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
                    upiId = UPI_ID,
                    onCopyUpiId = { copyUpiIdToClipboard() },
                    onWhatsAppClick = { openWhatsAppChat() },
                    onEnterKeyClick = { showEnterActivationKeyDialog() }
                )
            }
        }
    }

    /**
     * 1. COPY UPI ID ACTION
     */
    private fun copyUpiIdToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", UPI_ID)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "UPI ID Copied: $UPI_ID", Toast.LENGTH_SHORT).show()
    }

    /**
     * 2. WHATSAPP BUTTON ACTION (Opens WhatsApp directly without showing number on UI)
     */
    private fun openWhatsAppChat() {
        try {
            val prefilledMessage = "Hello Memory Plus Team,\nMaine ₹399 pay kar diye hain. Ye raha mera payment screenshot.\nKripya mera Activation Code share karein."
            val whatsappUri = "https://api.whatsapp.com/send?phone=$WHATSAPP_TARGET&text=" + Uri.encode(prefilledMessage)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUri))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp is not installed or unable to open link.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 3. ENTER ACTIVATION KEY ACTION
     */
    private fun showEnterActivationKeyDialog() {
        val input = EditText(this).apply {
            hint = "Enter Activation Key"
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("🔑 Enter Activation Key")
            .setMessage("Please enter the activation key received on WhatsApp:")
            .setView(container)
            .setPositiveButton("Activate") { dialog, _ ->
                val enteredKey = input.text.toString().trim()
                if (enteredKey.equals(SECRET_MASTER_KEY, ignoreCase = true)) {
                    dialog.dismiss()
                    val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_IS_PRO_UNLOCKED, true).apply()
                    PreferenceManager.setProUnlocked(this, true)

                    AlertDialog.Builder(this)
                        .setTitle("🎉 Lifetime Pro Activated!")
                        .setMessage("Memory Plus Lifetime Pro unlock ho chuka hai! Ab aap unlimited voice & photo reminders use kar sakte hain.")
                        .setCancelable(false)
                        .setPositiveButton("Start Using") { successDialog, _ ->
                            successDialog.dismiss()
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(mainIntent)
                            finish()
                        }
                        .show()
                } else {
                    Toast.makeText(
                        this,
                        "Galat Key! WhatsApp par payment screenshot share karke sahi key prapt karein.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

@Composable
fun PaywallScreenContent(
    upiId: String,
    onCopyUpiId: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onEnterKeyClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. TOP FOMO BADGE
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔥 EARLY BIRD OFFER — FIRST 1,000 USERS ONLY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "🔴 953 / 1,000 Claimed — Only 47 spots remaining at ₹399 (Regular ₹1,200/yr)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDE047),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. QR DISPLAY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // QR Code Frame
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.qr_phonepe),
                            contentDescription = "Scan QR Code",
                            modifier = Modifier.size(210.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Scan & Pay ₹399 via Any UPI App\n(PhonePe / GPay / Paytm)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // UPI ID Bar with Copy Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "UPI ID: $upiId",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                            Button(
                                onClick = onCopyUpiId,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy UPI ID",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. WHATSAPP CTA BUTTON
            Button(
                onClick = onWhatsAppClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
            ) {
                Text(
                    text = "💬 Share Screenshot on WhatsApp to Get Key",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Payment ka screenshot bhejein aur 1 minute me Activation Key paayein",
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. ENTER KEY BUTTON
            OutlinedButton(
                onClick = onEnterKeyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔑 Enter Activation Key",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
