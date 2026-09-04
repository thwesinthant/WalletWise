package com.example.walletwise.category

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
import com.example.walletwise.R
import com.example.walletwise.entity.Category

class CategoryAdapter(
    private val items: MutableList<Category>,
    private val onClick: (Category) -> Unit,
    private val onLongClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    // ============================================================
    // UPDATE LIST
    // ============================================================

    fun submitList(
        newItems: List<Category>
    ) {

        items.clear()

        items.addAll(
            newItems
        )

        notifyDataSetChanged()
    }

    // ============================================================
    // VIEW HOLDER
    // ============================================================

    class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val tile: FrameLayout =
            view.findViewById(
                R.id.iconTile
            )

        val icon: ImageView =
            view.findViewById(
                R.id.iconImage
            )

        val label: TextView =
            view.findViewById(
                R.id.labelText
            )
    }

    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_category,
                    parent,
                    false
                )

        return ViewHolder(
            view
        )
    }

    // ============================================================
    // BIND VIEW HOLDER
    // ============================================================

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item =
            items[position]

        val context =
            holder.itemView.context

        // ========================================================
        // ICON + LABEL
        // ========================================================

        holder.icon.setImageResource(
            item.iconRes
        )

        holder.label.text =
            item.label

        // ========================================================
        // ADD TILE
        // ========================================================

        if (item.isAddAction) {

            holder.tile.background =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_category_tile_dashed
                )

            holder.icon.setColorFilter(
                Color.GRAY
            )

        }

        // ========================================================
        // NORMAL CATEGORY TILE
        // ========================================================

        else {

            val bg =
                GradientDrawable().apply {

                    shape =
                        GradientDrawable.RECTANGLE

                    cornerRadius =
                        20 *
                                context.resources
                                    .displayMetrics
                                    .density

                    setColor(
                        item.bgColor
                    )

                    setStroke(
                        (
                                1.5 *
                                        context.resources
                                            .displayMetrics
                                            .density
                                ).toInt(),

                        ColorUtils.setAlphaComponent(
                            item.tintColor,
                            90
                        )
                    )
                }

            holder.tile.background =
                bg

            holder.icon.setColorFilter(
                item.tintColor
            )
        }

        // ========================================================
        // NORMAL CLICK
        // ========================================================

        holder.itemView.setOnClickListener {

            onClick(
                item
            )
        }

        // ========================================================
        // LONG CLICK
        // ========================================================

        holder.itemView.setOnLongClickListener {

            /*
             * Return false for the Add tile so it behaves
             * normally and does not consume long press.
             */
            if (item.isAddAction) {

                false

            } else {

                onLongClick(
                    item
                )

                true
            }
        }
    }

    // ============================================================
    // ITEM COUNT
    // ============================================================

    override fun getItemCount(): Int =
        items.size
}