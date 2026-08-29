package com.example.ui.paywall

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.preferences.PreferenceManager
import com.example.ui.theme.MyApplicationTheme

class PaywallActivity : ComponentActivity() {

    companion object {
        const val UPI_PA = "BHARATPE.8N0I0W8E7X20381@fbpe"
        const val UPI_PN = "Memory Plus"
        const val DEAL_PRICE = "399"
        const val REGULAR_PRICE = "₹1,200 / Year"
        const val UPI_CU = "INR"
        const val UPI_TN = "Memory Plus Lifetime Pro"
        const val MASTER_SECRET_KEY = "MP2026PRO"
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

    // Zero-bypass Activity Result Launcher to capture & strictly validate UPI status
    private val upiLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val responseData = result.data?.getStringExtra("response") ?: ""
        Log.d("PaywallActivity", "UPI Response: $responseData | resultCode: ${result.resultCode}")
        val lower = responseData.lowercase()

        val isSuccess = (result.resultCode == Activity.RESULT_OK) &&
                (lower.contains("status=success") || lower.contains("status=submitted") || lower.contains("success")) &&
                !lower.contains("status=failure") &&
                !lower.contains("status=failed")

        if (isSuccess) {
            unlockProLifetime()
        } else {
            Toast.makeText(this, "Payment cancel ya incomplete hai. Pro LOCKED hai.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
                    onPayClicked = { launchUpiPaymentIntent() },
                    onSecretKeyClicked = { showSecretKeyDialog() }
                )
            }
        }
    }

    /**
     * One-Tap UPI Intent Launcher
     */
    private fun launchUpiPaymentIntent() {
        try {
            val txnRef = "TXN" + System.currentTimeMillis()
            val uriString = "upi://pay?" +
                    "pa=$UPI_PA" +
                    "&pn=" + Uri.encode(UPI_PN) +
                    "&mc=" +
                    "&tr=" + txnRef +
                    "&am=$DEAL_PRICE" +
                    "&cu=$UPI_CU" +
                    "&tn=" + Uri.encode(UPI_TN)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            val chooser = Intent.createChooser(intent, "Pay ₹$DEAL_PRICE via UPI App")

            if (intent.resolveActivity(packageManager) != null || chooser.resolveActivity(packageManager) != null) {
                upiLauncher.launch(chooser)
            } else {
                try {
                    upiLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No supported UPI App (PhonePe, GPay, Paytm) found on device.",
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
     * Permanent Lifetime Pro Unlock logic
     */
    private fun unlockProLifetime() {
        // 1. Set is_pro_unlocked in SharedPreferences
        val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PRO_UNLOCKED, true).apply()
        PreferenceManager.setProUnlocked(this, true)

        // 2. Show Non-cancelable Confirmation Dialog
        AlertDialog.Builder(this)
            .setTitle("🎉 Payment Successful!")
            .setMessage("₹399 payment received successfully. You secured the Early Bird Lifetime Pro! Unlimited reminders unlocked permanently.")
            .setCancelable(false)
            .setPositiveButton("Start Using Pro") { dialog, _ ->
                dialog.dismiss()
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(mainIntent)
                finish()
            }
            .show()
    }

    /**
     * Master Secret Key Dialog Fallback
     */
    private fun showSecretKeyDialog() {
        val input = EditText(this).apply {
            hint = "Enter Master Secret Key (e.g. MP2026PRO)"
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("🔑 Unlock Lifetime Pro")
            .setMessage("Enter your secret key or authorization code to activate Lifetime Pro:")
            .setView(container)
            .setPositiveButton("Activate") { dialog, _ ->
                val enteredKey = input.text.toString().trim()
                if (enteredKey.equals(MASTER_SECRET_KEY, ignoreCase = true)) {
                    dialog.dismiss()
                    unlockProLifetime()
                } else {
                    Toast.makeText(this, "❌ Invalid Secret Key", Toast.LENGTH_SHORT).show()
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
    onPayClicked: () -> Unit,
    onSecretKeyClicked: () -> Unit
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
            Spacer(modifier = Modifier.height(8.dp))

            // 1. TOP URGENCY PILL BADGE
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔥 EARLY BIRD OFFER — FIRST 1,000 USERS ONLY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Price increases to ₹1,200/yr once 1,000 seats fill up",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. LIVE SLOTS PROGRESS BAR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔴 953 / 1,000 Claimed",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                        Text(
                            text = "Only 47 Spots Left!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { 0.953f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFEF4444),
                        trackColor = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. PRICE ANCHOR CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Regular Price Strikethrough
                    Text(
                        text = "Regular Price: ₹1,200 / Year",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        textDecoration = TextDecoration.LineThrough
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Deal Price Huge Bold Text
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹399",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "One-Time Lifetime Access",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Discount Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF059669).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "SAVE 85% TODAY • ZERO MONTHLY FEES",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. FEATURE BULLETS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FomoFeatureRow(
                        icon = Icons.Default.Bolt,
                        tint = Color(0xFFF59E0B),
                        text = "⚡ Unlimited Voice & Photo Reminders"
                    )
                    FomoFeatureRow(
                        icon = Icons.Default.Notifications,
                        tint = Color(0xFF38BDF8),
                        text = "🔔 Smart Voice Alerts & Notifications"
                    )
                    FomoFeatureRow(
                        icon = Icons.Default.Security,
                        tint = Color(0xFF10B981),
                        text = "🛡️ Lifetime Free Updates & No Ads"
                    )
                    FomoFeatureRow(
                        icon = Icons.Default.CheckCircle,
                        tint = Color(0xFFA855F7),
                        text = "🔒 Instant Auto-Unlock via UPI"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. PRIMARY CTA BUTTON
            Button(
                onClick = onPayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚡ GET LIFETIME PRO FOR ₹399",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtext for Security & Apps
            Text(
                text = "🔒 100% Safe Instant Payment via BharatPe / PhonePe / GPay / Paytm",
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. SECONDARY LINK: "Already Paid? Enter Secret Key"
            TextButton(
                onClick = onSecretKeyClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Already Paid? Enter Secret Key",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FomoFeatureRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
    }
}
