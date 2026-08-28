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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.example.data.preferences.PreferenceManager
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class PaywallActivity : ComponentActivity() {

    companion object {
        const val UPI_PA = "7024991656@apl"
        const val UPI_PN = "Uma Bai Gurjar"
        const val UPI_AM = "399"
        const val UPI_CU = "INR"
        const val UPI_TN = "Memory Plus Pro Lifetime"

        val UPI_URI_STRING: String = "upi://pay?pa=$UPI_PA&pn=Uma%20Bai%20Gurjar&am=$UPI_AM&cu=$UPI_CU&tn=Memory%20Plus%20Pro%20Lifetime"

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    // 1. UPI Activity Result Launcher
    private val upiPaymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleUpiPaymentResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
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
     * Manual Secret Key Fallback: Unlocks ONLY when exact key "MP2026PRO" is entered
     */
    private fun showSecretKeyDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter Pro Activation Key"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Enter Pro Key")
            .setMessage("If you paid manually via QR or need offline access, enter your activation key:")
            .setView(input)
            .setPositiveButton("Activate") { dialog, _ ->
                val key = input.text.toString().trim()
                if (PreferenceManager.verifyAndUnlockSecretKey(this, key)) {
                    Toast.makeText(this, "Pro Unlocked Successfully 🎉", Toast.LENGTH_LONG).show()
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

    /**
     * Launch Native UPI Payment Intent
     */
    private fun launchUpiPaymentIntent() {
        try {
            val uri = Uri.parse(UPI_URI_STRING)
            val upiIntent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(upiIntent, "Pay ₹399 via UPI App")

            if (upiIntent.resolveActivity(packageManager) != null || chooser.resolveActivity(packageManager) != null) {
                upiPaymentLauncher.launch(chooser)
            } else {
                try {
                    upiPaymentLauncher.launch(upiIntent)
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
     * Strict UPI Response Handler:
     * - Disallows empty/null response
     * - Disallows RESULT_CANCELED
     * - ONLY unlocks if status is "SUCCESS" or "SUBMITTED"
     */
    private fun handleUpiPaymentResult(resultCode: Int, data: Intent?) {
        val response = data?.getStringExtra("response") ?: ""
        Log.d("PaywallActivity", "UPI Response: $response | resultCode: $resultCode")

        if (resultCode == Activity.RESULT_OK && response.isNotBlank() && isUpiSuccess(response)) {
            // Update SharedPreferences: is_pro_unlocked = true
            PreferenceManager.setProUnlocked(this, true)

            Toast.makeText(
                this,
                "Payment Successful! Memory Plus Pro Lifetime Unlocked 🎉",
                Toast.LENGTH_LONG
            ).show()

            setResult(Activity.RESULT_OK)
            finish()
        } else {
            Toast.makeText(
                this,
                "Payment was not completed or failed. Pro features remain locked.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Helper to parse UPI key-values and status strictly
     */
    private fun isUpiSuccess(response: String): Boolean {
        if (response.isBlank()) {
            return false
        }

        // Parse key=value pairs standard in NPCI UPI responses (e.g. txnId=...&responseCode=...&Status=SUCCESS&...)
        val params = response.split("&").associate { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                parts[0].trim().lowercase(Locale.ROOT) to parts[1].trim()
            } else {
                parts[0].trim().lowercase(Locale.ROOT) to ""
            }
        }

        val status = params["status"] ?: params["txnstatus"] ?: ""
        return status.equals("SUCCESS", ignoreCase = true) || status.equals("SUBMITTED", ignoreCase = true)
    }
}

@Composable
fun PaywallScreenContent(
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

            // 1. Top Banner / Badge: 2-Day Limited Time Offer
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEA580C)
            ) {
                Text(
                    text = "🔥 LIMITED TIME LAUNCH OFFER (2 DAYS ONLY)",
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

            // Header
            Text(
                text = "Memory Plus Pro - Upgrade Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "You have reached the limit of 2 free reminders/actions. Upgrade to unlock unlimited voice reminders & edits forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Pricing Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Original Price (Strikethrough)
                    Text(
                        text = "₹999 / Year",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Deal Price (Large Bold Green)
                    Text(
                        text = "₹399 Lifetime Access",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Savings Note / Subtext
                    Text(
                        text = "Pay Once • Lifetime Unlimited Reminders & Edits",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PaywallFeatureRow("Unlimited Alarms, Edits & Voice Notes")
                    PaywallFeatureRow("1-Tap AI Natural Voice Input")
                    PaywallFeatureRow("Battery-Optimized 0s Delay Alarms")
                    PaywallFeatureRow("Lifetime Access (100% Instant Auto-Unlock)")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Primary UPI Intent
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
                    text = "Pay ₹399 via PhonePe / GPay / UPI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Manual Key Fallback Button: Unlocks ONLY with MP2026PRO
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

            Spacer(modifier = Modifier.height(8.dp))

            // Secure Payment Badge
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF1F5F9)
        )
    }
}
