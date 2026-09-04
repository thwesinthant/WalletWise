package com.example.walletwise.goal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.example.walletwise.R
import com.example.walletwise.entity.Goal

import java.text.NumberFormat
import java.util.Locale


class GoalAdapter(
    private val currency: String,
    private val onPiggyBankClick: (Goal) -> Unit,
    private val onDeleteClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {


    private val goals =
        mutableListOf<Goal>()


    // ============================================================
    // VIEW HOLDER
    // ============================================================

    inner class GoalViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {


        val tvGoalTitle: TextView =
            itemView.findViewById(
                R.id.tvGoalTitle
            )


        val tvGoalStatus: TextView =
            itemView.findViewById(
                R.id.tvGoalStatus
            )


        val tvGoalTarget: TextView =
            itemView.findViewById(
                R.id.tvGoalTarget
            )


        val tvGoalAmounts: TextView =
            itemView.findViewById(
                R.id.tvGoalAmounts
            )


        val tvGoalRemaining: TextView =
            itemView.findViewById(
                R.id.tvGoalRemaining
            )


        val tvGoalPercent: TextView =
            itemView.findViewById(
                R.id.tvGoalPercent
            )


        val progressGoal: ProgressBar =
            itemView.findViewById(
                R.id.progressGoal
            )


        val btnPiggyBank: View =
            itemView.findViewById(
                R.id.btnPiggyBank
            )


        val btnDeleteGoal: View =
            itemView.findViewById(
                R.id.btnDeleteGoal
            )
    }


    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GoalViewHolder {


        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_goal_card,
                    parent,
                    false
                )


        return GoalViewHolder(view)
    }


    // ============================================================
    // BIND
    // ============================================================

    override fun onBindViewHolder(
        holder: GoalViewHolder,
        position: Int
    ) {


        val goal =
            goals[position]


        // ========================================================
        // TITLE
        // ========================================================

        holder.tvGoalTitle.text =
            goal.title


        // ========================================================
        // TARGET
        // ========================================================

        holder.tvGoalTarget.text =
            "Target: ${formatMoney(goal.targetAmount)}"


        // ========================================================
        // AMOUNTS
        // ========================================================

        holder.tvGoalAmounts.text =
            "${formatMoney(goal.currentAmount)} / " +
                    formatMoney(goal.targetAmount)


        // ========================================================
        // REMAINING
        // ========================================================

        val remainingAmount =
            (
                    goal.targetAmount -
                            goal.currentAmount
                    )
                .coerceAtLeast(0.0)


        holder.tvGoalRemaining.text =
            "${formatMoney(remainingAmount)} remaining"


        // ========================================================
        // PROGRESS
        // ========================================================

        holder.tvGoalPercent.text =
            "${goal.progressPercent}%"


        holder.progressGoal.max =
            100


        holder.progressGoal.progress =
            goal.progressPercent


        // ========================================================
        // GOAL STATUS
        // ========================================================

        val isCompleted =
            goal.currentAmount >=
                    goal.targetAmount


        if (isCompleted) {

            // ==============================================
            // COMPLETED
            // ==============================================

            holder.tvGoalStatus.text =
                "COMPLETED"


            holder.tvGoalStatus.setTextColor(
                holder.itemView.context.getColor(
                    R.color.neutral_0
                )
            )


            holder.tvGoalStatus.setBackgroundResource(
                R.drawable.bg_goal_status_completed
            )


            // No need to add more money
            holder.btnPiggyBank.visibility =
                View.GONE


            // Remaining becomes zero
            holder.tvGoalRemaining.text =
                "Goal completed 🎉"


        } else {

            // ==============================================
            // ACTIVE
            // ==============================================

            holder.tvGoalStatus.text =
                "ACTIVE"


            holder.tvGoalStatus.setTextColor(
                holder.itemView.context.getColor(
                    R.color.primary_600
                )
            )


            holder.tvGoalStatus.setBackgroundResource(
                R.drawable.bg_goal_status_active
            )


            // Allow adding money
            holder.btnPiggyBank.visibility =
                View.VISIBLE
        }


        // ========================================================
        // ADD MONEY
        // ========================================================

        holder.btnPiggyBank.setOnClickListener {

            onPiggyBankClick(
                goal
            )
        }


        // ========================================================
        // DELETE
        // ========================================================

        holder.btnDeleteGoal.setOnClickListener {

            onDeleteClick(
                goal
            )
        }
    }


    // ============================================================
    // ITEM COUNT
    // ============================================================

    override fun getItemCount(): Int =
        goals.size


    // ============================================================
    // UPDATE LIST
    // ============================================================

    fun submitList(
        newGoals: List<Goal>
    ) {


        val diffCallback =
            object : DiffUtil.Callback() {


                override fun getOldListSize(): Int =
                    goals.size


                override fun getNewListSize(): Int =
                    newGoals.size


                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean =

                    goals[
                        oldItemPosition
                    ].goalId ==

                            newGoals[
                                newItemPosition
                            ].goalId


                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean =

                    goals[
                        oldItemPosition
                    ] ==

                            newGoals[
                                newItemPosition
                            ]
            }


        val diffResult =
            DiffUtil.calculateDiff(
                diffCallback
            )


        goals.clear()

        goals.addAll(
            newGoals
        )


        diffResult.dispatchUpdatesTo(
            this
        )
    }


    // ============================================================
    // FORMAT MONEY
    // ============================================================

    private fun formatMoney(
        amount: Double
    ): String {


        val formatter =
            NumberFormat.getNumberInstance(
                Locale.US
            )


        formatter.maximumFractionDigits =
            0


        formatter.minimumFractionDigits =
            0


        return "${formatter.format(amount)} $currency"
    }
}