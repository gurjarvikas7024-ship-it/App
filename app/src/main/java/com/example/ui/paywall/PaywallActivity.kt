package com.example.ui.paywall

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
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

class PaywallActivity : ComponentActivity() {

    companion object {
        const val STRIPE_PAYMENT_URL = "https://buy.stripe.com/YOUR_STRIPE_LINK"
        const val UPI_PAYMENT_URI = "upi://pay?pa=7024991656@ybl&pn=Memory%20Plus&am=399&cu=INR&tn=Memory%20Plus%20Pro%20Upgrade"

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
                    onPayUPI = {
                        payViaUPI()
                    },
                    onPayStripe = {
                        payViaStripe()
                    },
                    onSecretKeyClick = {
                        showSecretKeyDialog()
                    }
                )
            }
        }
    }

    /**
     * MODULE 2: Direct UPI Action (No Web Page)
     */
    private fun payViaUPI() {
        try {
            val upiUri = Uri.parse(UPI_PAYMENT_URI)
            val intent = Intent(Intent.ACTION_VIEW, upiUri)
            val chooser = Intent.createChooser(intent, "Pay with PhonePe / UPI")
            startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No UPI app found on device", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch UPI app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * MODULE 2: International Stripe Action
     */
    private fun payViaStripe() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(STRIPE_PAYMENT_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open Stripe checkout in browser", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * MODULE 2: Secret Activation Key Verification
     */
    private fun showSecretKeyDialog() {
        val input = EditText(this).apply {
            hint = "Enter Secret Activation Key"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Pro Activation Key")
            .setMessage("Please enter the activation key you received after successful payment:")
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
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "❌ Invalid Activation Key. Please try again.",
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
    onPayUPI: () -> Unit,
    onPayStripe: () -> Unit,
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

            // Lock Graphic Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
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
                    modifier = Modifier.size(40.dp)
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
                text = "You have reached the limit of 2 free reminders. Upgrade to unlock unlimited voice reminders.",
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
                    PaywallFeatureRow("Unlimited Alarms")
                    PaywallFeatureRow("1-Tap AI Voice Input")
                    PaywallFeatureRow("Battery-Optimized Alarms")
                    PaywallFeatureRow("Lifetime Access")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Button 1: Pay via PhonePe / Google Pay / UPI (₹399 - India)
            Button(
                onClick = onPayUPI,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pay via PhonePe / Google Pay / UPI (₹399 - India)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 2: Pay with Card / Apple Pay ($4.99 - US/UK)
            Button(
                onClick = onPayStripe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pay with Card / Apple Pay ($4.99 - US/UK)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 3: Already Paid? Enter Secret Key
            OutlinedButton(
                onClick = onSecretKeyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFF64748B)),
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
