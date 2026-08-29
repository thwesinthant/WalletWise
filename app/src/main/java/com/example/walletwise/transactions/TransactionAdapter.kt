package com.example.walletwise.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

class TransactionAdapter(
    private var rawList: List<Transaction> = emptyList(),
    private var currency: String = "MMK"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    // ============================================================
    // VIEW TYPES
    // ============================================================

    private val VIEW_TYPE_HEADER =
        0

    private val VIEW_TYPE_ITEM =
        1


    // ============================================================
    // CATEGORY MAP
    // ============================================================

    private var categoryMap:
            Map<String, CategoryEntity> =
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


    private var displayItems:
            List<ListItem> =
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
            newCurrency
                .ifBlank {
                    "MMK"
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
            rawList.groupBy {

                getGroupHeaderTitle(
                    it.createdAt
                )
            }


        for (
        (header, transactions)
        in grouped
        ) {

            items.add(
                ListItem.Header(
                    header
                )
            )


            transactions.forEach { transaction ->

                items.add(
                    ListItem.Item(
                        transaction
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

        return if (
            viewType == VIEW_TYPE_HEADER
        ) {

            val view =
                LayoutInflater
                    .from(
                        parent.context
                    )
                    .inflate(
                        R.layout.item_transaction_header,
                        parent,
                        false
                    )


            HeaderViewHolder(
                view
            )

        } else {

            val view =
                LayoutInflater
                    .from(
                        parent.context
                    )
                    .inflate(
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

                (
                        holder as HeaderViewHolder
                        ).tvHeaderTitle.text =
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


                // ------------------------------------------------
                // TITLE
                // ------------------------------------------------

                itemHolder
                    .tvTxnTitle
                    .text =
                    transaction.title


                // ------------------------------------------------
                // SUBTITLE
                // ------------------------------------------------

                itemHolder
                    .tvTxnSubtitle
                    .text =
                    transaction.note
                        ?: "${transaction.category} • ${transaction.paymentMethod}"


                // =================================================
                // CATEGORY ICON
                // ============================================================

                val category =
                    categoryMap[
                        transaction.category
                    ]


                if (
                    category != null
                ) {

                    itemHolder
                        .ivCategoryIcon
                        .setImageResource(
                            category.iconRes
                        )


                    itemHolder
                        .ivCategoryIcon
                        .setColorFilter(
                            category.tintColor
                        )

                } else {

                    val fallbackIcon =
                        if (
                            transaction.type ==
                            "INCOME"
                        ) {

                            R.drawable.ic_plus

                        } else {

                            R.drawable.ic_card
                        }


                    itemHolder
                        .ivCategoryIcon
                        .setImageResource(
                            fallbackIcon
                        )


                    itemHolder
                        .ivCategoryIcon
                        .clearColorFilter()
                }


                // =================================================
                // AMOUNT
                // ============================================================

                val formattedAmount =
                    String.format(
                        Locale.getDefault(),
                        "%,.2f",
                        transaction.amount
                    )


                if (
                    transaction.type ==
                    "INCOME"
                ) {

                    itemHolder
                        .tvTxnAmount
                        .text =
                        "$currency $formattedAmount"


                    itemHolder
                        .tvTxnAmount
                        .setTextColor(
                            ContextCompat.getColor(
                                holder.itemView.context,
                                R.color.income_green
                            )
                        )

                } else {

                    itemHolder
                        .tvTxnAmount
                        .text =
                        "-$currency $formattedAmount"


                    itemHolder
                        .tvTxnAmount
                        .setTextColor(
                            ContextCompat.getColor(
                                holder.itemView.context,
                                R.color.expense_red
                            )
                        )
                }
            }
        }
    }


    // ============================================================
    // ITEM COUNT
    // ============================================================

    override fun getItemCount(): Int {

        return displayItems.size
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
                it.label
            }

        notifyDataSetChanged()
    }


    // ============================================================
    // DATE HEADER
    // ============================================================

    private fun getGroupHeaderTitle(
        timeMillis: Long
    ): String {

        val itemCal =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        timeMillis
                }


        val nowCal =
            Calendar.getInstance()


        return if (
            isSameDay(
                itemCal,
                nowCal
            )
        ) {

            "Today"

        } else {

            nowCal.add(
                Calendar.DAY_OF_YEAR,
                -1
            )


            if (
                isSameDay(
                    itemCal,
                    nowCal
                )
            ) {

                "Yesterday"

            } else {

                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                ).format(
                    Date(
                        timeMillis
                    )
                )
            }
        }
    }


    // ============================================================
    // SAME DAY
    // ============================================================

    private fun isSameDay(
        cal1: Calendar,
        cal2: Calendar
    ): Boolean {

        return (
                cal1.get(
                    Calendar.YEAR
                ) ==
                        cal2.get(
                            Calendar.YEAR
                        )
                        &&
                        cal1.get(
                            Calendar.DAY_OF_YEAR
                        ) ==
                        cal2.get(
                            Calendar.DAY_OF_YEAR
                        )
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

        val tvHeaderTitle:
                TextView =
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

        val ivCategoryIcon:
                ImageView =
            view.findViewById(
                R.id.tvCategoryIcon
            )


        val tvTxnTitle:
                TextView =
            view.findViewById(
                R.id.tvTxnTitle
            )


        val tvTxnSubtitle:
                TextView =
            view.findViewById(
                R.id.tvTxnSubtitle
            )


        val tvTxnAmount:
                TextView =
            view.findViewById(
                R.id.tvTxnAmount
            )
    }
}