package com.example.walletwise

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.Goal
import com.example.walletwise.GoalAdapter

class GoalsActivity : AppCompatActivity() {

    private lateinit var adapter: GoalAdapter
    private val goals = mutableListOf(
        Goal("New Car", targetAmount = 8000, currentAmount = 4250),
        Goal("New Car", targetAmount = 8000, currentAmount = 4250),
        Goal("New Car", targetAmount = 8000, currentAmount = 4250)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        val rvGoals = findViewById<RecyclerView>(R.id.rvGoals)
        adapter = GoalAdapter(goals) { goal ->
            showAddMoneyDialog(goal)
        }
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = adapter

        findViewById<android.view.View>(R.id.btnAddGoal).setOnClickListener {
            showAddGoalDialog()
        }
    }

    private fun showAddGoalDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_goal)
        // Let the layout's own rounded-corner background show through
        // instead of the default dialog window background.
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val etTitle = dialog.findViewById<EditText>(R.id.etGoalTitle)
        val etTarget = dialog.findViewById<EditText>(R.id.etGoalTarget)
        val btnAdd = dialog.findViewById<TextView>(R.id.btnAdd)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        btnAdd.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val target = etTarget.text.toString().trim().toIntOrNull()

            if (title.isEmpty() || target == null || target <= 0) {
                Toast.makeText(this, "Enter a valid name and target amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            adapter.addGoal(Goal(title = title, targetAmount = target, currentAmount = 0))
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        setDialogWidth(dialog)
    }

    private fun showAddMoneyDialog(goal: Goal) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_money)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvAddMoneyTitle)
        val etAmount = dialog.findViewById<EditText>(R.id.etAddAmount)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btnAddMoneyConfirm)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnAddMoneyCancel)

        tvTitle.text = "Add Money to ${goal.title}"

        btnConfirm.setOnClickListener {
            val amountToAdd = etAmount.text.toString().trim().toIntOrNull()

            if (amountToAdd == null || amountToAdd <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cap so the saved amount never exceeds the target.
            goal.currentAmount = (goal.currentAmount + amountToAdd).coerceAtMost(goal.targetAmount)
            adapter.updateGoal(goal)

            if (goal.currentAmount >= goal.targetAmount) {
                Toast.makeText(this, "${goal.title} goal reached! 🎉", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        setDialogWidth(dialog)
    }

    /** Dialog windows default to WRAP_CONTENT width regardless of the XML root's
     *  layout_width, so the size has to be set explicitly after show(). */
    private fun setDialogWidth(dialog: Dialog, marginDp: Int = 24) {
        val marginPx = (marginDp * resources.displayMetrics.density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        dialog.window?.setLayout(
            screenWidth - (marginPx * 2),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}