package com.example.walletwise.category

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.walletwise.R

/**
 * Small modal shown when a category tile is tapped, letting the user edit or delete it.
 */
class CategoryActionsBottomSheet(
    private val categoryLabel: String,
    private val onEdit: () -> Unit,
    private val onDelete: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_category_actions, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(android.view.Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.categoryActionsTitle).text = categoryLabel

        view.findViewById<View>(R.id.editCategoryOption).setOnClickListener {
            onEdit()
            dismiss()
        }
        view.findViewById<View>(R.id.deleteCategoryOption).setOnClickListener {
            onDelete()
            dismiss()
        }
    }
}
