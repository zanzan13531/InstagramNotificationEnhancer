package com.example.instagramnotificationenhancer;

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var permissionBanner: LinearLayout
    private lateinit var permissionButton: Button
    private lateinit var appListContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find all our views
        permissionBanner = findViewById(R.id.permission_banner)
        permissionButton = findViewById(R.id.permission_button)
        appListContainer = findViewById(R.id.app_list_container)
        recyclerView = findViewById(R.id.app_list_recycler_view)

        // Set up the button to open settings
        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Set up the app list
        val appList = getInstalledApps()
        val sharedPrefs = getSharedPreferences("ReclassifierPrefs", MODE_PRIVATE)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppListAdapter(appList, sharedPrefs)
    }

    override fun onResume() {
        super.onResume()
        // Every time the user comes back to the app, check the permission status
        updateUiBasedOnPermission()
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    private fun updateUiBasedOnPermission() {
        if (isNotificationServiceEnabled()) {
            // Permission is GRANTED: show app list, hide banner
            permissionBanner.visibility = View.GONE
            appListContainer.visibility = View.VISIBLE
        } else {
            // Permission is DENIED: hide app list, show banner
            permissionBanner.visibility = View.VISIBLE
            appListContainer.visibility = View.GONE
        }
    }

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