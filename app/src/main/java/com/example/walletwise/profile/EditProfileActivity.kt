package com.example.walletwise.profile

import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.User
import com.example.walletwise.util.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentUser: User? = null
    private var isPasswordVisible: Boolean = false
    private var userId: Int = -1

    private var selectedImageUri: Uri? = null

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath  // ဒါကို DB ထဲ save လုပ်မယ်
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // ၀။ Intent ကနေ userId ကို ဆွဲယူခြင်း
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        etEmail.isFocusable = false
        etEmail.isClickable = false
        etEmail.isCursorVisible = false
        val etCurrency = findViewById<EditText>(R.id.etCurrency)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSend)

        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        val btnCamera = findViewById<ImageView>(R.id.btnCamera)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)

        // Password field ကို hash string နဲ့ prefill မလုပ်တော့ဘဲ placeholder ပြထားခြင်း
        etPassword.hint = "Leave blank to keep current password"

        btnBack?.setOnClickListener { finish() }

        // Gallery မှ ဓာတ်ပုံရွေးချယ်ရန် Picker
        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                ivProfile.imageTintList = null
                ivProfile.setImageURI(it)
            }
        }

        // ကင်မရာ icon သို့မဟုတ် Profile ပုံ နှိပ်ပါက Gallery ပွင့်မည်
        btnCamera?.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        ivProfile?.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // ၂။ Password မျက်လုံး Icon နှိပ်လျှင် Toggle ပြုလုပ်ခြင်း
        ivTogglePassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        database = AppDatabase.getDatabase(this)

        // ၃။ User Data ဆွဲယူပြသခြင်း
        lifecycleScope.launch {
            database.userDao().getUserById(userId).collect { user ->
                user?.let {
                    currentUser = it
                    withContext(Dispatchers.Main) {
                        etName.setText(it.fullName)
                        etEmail.setText(it.email)
                        etCurrency.setText(it.currency)

                        // Load existing profile photo
                        it.profileImage?.let { path ->
                            val imgFile = File(path)
                            if (imgFile.exists()) {
                                ivProfile.imageTintList = null
                                ivProfile.setImageURI(Uri.fromFile(imgFile))
                            }
                        }
                    }
                }
            }
        }

        // ၄။ Update ပြုလုပ်ခြင်း
        btnSave?.setOnClickListener {
            val updatedName = etName.text.toString().trim()
            val updatedEmail = etEmail.text.toString().trim()
            val updatedCurrency = etCurrency.text.toString().trim()
            val updatedPassword = etPassword.text.toString().trim()

            if (updatedName.isEmpty() || updatedEmail.isEmpty()) {
                Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                currentUser?.let { user ->
                    val newPasswordHash = if (updatedPassword.isNotEmpty()) {
                        PasswordUtils.hash(updatedPassword)
                    } else {
                        user.password
                    }

                    // သစ်ရွေးထားရင် internal storage ထဲ copy ကူးမယ်၊ မရွေးထားရင် ဟောင်းကိုပဲ ဆက်သုံးမယ်
                    val newImagePath = selectedImageUri?.let { uri ->
                        withContext(Dispatchers.IO) { saveImageToInternalStorage(uri) }
                    } ?: user.profileImage

                    val updatedUser = user.copy(
                        fullName = updatedName,
                        email = updatedEmail,
                        currency = updatedCurrency.ifEmpty { user.currency },
                        password = newPasswordHash,
                        profileImage = newImagePath
                    )

                    withContext(Dispatchers.IO) {
                        database.userDao().updateUser(updatedUser)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
}