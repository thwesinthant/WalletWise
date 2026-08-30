package com.example.walletwise.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.entity.AccountBalance
import java.text.NumberFormat
import java.util.Locale

class AccountAdapter(
    private var accounts: List<AccountBalance> = emptyList(),
    private var currency: String = "MMK",
    private val onMenuClick: (AccountBalance) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    class AccountViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val accountIcon: TextView =
            itemView.findViewById(
                R.id.tvAccountIcon
            )

        val accountName: TextView =
            itemView.findViewById(
                R.id.tvAccountName
            )

        val accountBalance: TextView =
            itemView.findViewById(
                R.id.tvAccountBalance
            )

        val accountMenu: TextView =
            itemView.findViewById(
                R.id.btnAccountMenu
            )
    }


    // =========================================================
    // CREATE VIEW HOLDER
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AccountViewHolder {

        val view =
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.item_account,
                parent,
                false
            )

        return AccountViewHolder(view)
    }


    // =========================================================
    // BIND
    // =========================================================

    override fun onBindViewHolder(
        holder: AccountViewHolder,
        position: Int
    ) {

        val account =
            accounts[position]


        holder.accountName.text =
            account.name


        holder.accountBalance.text =
            formatCurrency(
                account.currentBalance
            )


        // -----------------------------------------------------
        // ACCOUNT ICON
        // -----------------------------------------------------

        holder.accountIcon.text =
            getAccountIcon(
                account.name
            )


        // -----------------------------------------------------
        // MENU
        // -----------------------------------------------------

        holder.accountMenu.setOnClickListener {

            onMenuClick(
                account
            )
        }
    }


    // =========================================================
    // ITEM COUNT
    // =========================================================

    override fun getItemCount(): Int =
        accounts.size


    // =========================================================
    // UPDATE LIST
    // =========================================================

    fun updateList(
        newAccounts: List<AccountBalance>
    ) {

        accounts =
            newAccounts

        notifyDataSetChanged()
    }


    // =========================================================
    // UPDATE CURRENCY
    // =========================================================

    fun updateCurrency(
        newCurrency: String
    ) {

        currency =
            newCurrency.ifBlank {
                "MMK"
            }

        notifyDataSetChanged()
    }


    // =========================================================
    // FORMAT CURRENCY
    // =========================================================

    private fun formatCurrency(
        amount: Double
    ): String {

        return "$currency ${
            NumberFormat
                .getNumberInstance(
                    Locale.US
                )
                .apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                .format(amount)
        }"
    }


    // =========================================================
    // ACCOUNT ICON
    // =========================================================

    private fun getAccountIcon(
        name: String
    ): String {

        val lowerName =
            name.lowercase()

        return when {

            // 💵 Cash
            lowerName.contains("cash") ||
                    lowerName.contains("kyat") ||
                    lowerName.contains("mmk") ->
                "💵"


            // 📱 Myanmar Mobile Wallets
            lowerName.contains("wave") ||
                    lowerName.contains("wave money") ||
                    lowerName.contains("kbzpay") ||
                    lowerName.contains("kbz pay") ||
                    lowerName.contains("aya pay") ||
                    lowerName.contains("ayapay") ||
                    lowerName.contains("cb pay") ||
                    lowerName.contains("cbpay") ||
                    lowerName.contains("mpt money") ||
                    lowerName.contains("truemoney") ||
                    lowerName.contains("true money") ||
                    lowerName.contains("ok dollar") ||
                    lowerName.contains("ok$") ->
                "📱"


            // 🏦 Myanmar Banks
            lowerName.contains("kbz") ||
                    lowerName.contains("kanbawza") ||
                    lowerName.contains("aya") ||
                    lowerName.contains("ayabank") ||
                    lowerName.contains("yoma") ||
                    lowerName.contains("cb bank") ||
                    lowerName.contains("co-operative bank") ||
                    lowerName.contains("mab") ||
                    lowerName.contains("myanmar apex") ||
                    lowerName.contains("uab") ||
                    lowerName.contains("united amalgamated") ||
                    lowerName.contains("a bank") ||
                    lowerName.contains("abank") ||
                    lowerName.contains("agriculture bank") ||
                    lowerName.contains("myanma economic") ||
                    lowerName.contains("mftb") ||
                    lowerName.contains("myanma foreign trade") ||
                    lowerName.contains("mcb") ||
                    lowerName.contains("myanmar citizens") ||
                    lowerName.contains("shwe") ||
                    lowerName.contains("global treasure") ||
                    lowerName.contains("gtb") ||
                    lowerName.contains("first private") ||
                    lowerName.contains("fpb") ->
                "🏦"


            // 🌍 International Digital Wallets / Payment Services
            lowerName.contains("paypal") ||
                    lowerName.contains("payoneer") ||
                    lowerName.contains("wise") ||
                    lowerName.contains("revolut") ||
                    lowerName.contains("skrill") ||
                    lowerName.contains("neteller") ||
                    lowerName.contains("alipay") ||
                    lowerName.contains("wechat pay") ||
                    lowerName.contains("google pay") ||
                    lowerName.contains("apple pay") ||
                    lowerName.contains("samsung pay") ->
                "📱"


            // 💳 Credit / Debit Cards
            lowerName.contains("visa") ||
                    lowerName.contains("mastercard") ||
                    lowerName.contains("master card") ||
                    lowerName.contains("amex") ||
                    lowerName.contains("american express") ||
                    lowerName.contains("jcb") ||
                    lowerName.contains("unionpay") ||
                    lowerName.contains("debit card") ||
                    lowerName.contains("credit card") ||
                    lowerName.contains("card") ->
                "💳"


            // 👛 Generic Wallet
            lowerName.contains("wallet") ||
                    lowerName.contains("e-wallet") ||
                    lowerName.contains("ewallet") ->
                "👛"


            // 🏦 Generic Bank
            lowerName.contains("bank") ||
                    lowerName.contains("account") ->
                "🏦"


            else ->
                "💰"
        }
    }
}