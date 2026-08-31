package com.memoryplus.app

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_add_reminder) {
                checkLimitAndOpenCreateDialog()
                true
            } else false
        }

        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddReminder)
        fabAdd.setOnClickListener {
            showCreateReminderDialog()
        }
    }

    private fun checkLimitAndOpenCreateDialog() {
        showCreateReminderDialog()
    }

    private fun showCreateReminderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_reminder, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etReminderTitle)
        val btnPickDate = dialogView.findViewById<Button>(R.id.btnPickDate)
        val btnPickTime = dialogView.findViewById<Button>(R.id.btnPickTime)
        val tvSelectedDateTime = dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)

        val selectedCalendar = Calendar.getInstance()
        var isDateSelected = false
        var isTimeSelected = false

        val dialog = AlertDialog.Builder(this)
            .setTitle("➕ New Reminder")
            .setView(dialogView)
            .setPositiveButton("Save Reminder", null)
            .setNegativeButton("Cancel", null)
            .create()

        btnPickDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                isDateSelected = true
                updateDateTimeLabel(tvSelectedDateTime, selectedCalendar)
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        btnPickTime.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                isTimeSelected = true
                updateDateTimeLabel(tvSelectedDateTime, selectedCalendar)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveBtn.setOnClickListener {
                val title = etTitle.text.toString().trim()

                if (title.isEmpty()) {
                    etTitle.error = "Reminder title likhein"
                    return@setOnClickListener
                }

                if (!isDateSelected || !isTimeSelected) {
                    Toast.makeText(this, "Kripya Date aur Time dono select karein", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (selectedCalendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Aane wala (Future) time select karein", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                val isPro = prefs.getBoolean("is_pro_unlocked", false)
                if (!isPro) {
                    val count = prefs.getInt("reminder_count", 0)
                    prefs.edit().putInt("reminder_count", count + 1).apply()
                }

                scheduleExactAlarm(title, selectedCalendar.timeInMillis)
                Toast.makeText(this, "✅ Reminder Set Successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateDateTimeLabel(tv: TextView, cal: Calendar) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        tv.text = "Scheduled for: ${sdf.format(cal.time)}"
    }

    private fun scheduleExactAlarm(title: String, timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            timeInMillis.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
}
