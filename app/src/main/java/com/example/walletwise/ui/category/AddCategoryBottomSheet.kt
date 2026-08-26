package com.example.walletwise.ui.category

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.ui.category.IconPickerDialogFragment

class AddCategoryBottomSheet(
    private val onSave: (Category) -> Unit
) : DialogFragment() {

    private val iconOptions = listOf(
        R.drawable.ic_category_groceries,
        R.drawable.ic_category_travel,
        R.drawable.ic_category_car,
        R.drawable.ic_category_home,
        R.drawable.ic_category_insurances,
        R.drawable.ic_category_education,
        R.drawable.ic_category_marketing,
        R.drawable.ic_category_shopping,
        R.drawable.ic_category_internet,
        R.drawable.ic_category_water,
        R.drawable.ic_category_rent,
        R.drawable.ic_category_gym,
        R.drawable.ic_category_subscription,
        R.drawable.ic_category_vacation,
        R.drawable.ic_category_other,
        R.drawable.ic_cat_food,
        R.drawable.ic_cat_health,
        R.drawable.ic_cat_fitness,
        R.drawable.ic_cat_entertainment,
        R.drawable.ic_cat_gaming,
        R.drawable.ic_cat_pets,
        R.drawable.ic_cat_kids,
        R.drawable.ic_cat_fuel,
        R.drawable.ic_cat_pharmacy,
        R.drawable.ic_cat_clothing,
        R.drawable.ic_cat_beauty,
        R.drawable.ic_cat_bar,
        R.drawable.ic_cat_coffee,
        R.drawable.ic_cat_phone,
        R.drawable.ic_cat_utilities,
        R.drawable.ic_cat_repair,
        R.drawable.ic_cat_savings,
        R.drawable.ic_cat_creditcard,
        R.drawable.ic_cat_gifts,
        R.drawable.ic_cat_celebration,
        R.drawable.ic_cat_beach
    )

    private val stripIconCount = 4

    private val colorOptions = listOf(
        0xFF3A9E5F.toInt(),
        0xFF3D7BD9.toInt(),
        0xFFD9536F.toInt(),
        0xFF8A5FD9.toInt(),
        0xFFD97A3D.toInt()
    )

    private val colorBgMap = mapOf(
        0xFF3A9E5F.toInt() to 0xFFE3F5E9.toInt(),
        0xFF3D7BD9.toInt() to 0xFFE4EEFC.toInt(),
        0xFFD9536F.toInt() to 0xFFFBE5E9.toInt(),
        0xFF8A5FD9.toInt() to 0xFFEEE7FB.toInt(),
        0xFFD97A3D.toInt() to 0xFFFBEBE0.toInt()
    )

    private var selectedIcon = iconOptions.first()
    private var selectedColor = colorOptions.first()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_add_category, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val previewTile = view.findViewById<FrameLayout>(R.id.previewTile)
        val previewIcon = view.findViewById<ImageView>(R.id.previewIcon)
        val nameInput = view.findViewById<EditText>(R.id.categoryNameInput)
        val iconRecyclerView = view.findViewById<RecyclerView>(R.id.iconOptionsRecyclerView)
        val colorRecyclerView = view.findViewById<RecyclerView>(R.id.colorSwatchRecyclerView)
        val saveButton = view.findViewById<View>(R.id.saveCategoryButton)

        fun updatePreview() {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorBgMap[selectedColor] ?: selectedColor)
            }
            previewTile.background = bg
            previewIcon.setImageResource(selectedIcon)
            previewIcon.setColorFilter(selectedColor)
        }
        updatePreview()

        val stripIcons = iconOptions.take(stripIconCount)

        iconRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        iconRecyclerView.adapter = IconOptionAdapter(
            icons = stripIcons,
            moreIconRes = R.drawable.ic_plus,
            onSelected = { icon ->
                selectedIcon = icon
                updatePreview()
            },
            onMoreClicked = {
                IconPickerDialogFragment(iconOptions, selectedIcon) { picked ->
                    selectedIcon = picked
                    updatePreview()
                }.show(childFragmentManager, "IconPicker")
            }
        )

        colorRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        colorRecyclerView.adapter = ColorSwatchAdapter(colorOptions) { color ->
            selectedColor = color
            updatePreview()
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onSave(
                Category(
                    label = name,
                    iconRes = selectedIcon,
                    tintColor = selectedColor,
                    bgColor = colorBgMap[selectedColor] ?: selectedColor
                )
            )
            dismiss()
        }
    }
}