package com.example.walletwise.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Color
import android.view.MenuItem
class TransactionAdapter(

    private var rawList: List<Transaction> = emptyList(),

    private var currency: String = "MMK",

    private val onEditClick: ((Transaction) -> Unit)? = null,

    private val onDeleteClick: ((Transaction) -> Unit)? = null

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    // ============================================================
    // VIEW TYPES
    // ============================================================

    companion object {

        private const val VIEW_TYPE_HEADER = 0

        private const val VIEW_TYPE_ITEM = 1

        private const val MENU_EDIT = 1001

        private const val MENU_DELETE = 1002
    }


    // ============================================================
    // CATEGORY MAP
    // ============================================================

    private var categoryMap: Map<Long, CategoryEntity> =
        emptyMap()


    // ============================================================
    // DISPLAY ITEMS
    // ============================================================

    private sealed class ListItem {

        data class Header(
            val title: String
        ) : ListItem()

        data class Item(
            val transaction: Transaction
        ) : ListItem()
    }


    private var displayItems: List<ListItem> =
        emptyList()


    init {

        buildDisplayItems()
    }


    // ============================================================
    // UPDATE CURRENCY
    // ============================================================

    fun updateCurrency(
        newCurrency: String
    ) {

        currency =
            newCurrency.ifBlank {
                "MMK"
            }

        notifyDataSetChanged()
    }


    // ============================================================
    // UPDATE TRANSACTIONS
    // ============================================================

    fun updateList(
        newList: List<Transaction>
    ) {

        rawList =
            newList

        buildDisplayItems()

        notifyDataSetChanged()
    }


    // ============================================================
    // UPDATE CATEGORIES
    // ============================================================

    fun updateCategories(
        categories: List<CategoryEntity>
    ) {

        categoryMap =
            categories.associateBy {
                it.id
            }

        notifyDataSetChanged()
    }


    // ============================================================
    // BUILD DISPLAY ITEMS
    // ============================================================

    private fun buildDisplayItems() {

        val items =
            mutableListOf<ListItem>()

        val grouped =
            rawList.groupBy { transaction ->

                getGroupHeaderTitle(
                    transaction.createdAt
                )
            }

        grouped.forEach { (header, transactions) ->

            items.add(
                ListItem.Header(
                    title = header
                )
            )

            transactions.forEach { transaction ->

                items.add(
                    ListItem.Item(
                        transaction = transaction
                    )
                )
            }
        }

        displayItems =
            items
    }


    // ============================================================
    // VIEW TYPE
    // ============================================================

    override fun getItemViewType(
        position: Int
    ): Int {

        return when (
            displayItems[position]
        ) {

            is ListItem.Header ->
                VIEW_TYPE_HEADER

            is ListItem.Item ->
                VIEW_TYPE_ITEM
        }
    }


    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater =
            LayoutInflater.from(
                parent.context
            )

        return if (
            viewType == VIEW_TYPE_HEADER
        ) {

            val view =
                inflater.inflate(
                    R.layout.item_transaction_header,
                    parent,
                    false
                )

            HeaderViewHolder(
                view
            )

        } else {

            val view =
                inflater.inflate(
                    R.layout.item_transaction_row,
                    parent,
                    false
                )

            TransactionViewHolder(
                view
            )
        }
    }


    // ============================================================
    // BIND VIEW HOLDER
    // ============================================================

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (
            val item =
                displayItems[position]
        ) {


            // ====================================================
            // HEADER
            // ====================================================

            is ListItem.Header -> {

                val headerHolder =
                    holder as HeaderViewHolder

                headerHolder
                    .tvHeaderTitle
                    .text =
                    item.title
            }


            // ====================================================
            // TRANSACTION
            // ====================================================

            is ListItem.Item -> {

                val transaction =
                    item.transaction

                val itemHolder =
                    holder as TransactionViewHolder

                bindTransaction(
                    itemHolder,
                    transaction
                )
            }
        }
    }


    // ============================================================
    // BIND TRANSACTION
    // ============================================================

    private fun bindTransaction(
        holder: TransactionViewHolder,
        transaction: Transaction
    ) {


        // =========================================================
        // CATEGORY
        // =========================================================

        val category =
            transaction.categoryId?.let { categoryId ->

                categoryMap[
                    categoryId
                ]
            }


        // =========================================================
        // TITLE
        // =========================================================

        holder
            .tvTxnTitle
            .text =
            transaction.title


        // =========================================================
        // SUBTITLE
        // =========================================================

        holder
            .tvTxnSubtitle
            .text =
            transaction.note
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: category
                    ?.label
                        ?: "No category"


        // =========================================================
        // CATEGORY ICON
        // =========================================================

        if (
            category != null
        ) {

            val iconResId =
                getDrawableResourceId(
                    holder.itemView.context,
                    category.iconName
                )

            holder
                .ivCategoryIcon
                .setImageResource(
                    iconResId
                )

            holder
                .ivCategoryIcon
                .setColorFilter(
                    category.tintColor
                )

        } else {

            val fallbackIcon =
                if (
                    transaction.type == "INCOME"
                ) {

                    R.drawable.ic_plus

                } else {

                    R.drawable.ic_card
                }

            holder
                .ivCategoryIcon
                .setImageResource(
                    fallbackIcon
                )

            holder
                .ivCategoryIcon
                .clearColorFilter()
        }


        // =========================================================
        // AMOUNT
        // =========================================================

        val formattedAmount =
            String.format(
                Locale.getDefault(),
                "%,.2f",
                transaction.amount
            )


        if (
            transaction.type == "INCOME"
        ) {

            holder
                .tvTxnAmount
                .text =
                "$currency $formattedAmount"

            holder
                .tvTxnAmount
                .setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.income_green
                    )
                )

        } else {

            holder
                .tvTxnAmount
                .text =
                "-$currency $formattedAmount"

            holder
                .tvTxnAmount
                .setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.expense_red
                    )
                )
        }


        // =========================================================
        // MORE MENU
        // =========================================================

        holder
            .btnTransactionMore
            .setOnClickListener { view ->

                showTransactionMenu(
                    anchor = view,
                    transaction = transaction
                )
            }
    }


    // ============================================================
    // CONVERT DRAWABLE NAME TO RESOURCE ID
    // ============================================================

    private fun getDrawableResourceId(
        context: android.content.Context,
        iconName: String
    ): Int {

        val resourceId =
            context.resources.getIdentifier(
                iconName,
                "drawable",
                context.packageName
            )

        return if (
            resourceId != 0
        ) {

            resourceId

        } else {

            // Safe fallback if drawable name does not exist.
            R.drawable.ic_card
        }
    }


    // ============================================================
    // SHOW TRANSACTION MENU
    // ============================================================

    private fun showTransactionMenu(
        anchor: View,
        transaction: Transaction
    ) {

        val popupMenu =
            PopupMenu(
                anchor.context,
                anchor
            )

        popupMenu.menu.add(
            0,
            MENU_EDIT,
            0,
            "Edit"
        )

        popupMenu.menu.add(
            0,
            MENU_DELETE,
            1,
            "Delete"
        )

        // Force popup menu text to use the app's dark text color
        for (i in 0 until popupMenu.menu.size()) {

            val menuItem =
                popupMenu.menu.getItem(i)

            val spannable =
                android.text.SpannableString(
                    menuItem.title
                )

            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    ContextCompat.getColor(
                        anchor.context,
                        R.color.neutral_900
                    )
                ),
                0,
                spannable.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            menuItem.title =
                spannable
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->

            when (menuItem.itemId) {

                MENU_EDIT -> {

                    onEditClick?.invoke(
                        transaction
                    )

                    true
                }

                MENU_DELETE -> {

                    onDeleteClick?.invoke(
                        transaction
                    )

                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }


    // ============================================================
    // ITEM COUNT
    // ============================================================

    override fun getItemCount(): Int {

        return displayItems.size
    }


    // ============================================================
    // DATE HEADER
    // ============================================================

    private fun getGroupHeaderTitle(
        timeMillis: Long
    ): String {

        val itemCalendar =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        timeMillis
                }


        val todayCalendar =
            Calendar.getInstance()


        if (
            isSameDay(
                itemCalendar,
                todayCalendar
            )
        ) {

            return "Today"
        }


        todayCalendar.add(
            Calendar.DAY_OF_YEAR,
            -1
        )


        if (
            isSameDay(
                itemCalendar,
                todayCalendar
            )
        ) {

            return "Yesterday"
        }


        return SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(
            Date(
                timeMillis
            )
        )
    }


    // ============================================================
    // SAME DAY
    // ============================================================

    private fun isSameDay(
        cal1: Calendar,
        cal2: Calendar
    ): Boolean {

        return cal1.get(
            Calendar.YEAR
        ) == cal2.get(
            Calendar.YEAR
        )
                &&
                cal1.get(
                    Calendar.DAY_OF_YEAR
                ) == cal2.get(
            Calendar.DAY_OF_YEAR
        )
    }


    // ============================================================
    // HEADER VIEW HOLDER
    // ============================================================

    class HeaderViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(
        view
    ) {

        val tvHeaderTitle: TextView =
            view.findViewById(
                R.id.tvHeaderTitle
            )
    }


    // ============================================================
    // TRANSACTION VIEW HOLDER
    // ============================================================

    class TransactionViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(
        view
    ) {

        val ivCategoryIcon: ImageView =
            view.findViewById(
                R.id.ivCategoryIcon
            )


        val tvTxnTitle: TextView =
            view.findViewById(
                R.id.tvTxnTitle
            )


        val tvTxnSubtitle: TextView =
            view.findViewById(
                R.id.tvTxnSubtitle
            )


        val tvTxnAmount: TextView =
            view.findViewById(
                R.id.tvTxnAmount
            )


        val btnTransactionMore: TextView =
            view.findViewById(
                R.id.btnTransactionMore
            )
    }
}