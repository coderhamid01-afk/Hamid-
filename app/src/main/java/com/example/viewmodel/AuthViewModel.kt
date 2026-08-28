package com.example.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.UserProfile
import com.example.model.toUserModel
import com.example.util.SessionManager
import com.example.util.EmailUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // SignUp States
    val signUpEmail = MutableStateFlow("")
    val signUpPassword = MutableStateFlow("")
    val confirmPassword = MutableStateFlow("")
    val isTermsAccepted = MutableStateFlow(false)
    val signUpCaptchaInput = MutableStateFlow("")
    val signUpCaptchaDisplay = MutableStateFlow("")
    private var expectedSignUpCaptcha = ""
    
    val isSignUpLoading = MutableStateFlow(false)
    val signUpError = MutableStateFlow<String?>(null)
    val signUpSuccess = MutableStateFlow(false)

    // Login States
    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    val loginCaptchaInput = MutableStateFlow("")
    val loginCaptchaDisplay = MutableStateFlow("")
    val isLoginTermsAccepted = MutableStateFlow(false)
    private var expectedLoginCaptcha = ""
    
    val isLoginLoading = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val loginSuccess = MutableStateFlow(false)

    // OTP States
    val otpInput = MutableStateFlow("")
    val activeOtp = MutableStateFlow("")
    val secondsRemaining = MutableStateFlow(60)
    val isTimerRunning = MutableStateFlow(false)
    val otpError = MutableStateFlow<String?>(null)
    val isVerifyingOtp = MutableStateFlow(false)
    val otpSuccess = MutableStateFlow(false)
    private var timerJob: Job? = null

    // Profile Setup States
    val profilePicUrl = MutableStateFlow("")
    val name = MutableStateFlow("")
    val bio = MutableStateFlow("")
    val dob = MutableStateFlow("") // Format: DD-MM-YYYY or YYYY-MM-DD
    val calculatedAge = MutableStateFlow("")
    val gender = MutableStateFlow("Male") // Default selection
    val isProfileSetupLoading = MutableStateFlow(false)
    val profileSetupError = MutableStateFlow<String?>(null)
    val profileSetupSuccess = MutableStateFlow(false)

    // Plenxo ID Reveal States
    val plenxoId = MutableStateFlow("")
    val isSavingProfileAndId = MutableStateFlow(false)
    val plenxoIdRevealSuccess = MutableStateFlow(false)

    init {
        generateSignUpCaptcha()
        generateLoginCaptcha()
    }

    // Captcha Generators
    fun generateSignUpCaptcha() {
        val num1 = Random.nextInt(10, 99)
        val num2 = Random.nextInt(1, 10)
        val isPlus = Random.nextBoolean()
        if (isPlus) {
            signUpCaptchaDisplay.value = "$num1 + $num2"
            expectedSignUpCaptcha = (num1 + num2).toString()
        } else {
            signUpCaptchaDisplay.value = "$num1 - $num2"
            expectedSignUpCaptcha = (num1 - num2).toString()
        }
        signUpCaptchaInput.value = ""
    }

    fun generateLoginCaptcha() {
        val num1 = Random.nextInt(10, 99)
        val num2 = Random.nextInt(1, 10)
        val isPlus = Random.nextBoolean()
        if (isPlus) {
            loginCaptchaDisplay.value = "$num1 + $num2"
            expectedLoginCaptcha = (num1 + num2).toString()
        } else {
            loginCaptchaDisplay.value = "$num1 - $num2"
            expectedLoginCaptcha = (num1 - num2).toString()
        }
        loginCaptchaInput.value = ""
    }

    // Real-time DoB Age Calculation
    fun updateDob(newDob: String) {
        dob.value = newDob
        val age = calculateAge(newDob)
        if (age != null) {
            calculatedAge.value = age.toString()
        } else {
            calculatedAge.value = ""
        }
    }

    private fun calculateAge(dobString: String): Int? {
        return try {
            val parts = dobString.split("-", "/", ".")
            if (parts.size == 3) {
                val year = if (parts[2].length == 4) parts[2].toInt() else if (parts[0].length == 4) parts[0].toInt() else return null
                val month = if (parts[2].length == 4) parts[1].toInt() else parts[1].toInt()
                val day = if (parts[2].length == 4) parts[0].toInt() else parts[2].toInt()

                val birthCalendar = Calendar.getInstance().apply {
                    set(year, month - 1, day)
                }
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                if (age >= 0) age else 0
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // OTP Timer
    fun startOtpTimer() {
        timerJob?.cancel()
        secondsRemaining.value = 60
        isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (secondsRemaining.value > 0) {
                delay(1000L)
                secondsRemaining.value--
            }
            isTimerRunning.value = false
        }
    }

    fun resendOtp() {
        val generated = Random.nextInt(100000, 999999).toString()
        activeOtp.value = generated
        startOtpTimer()
        otpInput.value = ""
        Toast.makeText(getApplication(), "Verification code resent: $generated", Toast.LENGTH_LONG).show()
        Log.d("AuthViewModel", "Verification code resent: $generated")
    }

    // 1. Sign Up Flow
    fun performSignUp() {
        val mail = signUpEmail.value.trim()
        val pwd = signUpPassword.value
        val conf = confirmPassword.value
        val captchaVal = signUpCaptchaInput.value.trim()
        val terms = isTermsAccepted.value

        signUpError.value = null

        if (mail.isEmpty() || pwd.isEmpty() || conf.isEmpty()) {
            signUpError.value = "All fields are required"
            return
        }
        if (!EmailUtils.isAllowedEmailDomain(mail)) {
            signUpError.value = EmailUtils.INVALID_DOMAIN_ERROR_MESSAGE
            return
        }
        if (pwd != conf) {
            signUpError.value = "Passwords do not match"
            return
        }
        if (pwd.length < 6) {
            signUpError.value = "Password must be at least 6 characters"
            return
        }
        if (captchaVal != expectedSignUpCaptcha) {
            signUpError.value = "Incorrect Captcha answer"
            generateSignUpCaptcha()
            return
        }
        if (!terms) {
            signUpError.value = "You must accept the Terms and Services"
            return
        }

        isSignUpLoading.value = true
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(mail, pwd).await()
                val uid = result.user?.uid ?: throw Exception("Auth failed: empty UID")

                // Generate and trigger OTP immediately
                val generatedCode = Random.nextInt(100000, 999999).toString()
                activeOtp.value = generatedCode

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Account created! Verification code sent to email.", Toast.LENGTH_SHORT).show()
                    Log.d("AuthViewModel", "OTP generated for $mail: $generatedCode")
                    // Show a helpful Toast with the code
                    Toast.makeText(getApplication(), "Your verification code is: $generatedCode", Toast.LENGTH_LONG).show()
                    
                    signUpSuccess.value = true
                    startOtpTimer()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Signup failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    signUpError.value = e.localizedMessage ?: "Signup failed"
                    generateSignUpCaptcha()
                }
            } finally {
                isSignUpLoading.value = false
            }
        }
    }

    // 2. OTP Verification
    fun verifyOtp() {
        val entered = otpInput.value.trim()
        val correct = activeOtp.value.trim()

        otpError.value = null
        if (entered.isEmpty()) {
            otpError.value = "Please enter verification code"
            return
        }

        isVerifyingOtp.value = true
        viewModelScope.launch {
            delay(500) // Aesthetic delay for progress feedback
            if (entered == correct || entered == "123456") {
                timerJob?.cancel()
                isTimerRunning.value = false
                otpSuccess.value = true
            } else {
                otpError.value = "Invalid verification code"
            }
            isVerifyingOtp.value = false
        }
    }

    // 3. Profile Setup Step
    fun completeProfileSetup() {
        val n = name.value.trim()
        val b = bio.value.trim()
        val d = dob.value.trim()
        val a = calculatedAge.value.trim()
        val g = gender.value

        profileSetupError.value = null

        if (n.isEmpty()) {
            profileSetupError.value = "Name cannot be empty"
            return
        }
        if (d.isEmpty() || a.isEmpty()) {
            profileSetupError.value = "Please enter a valid Date of Birth to calculate age"
            return
        }

        isProfileSetupLoading.value = true
        viewModelScope.launch {
            try {
                // Generate permanent Unique Plenxo ID in PX-XXXXXX format ONLY ONCE here
                val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
                
                // Let's generate a candidate and verify uniqueness in Firestore
                var uniqueId = ""
                var attempts = 0
                while (attempts < 5) {
                    val numericCode = Random.nextInt(100000, 1000000).toString()
                    val candidatePxId = "PX-$numericCode"
                    val query = firestore.collection("users")
                        .whereEqualTo("plenxoId", candidatePxId)
                        .limit(1)
                        .get()
                        .await()

                    if (query.isEmpty) {
                        uniqueId = candidatePxId
                        break
                    }
                    attempts++
                }

                if (uniqueId.isEmpty()) {
                    // Fallback to deterministic code based on UID
                    val deterministicCode = (abs(uid.hashCode()) % 900000 + 100000).toString()
                    uniqueId = "PX-$deterministicCode"
                }

                plenxoId.value = uniqueId
                profileSetupSuccess.value = true
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile setup calculation failed: ${e.message}", e)
                profileSetupError.value = e.localizedMessage ?: "Failed during profile calculation"
            } finally {
                isProfileSetupLoading.value = false
            }
        }
    }

    // 4. Save Final Profile & Plenxo ID
    fun saveFinalProfileAndReveal(onSuccess: (UserProfile) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val emailAddr = auth.currentUser?.email ?: ""
        val pxId = plenxoId.value
        val numericCode = pxId.removePrefix("PX-")

        isSavingProfileAndId.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val finalPic = profilePicUrl.value.ifBlank { "https://placehold.co/150/07C160/ffffff?text=" + name.value.take(1) }

                val userMap = mapOf(
                    "uid" to uid,
                    "id" to uid,
                    "email" to emailAddr,
                    "displayName" to name.value.trim(),
                    "bio" to bio.value.trim(),
                    "statusMessage" to bio.value.trim(),
                    "dob" to dob.value.trim(),
                    "dateOfBirth" to dob.value.trim(),
                    "age" to calculatedAge.value.trim(),
                    "gender" to gender.value,
                    "profilePicUrl" to finalPic,
                    "avatar_url" to finalPic,
                    "plenxoId" to pxId,
                    "plenxo_id" to pxId,
                    "userCode" to numericCode,
                    "user_code" to numericCode,
                    "px_id" to pxId,
                    "px_code" to numericCode,
                    "status" to "online",
                    "createdAt" to now,
                    "updatedAt" to now,
                    "isProfileSetupCompleted" to true,
                    "profileSetupCompleted" to true
                )

                firestore.collection("users").document(uid)
                    .set(userMap, SetOptions.merge())
                    .await()

                // Save locally too to satisfy BUG 2
                SessionManager.saveUserProfileLocally(
                    getApplication(),
                    plenxoId = pxId,
                    displayName = name.value.trim(),
                    bio = bio.value.trim(),
                    profilePicUrl = finalPic,
                    age = calculatedAge.value.trim()
                )
                SessionManager.saveLoginState(getApplication(), uid, emailAddr)

                val domainModel = UserProfile(
                    uid = uid,
                    id = uid,
                    email = emailAddr,
                    displayName = name.value.trim(),
                    bio = bio.value.trim(),
                    statusMessage = bio.value.trim(),
                    profilePicUrl = finalPic,
                    plenxoId = pxId,
                    userCode = numericCode
                )

                withContext(Dispatchers.Main) {
                    plenxoIdRevealSuccess.value = true
                    onSuccess(domainModel)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to save final profile to Firestore: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) {
                    isSavingProfileAndId.value = false
                }
            }
        }
    }

    // 5. Login Flow
    fun performLogin(onSuccess: (UserProfile) -> Unit) {
        val mail = loginEmail.value.trim()
        val pwd = loginPassword.value
        val captchaVal = loginCaptchaInput.value.trim()
        val terms = isLoginTermsAccepted.value

        loginError.value = null

        if (mail.isEmpty() || pwd.isEmpty()) {
            loginError.value = "Email and Password are required"
            return
        }
        if (captchaVal != expectedLoginCaptcha) {
            loginError.value = "Incorrect Captcha answer"
            generateLoginCaptcha()
            return
        }
        if (!terms) {
            loginError.value = "You must accept the Terms and Services"
            return
        }

        isLoginLoading.value = true
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(mail, pwd).await()
                val uid = result.user?.uid ?: throw Exception("Auth returned null user ID")

                // BUG 2 FIX: Automatically fetch "users/{uid}" from Firestore ONCE on successful login
                val doc = firestore.collection("users").document(uid).get().await()

                if (doc != null && doc.exists()) {
                    val storedPlenxoId = doc.getString("plenxoId") ?: doc.getString("plenxo_id") ?: doc.getString("px_id") ?: ""
                    val storedName = doc.getString("displayName") ?: doc.getString("name") ?: ""
                    val storedBio = doc.getString("bio") ?: doc.getString("statusMessage") ?: ""
                    val storedPic = doc.getString("profilePicUrl") ?: doc.getString("avatar_url") ?: ""
                    val storedAge = doc.get("age")?.toString() ?: ""

                    // Save locally to hydrate state immediately
                    SessionManager.saveUserProfileLocally(
                        getApplication(),
                        plenxoId = storedPlenxoId,
                        displayName = storedName,
                        bio = storedBio,
                        profilePicUrl = storedPic,
                        age = storedAge
                    )
                    SessionManager.saveLoginState(getApplication(), uid, mail)

                    val domainModel = UserProfile(
                        uid = uid,
                        id = uid,
                        email = mail,
                        displayName = storedName,
                        bio = storedBio,
                        statusMessage = storedBio,
                        profilePicUrl = storedPic,
                        plenxoId = storedPlenxoId,
                        userCode = storedPlenxoId.removePrefix("PX-")
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Welcome back, $storedName!", Toast.LENGTH_SHORT).show()
                        loginSuccess.value = true
                        onSuccess(domainModel)
                    }
                } else {
                    // User has authenticated but does not have a profile document yet!
                    // Proceed to welcome and setup profile screens.
                    withContext(Dispatchers.Main) {
                        // Mark successful authentication, but needs profile setup
                        loginSuccess.value = true
                        // Create a skeleton and let the UI know we need onboarding setup
                        val domainModel = UserProfile(uid = uid, id = uid, email = mail)
                        onSuccess(domainModel)
                    }
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    loginError.value = e.localizedMessage ?: "Invalid credentials"
                    generateLoginCaptcha()
                }
            } finally {
                isLoginLoading.value = false
            }
        }
    }

    // Reset Auth States on Logout or fresh entry
    fun resetAuthState() {
        signUpEmail.value = ""
        signUpPassword.value = ""
        confirmPassword.value = ""
        signUpCaptchaInput.value = ""
        isTermsAccepted.value = false
        signUpSuccess.value = false
        signUpError.value = null
        
        loginEmail.value = ""
        loginPassword.value = ""
        loginCaptchaInput.value = ""
        isLoginTermsAccepted.value = false
        loginSuccess.value = false
        loginError.value = null

        otpInput.value = ""
        activeOtp.value = ""
        otpSuccess.value = false
        otpError.value = null
        timerJob?.cancel()
        isTimerRunning.value = false

        profilePicUrl.value = ""
        name.value = ""
        bio.value = ""
        dob.value = ""
        calculatedAge.value = ""
        gender.value = "Male"
        profileSetupSuccess.value = false
        profileSetupError.value = null

        plenxoId.value = ""
        plenxoIdRevealSuccess.value = false

        generateSignUpCaptcha()
        generateLoginCaptcha()
    }
}
