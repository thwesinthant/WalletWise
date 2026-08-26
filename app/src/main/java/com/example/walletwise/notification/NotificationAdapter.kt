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
    private val onMarkAsReadClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimeAgo: TextView = view.findViewById(R.id.tvTimeAgo)
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
        val tvMarkAsRead: TextView = view.findViewById(R.id.tvMarkAsRead)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]
        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message

        // အချိန်တွက်ချက်ပြသခြင်း
        val createdTime = item.createdAt.toLongOrNull() ?: System.currentTimeMillis()
        holder.tvTimeAgo.text = getTimeAgo(createdTime)

        val titleLower = item.title.lowercase()
        val typeUpper = item.type.uppercase()

        // Notification အမျိုးအစားအလိုက် Icon သတ်မှတ်ခြင်း
        when {
            // ၁။ Income သီးသန့် Icon (ဝင်ငွေပြ Icon)
            titleLower.contains("income") || typeUpper == "INCOME" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_input_add)
            }
            // ၂။ Large Transaction (သတိပေး Alert Icon)
            titleLower.contains("large") || typeUpper == "TRANSACTION" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            }
            // ၃။ Expense (အသုံးစရိတ် Icon)
            titleLower.contains("expense") || typeUpper == "EXPENSE" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_menu_send)
            }
            // ၄။ Budget Alert
            titleLower.contains("budget") || typeUpper == "BUDGET_ALERT" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            }
            // ၅။ Bill Due
            titleLower.contains("bill") || typeUpper == "BILL_DUE" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_menu_agenda)
            }
            // ၆။ Summary
            titleLower.contains("summary") || typeUpper == "SUMMARY" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            }
            // ၇။ Goal Reminder
            titleLower.contains("goal") || typeUpper == "GOAL_REMINDER" -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_menu_compass)
            }
            else -> {
                holder.imgIcon.setImageResource(android.R.drawable.ic_popup_reminder)
            }
        }

        // ဖတ်ပြီးသား (isRead = true) ဖြစ်ပါက မှိန်ပြပြီး Mark as read ခလုတ်ကို ဖျောက်ခြင်း
        if (item.isRead) {
            holder.itemView.alpha = 0.4f
            holder.tvMarkAsRead.visibility = View.GONE
        } else {
            holder.itemView.alpha = 1.0f
            holder.tvMarkAsRead.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            if (!item.isRead) onMarkAsReadClick(item)
        }

        holder.tvMarkAsRead.setOnClickListener {
            if (!item.isRead) onMarkAsReadClick(item)
        }
    }

    override fun getItemCount(): Int = notifications.size
    fun updateList(newList: List<Notification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    private fun getTimeAgo(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time

        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        val minutes = diff / minute
        val hours = diff / hour
        val days = diff / day

        return when {
            diff < minute -> "Just now"
            diff < hour -> "${minutes}m"
            hours == 1L -> "1 hr"
            hours < 24 -> "$hours hr"
            days == 1L -> "1 dy"
            else -> "$days dy"
        }
    }
}