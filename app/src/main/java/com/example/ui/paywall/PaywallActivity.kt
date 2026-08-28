package com.example.ui.paywall

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.preferences.PreferenceManager
import com.example.ui.theme.*

class PaywallActivity : ComponentActivity() {

    companion object {
        const val STRIPE_PAYMENT_URL = "https://buy.stripe.com/YOUR_STRIPE_LINK"
        const val RAZORPAY_UPI_URL = "https://rzp.io/l/YOUR_RAZORPAY_LINK"

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Block hardware/gesture back press if locked
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (PreferenceManager.isLocked(this@PaywallActivity)) {
                    Toast.makeText(
                        this@PaywallActivity,
                        "Free trial ended. Please unlock Pro to continue.",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Minimize app rather than bypassing lock
                    moveTaskToBack(true)
                } else {
                    isEnabled = false
                    finish()
                }
            }
        })

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
                    onPayStripe = {
                        openUrl(STRIPE_PAYMENT_URL)
                    },
                    onPayRazorpay = {
                        openUrl(RAZORPAY_UPI_URL)
                    },
                    onSecretKeyClick = {
                        showSecretKeyDialog()
                    }
                )
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open payment link. Please check your browser.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSecretKeyDialog() {
        val input = EditText(this).apply {
            hint = "Enter Secret Key (e.g. MP2026PRO)"
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Pro Secret Key")
            .setMessage("If you have completed payment or received a VIP activation key, enter it below:")
            .setView(input)
            .setPositiveButton("Activate") { dialog, _ ->
                val key = input.text.toString().trim()
                if (PreferenceManager.verifyAndUnlockSecretKey(this, key)) {
                    Toast.makeText(
                        this,
                        "🎉 Memory Plus Pro Unlocked Successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "❌ Invalid Secret Key. Please try again.",
                        Toast.LENGTH_SHORT
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
    onPayStripe: () -> Unit,
    onPayRazorpay: () -> Unit,
    onSecretKeyClick: () -> Unit
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
            Spacer(modifier = Modifier.height(16.dp))

            // Lock & Crown Visual Icon
            Box(
                modifier = Modifier
                    .size(88.dp)
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
                    contentDescription = "Trial Ended Lock",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Memory Plus Pro - Free Trial Ended",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You have used your 2 free reminders. Upgrade to Pro to continue using Memory Plus.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PaywallFeatureRow("Unlimited Reminders & Smart Alarms")
                    PaywallFeatureRow("1-Tap AI Voice Input (Hindi & English)")
                    PaywallFeatureRow("Battery-Optimized Exact Alarms (Xiaomi, Samsung, Vivo)")
                    PaywallFeatureRow("Lifetime Pro Access & No Ads")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Button 1: Stripe (US/UK)
            Button(
                onClick = onPayStripe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pay with Card / Apple Pay ($4.99 / Year - US/UK)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 2: Razorpay / UPI / PhonePe / GPay (India)
            Button(
                onClick = onPayRazorpay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pay with PhonePe / Google Pay / UPI (₹399 / Year - India)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 3: Secret Key
            OutlinedButton(
                onClick = onSecretKeyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64748B)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = AmberAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Already Paid? Enter Secret Key",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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
