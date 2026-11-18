package com.vire.android.android

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
        if (!isLogout && currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val email = currentUser.email.orEmpty()
                        val username = doc.getString("username")
                            ?: email.substringBefore("@").ifBlank { "Gamer" }
                        db.collection(UserDirectory.COLLECTION)
                            .document(currentUser.uid)
                            .set(
                                UserDirectory.fromPrivateData(currentUser.uid, doc.data.orEmpty()),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .addOnSuccessListener {
                                saveLocalSession(currentUser.uid, username, email)
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Could not sync your searchable profile: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Your account profile is missing. Sign up with the same email to finish setup.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Could not load your profile from Firestore: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
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

            if (!Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
                binding.emailOrUsernameEditText.error = "Enter the email used for this account"
                Toast.makeText(
                    this,
                    "Sign in with your email address. Usernames are not used to look up private email addresses.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            performFirebaseLogin(emailOrUsername, password)
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
                            db.collection(UserDirectory.COLLECTION)
                                .document(uid)
                                .set(
                                    UserDirectory.fromPrivateData(uid, doc.data.orEmpty()),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .addOnSuccessListener {
                                    saveLocalSession(uid, username, email)
                                    Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Could not sync your searchable profile: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Your account exists, but its profile is missing. Sign up with the same email to finish setup.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Signed in, but Firestore could not load your profile: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
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
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("uid", uid)
            .putString("username", username)
            .putString("email", email)
            .apply()
    }
}
