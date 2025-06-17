package com.example.instagramnotificationenhancer

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

// Data class to hold the info for each app
data class AppInfo(
    val name: String,
    val icon: Drawable,
    val packageName: String
)

class AppListAdapter(private val apps: List<AppInfo>, private val prefs: SharedPreferences) :
    RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
        val appToggle: SwitchMaterial = view.findViewById(R.id.app_toggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name
        holder.appIcon.setImageDrawable(app.icon)

        // Set the switch state based on saved preferences
        holder.appToggle.isChecked = prefs.getBoolean(app.packageName, false)

        // When the switch is clicked, save the new state
        holder.appToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(app.packageName, isChecked).apply()
        }
    }
}