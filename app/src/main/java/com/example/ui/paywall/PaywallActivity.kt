package com.example.ui.paywall

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.preferences.PreferenceManager
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.MyApplicationTheme

class PaywallActivity : ComponentActivity() {

    companion object {
        const val UPI_PA = "BHARATPE.8N0I0W8E7X20381@fbpe"
        const val UPI_PN = "Memory Plus"
        const val UPI_AM = "399"
        const val UPI_CU = "INR"
        const val UPI_TN = "Memory Plus Pro Lifetime"
        const val SHARED_PREFS_FILE = "MemoryPlusPrefs"
        const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    // Activity Result Launcher to capture and verify payment response from UPI apps
    private val upiLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val responseData = result.data?.getStringExtra("response") ?: ""
        Log.d("PaywallActivity", "UPI Response: $responseData | resultCode: ${result.resultCode}")
        val lower = responseData.lowercase()

        val isSuccess = (result.resultCode == Activity.RESULT_OK) &&
                (lower.contains("status=success") || lower.contains("status=submitted") || lower.contains("success")) &&
                !lower.contains("status=failure") &&
                !lower.contains("status=failed")

        if (isSuccess) {
            // 1. Permanently Unlock Pro
            val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_PRO_UNLOCKED, true).apply()
            PreferenceManager.setProUnlocked(this, true)

            // 2. Show Success Confirmation
            AlertDialog.Builder(this)
                .setTitle("🎉 Payment Successful!")
                .setMessage("₹399 payment receive ho gaya hai. Memory Plus Pro Lifetime unlock ho chuka hai!")
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
        } else {
            Toast.makeText(this, "Payment poora nahi hua ya cancel ho gaya. Pro locked hai.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
                    onPayClicked = {
                        launchUpiPaymentIntent()
                    }
                )
            }
        }
    }

    /**
     * One-Tap UPI Launcher Logic
     */
    private fun launchUpiPaymentIntent() {
        try {
            val txnRef = "TXN" + System.currentTimeMillis()
            val uriString = "upi://pay?" +
                    "pa=$UPI_PA" +
                    "&pn=" + Uri.encode(UPI_PN) +
                    "&mc=" +
                    "&tr=" + txnRef +
                    "&am=$UPI_AM" +
                    "&cu=$UPI_CU" +
                    "&tn=" + Uri.encode(UPI_TN)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            val chooser = Intent.createChooser(intent, "Pay ₹399 via UPI App")

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
}

@Composable
fun PaywallScreenContent(
    onPayClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Header Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEA580C)
            ) {
                Text(
                    text = "⚡ UPGRADE TO LIFETIME PRO",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lock Graphic Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
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
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
                text = "Memory Plus Pro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Unlock unlimited natural voice reminders, custom alarms and edits forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Price Badge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "₹399 One-Time Payment",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Pay Once • Use Forever on this Device",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PaywallFeatureRow(
                        icon = Icons.Default.Bolt,
                        tint = Color(0xFFF59E0B),
                        text = "⚡ Unlimited Reminders"
                    )
                    PaywallFeatureRow(
                        icon = Icons.Default.Notifications,
                        tint = Color(0xFF38BDF8),
                        text = "🔔 Voice Alerts & Custom Sounds"
                    )
                    PaywallFeatureRow(
                        icon = Icons.Default.Security,
                        tint = Color(0xFF10B981),
                        text = "🛡️ 100% Ad-Free Lifetime Access"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Emerald Green CTA Button: 1-Tap UPI Launcher
            Button(
                onClick = onPayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚡ Pay ₹399 & Unlock Lifetime Pro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtext
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Supports PhonePe, Google Pay, Paytm & all UPI Apps",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaywallFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
    }
}
