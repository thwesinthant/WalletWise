package com.example.walletwise.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R

class IconGridAdapter(
    private val icons: List<Int>,
    private val selectedIcon: Int,
    private val onPicked: (Int) -> Unit
) : RecyclerView.Adapter<IconGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.iconOptionImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val icon = icons[position]
        holder.image.setImageResource(icon)
        holder.image.setColorFilter(
            if (icon == selectedIcon) 0xFF3D7BD9.toInt() else 0xFFAAAAAA.toInt()
        )
        holder.itemView.setOnClickListener { onPicked(icon) }
    }

    override fun getItemCount() = icons.size
}