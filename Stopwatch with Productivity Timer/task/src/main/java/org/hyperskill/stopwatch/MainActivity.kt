package org.hyperskill.stopwatch


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

const val CHANNEL_ID = "org.hyperskill"

class MainActivity : AppCompatActivity() {
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val textView = findViewById<TextView>(R.id.textView)
        val startButton = findViewById<Button>(R.id.startButton)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        var running = false
        var timeElapsed = 0
        var upperLimit = 0
        textView.text = formatSeconds(timeElapsed)
        val handler = Handler(Looper.getMainLooper())

        progressBar.visibility = View.INVISIBLE


        // Create notification channel

        val name = "Notification"
        val descriptionText = "Time exceeded"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)


        // Create notification builder
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Notification")
            .setContentText("Time exceeded")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)


        val updateTimer: Runnable = object : Runnable {
            override fun run() {
                timeElapsed++
                textView.text = formatSeconds(timeElapsed)
                handler.postDelayed(this, 1000)

                progressBar.indeterminateTintList = ColorStateList.valueOf(randomColor())

                if ((upperLimit > 0) && (timeElapsed > upperLimit)) {
                    val notification = notificationBuilder.build().apply {
                        flags = flags or android.app.Notification.FLAG_INSISTENT
                    }
                    notificationManager.notify(393939, notification)
                }

                textView.setTextColor(if ((upperLimit > 0) && (timeElapsed > upperLimit)) Color.RED else Color.BLACK)
            }
        }

        fun startTimer() {
            running = true
            settingsButton.isEnabled = false
            handler.postDelayed(updateTimer, 1000)
            progressBar.visibility = View.VISIBLE
        }

        val notificationsRequestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                // User has granted the permission.
                startTimer()
            } else {
                // User has denied the permission.
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        startButton.setOnClickListener {
            if (!running) {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Notification permission already granted. You can start the timer now.
                        startTimer()
                    }

                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) -> {
                        // The permission was denied some time before. Show the rationale!
                        AlertDialog.Builder(this).setTitle("Permission required")
                            .setMessage("This app needs permission to access this feature.")
                            .setPositiveButton("Grant") { _, _ ->
                                notificationsRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }.setNegativeButton("Cancel", null).show()
                    }

                    else -> {
                        // Permission is not granted. Ask for it
                        notificationsRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        resetButton.setOnClickListener {
            running = false
            timeElapsed = 0
            textView.text = formatSeconds(timeElapsed)
            handler.removeCallbacks(updateTimer)
            progressBar.visibility = View.INVISIBLE
            textView.setTextColor(Color.BLACK)
            settingsButton.isEnabled = true
        }

        settingsButton.setOnClickListener {
            val contentView =
                LayoutInflater.from(this).inflate(R.layout.dialog_upper_limit, null, false)
            AlertDialog.Builder(this)
                .setTitle("Set upper limit in seconds")
                .setView(contentView)
                .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                    val editText = contentView.findViewById<EditText>(R.id.upperLimitEditText)
                    try {
                        upperLimit = editText.text.toString().toInt()
                        dialogInterface.dismiss()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                    }
                }.setNegativeButton(android.R.string.no) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()
        }


    }
}

fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

fun randomColor(): Int {
    val r = Random.nextInt(0, 256)
    val g = Random.nextInt(0, 256)
    val b = Random.nextInt(0, 256)
    return Color.rgb(r, g, b)
}