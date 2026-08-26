package com.example.walletwise.category

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R

class ColorSwatchAdapter(
    private val colors: List<Int>,
    initialSelectedIndex: Int = 0,
    private val onSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorSwatchAdapter.ViewHolder>() {

    private var selectedPosition = initialSelectedIndex.coerceIn(0, (colors.size - 1).coerceAtLeast(0))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val swatch: View = view.findViewById(R.id.colorSwatch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_swatch, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        val isSelected = position == selectedPosition

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (isSelected) {
                setStroke(6, 0xFF333333.toInt())
            }
        }
        holder.swatch.background = bg

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPosition
            selectedPosition = currentPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onSelected(colors[currentPosition])
        }
    }

    override fun getItemCount() = colors.size
}