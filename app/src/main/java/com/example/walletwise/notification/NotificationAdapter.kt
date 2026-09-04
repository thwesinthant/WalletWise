package com.example.walletwise.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.walletwise.R
import com.example.walletwise.entity.Notification


class NotificationAdapter(

    private var notifications: List<Notification>,

    private val onMarkAsReadClick:
        (Notification) -> Unit,

    private val onDeleteClick:
        (Notification) -> Unit

) : RecyclerView.Adapter<
        NotificationAdapter.NotificationViewHolder
        >() {


    // ============================================================
    // VIEW HOLDER
    // ============================================================

    class NotificationViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {


        val tvTitle: TextView =
            view.findViewById(
                R.id.tvTitle
            )


        val tvMessage: TextView =
            view.findViewById(
                R.id.tvMessage
            )


        val tvTimeAgo: TextView =
            view.findViewById(
                R.id.tvTimeAgo
            )


        val imgIcon: ImageView =
            view.findViewById(
                R.id.imgIcon
            )


        val tvMarkAsRead: TextView =
            view.findViewById(
                R.id.tvMarkAsRead
            )


        val btnDelete: ImageView =
            view.findViewById(
                R.id.btnDelete
            )
    }


    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationViewHolder {


        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_notification,
                    parent,
                    false
                )


        return NotificationViewHolder(view)
    }


    // ============================================================
    // BIND VIEW HOLDER
    // ============================================================

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int
    ) {


        val item =
            notifications[position]


        // ========================================================
        // TITLE
        // ========================================================

        holder.tvTitle.text =
            item.title


        // ========================================================
        // MESSAGE
        // ========================================================

        holder.tvMessage.text =
            item.message


        // ========================================================
        // TIME AGO
        // ========================================================

        val createdTime =
            item.createdAt
                .toLongOrNull()
                ?: System.currentTimeMillis()


        holder.tvTimeAgo.text =
            getTimeAgo(createdTime)


        // ========================================================
        // NOTIFICATION TYPE
        // ========================================================

        val titleLower =
            item.title.lowercase()


        val typeUpper =
            item.type.uppercase()


        // ========================================================
        // ICON
        // ========================================================

        val iconResource =
            when {


                // INCOME

                titleLower.contains("income") ||
                        typeUpper == "INCOME" -> {

                    R.drawable.ic_notification_income
                }


                // EXPENSE

                titleLower.contains("expense") ||
                        typeUpper == "EXPENSE" -> {

                    R.drawable.ic_notification_expense
                }


                // LARGE TRANSACTION

                titleLower.contains("large") ||
                        typeUpper == "TRANSACTION" -> {

                    R.drawable.ic_notification_transaction
                }


                // BUDGET

                titleLower.contains("budget") ||
                        typeUpper == "BUDGET_ALERT" ||
                        typeUpper == "BUDGET_EXCEEDED" ||
                        typeUpper == "CATEGORY_BUDGET_EXCEEDED" -> {

                    R.drawable.ic_notification_budget
                }


                // BILL

                titleLower.contains("bill") ||
                        typeUpper == "BILL_DUE" -> {

                    R.drawable.ic_notification_bill
                }


                // SUMMARY

                titleLower.contains("summary") ||
                        typeUpper == "SUMMARY" -> {

                    R.drawable.ic_notification_summary
                }


                // GOAL

                titleLower.contains("goal") ||
                        typeUpper == "GOAL_REMINDER" ||
                        typeUpper == "GOAL_COMPLETED" -> {

                    R.drawable.ic_notification_goal
                }


                // DEFAULT

                else -> {

                    R.drawable.ic_notification_default
                }
            }


        holder.imgIcon.setImageResource(
            iconResource
        )


        // ========================================================
        // READ STATE
        // ========================================================

        if (item.isRead) {

            holder.itemView.alpha =
                0.45f

            holder.tvMarkAsRead.visibility =
                View.GONE

        } else {

            holder.itemView.alpha =
                1.0f

            holder.tvMarkAsRead.visibility =
                View.VISIBLE
        }


        // ========================================================
        // CLICK NOTIFICATION
        // ========================================================

        holder.itemView.setOnClickListener {

            if (!item.isRead) {

                onMarkAsReadClick(item)
            }
        }


        // ========================================================
        // MARK AS READ
        // ========================================================

        holder.tvMarkAsRead.setOnClickListener {

            if (!item.isRead) {

                onMarkAsReadClick(item)
            }
        }


        // ========================================================
        // DELETE NOTIFICATION
        // ========================================================

        holder.btnDelete.setOnClickListener {

            onDeleteClick(item)
        }
    }


    // ============================================================
    // ITEM COUNT
    // ============================================================

    override fun getItemCount(): Int {

        return notifications.size
    }


    // ============================================================
    // UPDATE LIST
    // ============================================================

    fun updateList(
        newList: List<Notification>
    ) {

        notifications =
            newList

        notifyDataSetChanged()
    }


    // ============================================================
    // TIME AGO
    // ============================================================

    private fun getTimeAgo(
        time: Long
    ): String {


        val now =
            System.currentTimeMillis()


        val diff =
            now - time


        val minute =
            60 * 1000L


        val hour =
            60 * minute


        val day =
            24 * hour


        val minutes =
            diff / minute


        val hours =
            diff / hour


        val days =
            diff / day


        return when {

            diff < minute -> {

                "Just now"
            }

            diff < hour -> {

                "${minutes}m"
            }

            hours == 1L -> {

                "1 hr"
            }

            hours < 24 -> {

                "$hours hr"
            }

            days == 1L -> {

                "1 day"
            }

            else -> {

                "$days days"
            }
        }
    }
}