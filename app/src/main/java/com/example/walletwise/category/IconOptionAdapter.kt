package com.example.walletwise.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R

class IconOptionAdapter(
    private val icons: List<Int>,
    private val moreIconRes: Int? = null,
    initialSelectedIndex: Int = 0,
    private val onSelected: (Int) -> Unit,
    private val onMoreClicked: (() -> Unit)? = null
) : RecyclerView.Adapter<IconOptionAdapter.ViewHolder>() {

    private var selectedPosition = initialSelectedIndex.coerceIn(0, (icons.size - 1).coerceAtLeast(0))
    private val hasMoreTile = moreIconRes != null && onMoreClicked != null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: FrameLayout = view.findViewById(R.id.iconOptionRoot)
        val image: ImageView = view.findViewById(R.id.iconOptionImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isMoreTile = hasMoreTile && position == icons.size

        if (isMoreTile) {
            holder.image.setImageResource(moreIconRes!!)
            holder.image.setColorFilter(0xFF8FA0B3.toInt())
            holder.root.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.bg_category_tile_dashed
            )
            holder.itemView.setOnClickListener { onMoreClicked?.invoke() }
            return
        }

        holder.root.background = null
        val isSelected = position == selectedPosition
        holder.image.setImageResource(icons[position])
        holder.image.setColorFilter(
            if (isSelected) 0xFF3D7BD9.toInt() else 0xFFAAAAAA.toInt()
        )

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPosition
            selectedPosition = currentPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onSelected(icons[currentPosition])
        }
    }

    fun selectExternally(iconRes: Int) {
        val index = icons.indexOf(iconRes)
        if (index >= 0) {
            val previous = selectedPosition
            selectedPosition = index
            notifyItemChanged(previous)
            notifyItemChanged(index)
        }
    }

    override fun getItemCount() = icons.size + if (hasMoreTile) 1 else 0
}