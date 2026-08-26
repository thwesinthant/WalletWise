package com.example.walletwise.goal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R


class GoalAdapter(
    private val goals: MutableList<Goal>,
    private val onPiggyBankClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvGoalTitle)
        val tvTarget: TextView = itemView.findViewById(R.id.tvGoalTarget)
        val tvAmounts: TextView = itemView.findViewById(R.id.tvGoalAmounts)
        val tvPercent: TextView = itemView.findViewById(R.id.tvGoalPercent)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressGoal)
        val btnPiggyBank: ImageButton = itemView.findViewById(R.id.btnPiggyBank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_card, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvTitle.text = goal.title
        holder.tvTarget.text = "${goal.targetAmount} MMK"
        holder.tvAmounts.text = "${goal.currentAmount} MMK / ${goal.targetAmount} MMK"
        holder.tvPercent.text = "${goal.progressPercent}%"
        holder.progressBar.progress = goal.progressPercent

        holder.btnPiggyBank.setOnClickListener { onPiggyBankClick(goal) }
    }

    override fun getItemCount(): Int = goals.size

    /** Adds a new goal to the top of the list and refreshes the RecyclerView. */
    fun addGoal(goal: Goal) {
        goals.add(0, goal)
        notifyItemInserted(0)
    }

    /** Call after mutating a goal's currentAmount, to refresh just that card. */
    fun updateGoal(goal: Goal) {
        val index = goals.indexOf(goal)
        if (index != -1) {
            notifyItemChanged(index)
        }
    }
}