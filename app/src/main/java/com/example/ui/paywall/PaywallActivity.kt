package com.example.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class PaywallActivity : ComponentActivity(), PaymentResultListener {

    companion object {
        // Razorpay API Key ID (Replace with your actual Razorpay Key rzp_live_xxx or rzp_test_xxx)
        const val RAZORPAY_KEY_ID = "rzp_live_R4Z0RP4YK3Y1D"

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload Razorpay Checkout for ultra-fast launch
        try {
            Checkout.preload(applicationContext)
        } catch (e: Exception) {
            Log.e("PaywallActivity", "Razorpay preload exception", e)
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                PaywallScreenContent(
                    onPayClicked = {
                        startRazorpayPayment()
                    }
                )
            }
        }
    }

    /**
     * Trigger Razorpay Checkout for automated in-app payment unlock
     */
    private fun startRazorpayPayment() {
        val checkout = Checkout()
        checkout.setKeyID(RAZORPAY_KEY_ID)

        try {
            val options = JSONObject().apply {
                put("name", "Memory Plus")
                put("description", "Memory Plus Pro - Lifetime Unlimited Access")
                put("currency", "INR")
                put("amount", 39900) // ₹399 in paise (399 * 100)

                // Theme customization
                val theme = JSONObject()
                theme.put("color", "#059669")
                put("theme", theme)

                // Retry configuration
                val retryObj = JSONObject()
                retryObj.put("enabled", true)
                retryObj.put("max_count", 4)
                put("retry", retryObj)

                // Prefill user details (optional)
                val prefill = JSONObject()
                prefill.put("email", "support@memoryplus.app")
                put("prefill", prefill)
            }

            checkout.open(this@PaywallActivity, options)
        } catch (e: Exception) {
            Log.e("PaywallActivity", "Error initiating Razorpay checkout", e)
            Toast.makeText(this, "Error starting payment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 100% Automated Unlock upon successful payment
     */
    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Log.d("PaywallActivity", "Payment Successful. Payment ID: $razorpayPaymentID")
        
        // 1. Permanently unlock Pro in SharedPreferences
        PreferenceManager.setProUnlocked(this, true)

        // 2. Show celebration Toast
        Toast.makeText(
            this,
            "Payment Successful! Pro Features Unlocked 🎉",
            Toast.LENGTH_LONG
        ).show()

        // 3. Immediately finish PaywallActivity and return with full access
        setResult(Activity.RESULT_OK)
        finish()
    }

    /**
     * Handle payment cancellation or failure
     */
    override fun onPaymentError(code: Int, response: String?) {
        Log.e("PaywallActivity", "Payment Error: Code $code, Response: $response")
        Toast.makeText(
            this,
            "Payment cancelled or failed. Please try again.",
            Toast.LENGTH_SHORT
        ).show()
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
            Spacer(modifier = Modifier.height(16.dp))

            // Lock Graphic Icon
            Box(
                modifier = Modifier
                    .size(84.dp)
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
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Text(
                text = "Memory Plus Pro - Upgrade Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "You have reached the limit of 2 free reminders. Upgrade to unlock unlimited voice reminders forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PaywallFeatureRow("Unlimited Alarms & Voice Notes")
                    PaywallFeatureRow("1-Tap AI Natural Voice Input")
                    PaywallFeatureRow("Battery-Optimized 0s Delay Alarms")
                    PaywallFeatureRow("Lifetime Access (100% Instant Auto-Unlock)")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Razorpay 1-Tap Checkout Button (UPI / PhonePe / GPay / Cards / Paytm)
            Button(
                onClick = onPayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pay ₹399 via UPI / PhonePe / GPay",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secure Payment Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% Secure Checkout via Razorpay • Instant Unlock",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
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
