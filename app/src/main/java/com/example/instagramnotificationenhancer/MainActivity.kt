package com.example.instagramnotificationenhancer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    // Your existing views
    private lateinit var listenerPermissionBanner: LinearLayout
    private lateinit var grantListenerPermissionButton: Button
    private lateinit var appListContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView

    // New views for the second banner
    private lateinit var disabledNotificationBanner: LinearLayout
    private lateinit var enableNotificationsButton: Button

    // New permission launcher for the pop-up dialog
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications are required for this app to function.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find your existing views
        listenerPermissionBanner = findViewById(R.id.permission_banner)
        grantListenerPermissionButton = findViewById(R.id.permission_button)
        appListContainer = findViewById(R.id.app_list_container)
        recyclerView = findViewById(R.id.app_list_recycler_view)

        // Find the new views from the merged layout
        disabledNotificationBanner = findViewById(R.id.disabled_notification_banner)
        enableNotificationsButton = findViewById(R.id.enable_notifications_button)

        // Your existing click listener is preserved
        grantListenerPermissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // New click listener for the new banner
        enableNotificationsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        }

        // New function call to ask for the runtime permission
        askForPostNotificationPermission()

        // Your existing app list setup is preserved
        val appList = getInstalledApps()
        val sharedPrefs = getSharedPreferences("ReclassifierPrefs", MODE_PRIVATE)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppListAdapter(appList, sharedPrefs)
    }

    override fun onResume() {
        super.onResume()
        // We call the new, more powerful update function
        updateUiBasedOnPermissions()
    }

    // Your existing isNotificationServiceEnabled() function is preserved
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    // This is the updated version of your updateUiBasedOnPermission() function
    private fun updateUiBasedOnPermissions() {
        val listenerEnabled = isNotificationServiceEnabled()
        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()

        // Logic for the first banner (Listener Service)
        listenerPermissionBanner.visibility = if (listenerEnabled) View.GONE else View.VISIBLE

        // Logic for the second banner (App's own notifications)
        disabledNotificationBanner.visibility = if (notificationsEnabled) View.GONE else View.VISIBLE

        // The app list is only shown if BOTH permissions are granted
        if (listenerEnabled && notificationsEnabled) {
            appListContainer.visibility = View.VISIBLE
        } else {
            appListContainer.visibility = View.GONE
        }
    }

    // This is a new helper function to ask for the pop-up permission
    private fun askForPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is Android 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Your existing getInstalledApps() function is preserved
    private fun getInstalledApps(): List<AppInfo> {
        val pm: PackageManager = packageManager
        val apps = mutableListOf<AppInfo>()
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolvedApps = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolvedApps) {
            val app = AppInfo(
                name = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm),
                packageName = info.activityInfo.packageName
            )
            apps.add(app)
        }
        return apps.sortedBy { it.name.lowercase() }
    }
}