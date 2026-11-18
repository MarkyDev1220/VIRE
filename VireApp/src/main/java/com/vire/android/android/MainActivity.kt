package com.vire.android.android

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isLogout = intent.getBooleanExtra("is_logout", false)

        val currentUser = auth.currentUser
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedUid = prefs.getString("uid", null)

        if (!isLogout && (currentUser != null || savedUid != null)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setupTcgEntranceAnimation()
        setupInteractiveCardTilt()

        binding.loginSubmitButton.setOnClickListener {
            val emailOrUsername = binding.emailOrUsernameEditText.text.toString().trim()
            val password = binding.loginPasswordEditText.text.toString().trim()

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (emailOrUsername.contains("@")) {
                if (!Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
                    binding.emailOrUsernameEditText.error = "Enter a valid email"
                    return@setOnClickListener
                }
                performFirebaseLogin(emailOrUsername, password)
            } else {
                db.collection("users")
                    .whereEqualTo("usernameLowercase", emailOrUsername.lowercase())
                    .get()
                    .addOnSuccessListener { result ->
                        val matchedDoc = result.documents.firstOrNull()
                        val email = matchedDoc?.getString("email")
                        if (!email.isNullOrEmpty()) {
                            performFirebaseLogin(email, password)
                        } else {
                            db.collection("users")
                                .whereEqualTo("username", emailOrUsername)
                                .get()
                                .addOnSuccessListener { res2 ->
                                    val email2 = res2.documents.firstOrNull()?.getString("email")
                                    if (!email2.isNullOrEmpty()) {
                                        performFirebaseLogin(email2, password)
                                    } else {
                                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Login failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun setupTcgEntranceAnimation() {
        val density = resources.displayMetrics.density
        binding.loginCardContainer.cameraDistance = 10000 * density

        // Initial state
        binding.tcgLogoIcon.apply {
            alpha = 0f
            translationY = -150f
            scaleX = 0.3f
            scaleY = 0.3f
        }

        binding.welcomeTitle.apply {
            alpha = 0f
            translationY = -40f
        }

        binding.welcomeSubtitle.apply {
            alpha = 0f
            translationY = -30f
        }

        binding.instructionText.apply {
            alpha = 0f
        }

        binding.loginCardContainer.apply {
            alpha = 0f
            translationY = 300f
            rotationX = -35f
            scaleX = 0.85f
            scaleY = 0.85f
        }

        // 1. TCG Emblem Drop & Bounce
        binding.tcgLogoIcon.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator(1.4f))
            .setStartDelay(100)
            .start()

        // 2. Welcome Title & Subtitle Fade & Slide
        binding.welcomeTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(300)
            .start()

        binding.welcomeSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(400)
            .start()

        binding.instructionText.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(500)
            .start()

        // 3. TCG 3D Card Deal & Flip onto Board
        val cardAlpha = ObjectAnimator.ofFloat(binding.loginCardContainer, View.ALPHA, 0f, 1f)
        val cardTransY = ObjectAnimator.ofFloat(binding.loginCardContainer, View.TRANSLATION_Y, 300f, 0f)
        val cardRotX = ObjectAnimator.ofFloat(binding.loginCardContainer, View.ROTATION_X, -35f, 0f)
        val cardScaleX = ObjectAnimator.ofFloat(binding.loginCardContainer, View.SCALE_X, 0.85f, 1f)
        val cardScaleY = ObjectAnimator.ofFloat(binding.loginCardContainer, View.SCALE_Y, 0.85f, 1f)

        AnimatorSet().apply {
            playTogether(cardAlpha, cardTransY, cardRotX, cardScaleX, cardScaleY)
            duration = 750
            interpolator = DecelerateInterpolator(1.5f)
            startDelay = 500
            start()
        }
    }

    private fun setupInteractiveCardTilt() {
        val card = binding.loginCardContainer
        card.setOnTouchListener { view, event ->
            val width = view.width.toFloat()
            val height = view.height.toFloat()

            if (width <= 0 || height <= 0) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    val touchX = event.x.coerceIn(0f, width)
                    val touchY = event.y.coerceIn(0f, height)

                    // Calculate rotation angles (-10deg to +10deg) based on touch position relative to card center
                    val rotY = ((touchX - (width / 2f)) / (width / 2f)) * 10f
                    val rotX = -((touchY - (height / 2f)) / (height / 2f)) * 10f

                    view.animate()
                        .rotationX(rotX)
                        .rotationY(rotY)
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(100)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    view.animate()
                        .rotationX(0f)
                        .rotationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .rotationX(0f)
                        .rotationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
            false
        }
    }

    private fun performFirebaseLogin(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val username = doc.getString("username") ?: email.substringBefore("@")
                            saveLocalSession(uid, username, email)
                            Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        } else {
                            // Document does not exist - account was deleted
                            auth.signOut()
                            Toast.makeText(this, "This account has been deleted.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        saveLocalSession(uid, email.substringBefore("@"), email)
                        startActivity(Intent(this, HomeActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                val errorMsg = if (e is SecurityException || e.message?.contains("SecurityException", ignoreCase = true) == true ||
                    e.message?.contains("broker", ignoreCase = true) == true || e.message?.contains("Unknown calling package", ignoreCase = true) == true) {
                    "Google Play Services error: ${e.localizedMessage}. Please ensure Google Play Services is updated on your device/emulator."
                } else if (e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("unreachable host", ignoreCase = true) == true) {
                    "Network error: Unable to reach Firebase servers. Please check your internet or emulator network connection."
                } else {
                    "Login failed: ${e.message}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
    }

    private fun saveLocalSession(uid: String, username: String, email: String = "") {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("uid", uid)
            .putString("username", username)
            .putString("email", email)
            .apply()
    }
}
