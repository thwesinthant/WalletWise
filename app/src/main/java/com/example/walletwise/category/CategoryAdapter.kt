package com.example.walletwise

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.entity.Category

class CategoryAdapter(
    private val items: MutableList<Category>,
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    /** Replaces the whole displayed list (including the "Add" tile) with fresh data. */
    fun submitList(newItems: List<Category>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tile: FrameLayout = view.findViewById(R.id.iconTile)
        val icon: ImageView = view.findViewById(R.id.iconImage)
        val label: TextView = view.findViewById(R.id.labelText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.icon.setImageResource(item.iconRes)
        holder.label.text = item.label

        if (item.isAddAction) {
            holder.tile.background = ContextCompat.getDrawable(context, R.drawable.bg_category_tile_dashed)
            holder.icon.setColorFilter(Color.GRAY)
        } else {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * context.resources.displayMetrics.density
                setColor(item.bgColor)
                setStroke(
                    (1.5 * context.resources.displayMetrics.density).toInt(),
                    ColorUtils.setAlphaComponent(item.tintColor, 90)
                )
            }
            holder.tile.background = bg
            holder.icon.setColorFilter(item.tintColor)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
