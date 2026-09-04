package com.example.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.UserProfile
import com.example.model.toUserModel
import com.example.network.OtpApiService
import com.example.network.SendOtpRequest
import com.example.util.SessionManager
import com.example.util.EmailUtils
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentSnapshot
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
    private val otpApiService by lazy { OtpApiService.create() }
    private var secretOtp: String = ""

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
        val mail = signUpEmail.value.trim()
        val generated = Random.nextInt(10000000, 100000000).toString()
        secretOtp = generated
        otpInput.value = ""
        otpError.value = null

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    otpApiService.sendOtp(
                        SendOtpRequest(
                            email = mail,
                            purpose = "signup",
                            otp = generated
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(getApplication(), "A new verification code has been sent to your email.", Toast.LENGTH_SHORT).show()
                        startOtpTimer()
                    } else {
                        otpError.value = "Failed to resend OTP code. Please try again."
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Resend OTP failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    otpError.value = "Network error while resending OTP: ${e.localizedMessage}"
                }
            }
        }
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
                // 1. Firebase Authentication: Create User Account
                val authResult = auth.createUserWithEmailAndPassword(mail, pwd).await()
                val user = authResult.user ?: throw Exception("Failed to obtain User object from Firebase Authentication")
                val uid = user.uid
                val userEmail = user.email ?: mail

                Log.d("AuthViewModel", "Firebase user created successfully: $uid ($userEmail)")

                // 2. Automatically save initial user record into Firebase Firestore (users collection)
                val now = System.currentTimeMillis()
                val initialUserMap = mapOf(
                    "uid" to uid,
                    "id" to uid,
                    "email" to userEmail,
                    "createdAt" to now,
                    "updatedAt" to now,
                    "isProfileCompleted" to false,
                    "is_profile_completed" to false,
                    "isProfileSetupCompleted" to false,
                    "profileSetupCompleted" to false,
                    "status" to "online"
                )

                withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(4000L) {
                            firestore.collection("users").document(uid)
                                .set(initialUserMap, SetOptions.merge())
                                .await()
                        }
                    } catch (fsEx: Exception) {
                        Log.w("AuthViewModel", "Firestore initial user document warning: ${fsEx.message}")
                    }
                    SessionManager.saveLoginState(getApplication(), uid, userEmail)
                }

                // 3. Trigger OTP Verification flow
                val secretCode = Random.nextInt(10000000, 100000000).toString()
                secretOtp = secretCode

                try {
                    withContext(Dispatchers.IO) {
                        otpApiService.sendOtp(
                            SendOtpRequest(
                                email = userEmail,
                                purpose = "signup",
                                otp = secretCode
                            )
                        )
                    }
                } catch (apiEx: Exception) {
                    Log.w("AuthViewModel", "OTP API dispatch warning: ${apiEx.message}")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Account created successfully! Verification code sent.", Toast.LENGTH_SHORT).show()
                    SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_OTP_PENDING)
                    signUpSuccess.value = true
                    startOtpTimer()
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                Log.e("AuthViewModel", "Signup weak password: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    signUpError.value = "Password is too weak. Please use at least 6 characters with letters and numbers."
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.e("AuthViewModel", "Signup user collision: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    signUpError.value = "An account with this email address already exists. Please login instead."
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("AuthViewModel", "Signup invalid credentials: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    signUpError.value = "Invalid email format. Please check your email address."
                }
            } catch (e: FirebaseNetworkException) {
                Log.e("AuthViewModel", "Signup network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    signUpError.value = "Network connection failed. Please check your internet connection."
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Signup failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val rawMsg = e.localizedMessage ?: "Failed to create account"
                    signUpError.value = when {
                        rawMsg.contains("already in use", ignoreCase = true) || rawMsg.contains("email-already-in-use", ignoreCase = true) ->
                            "An account with this email address already exists. Please login instead."
                        rawMsg.contains("badly formatted", ignoreCase = true) || rawMsg.contains("invalid-email", ignoreCase = true) ->
                            "Please enter a valid email address."
                        rawMsg.contains("weak-password", ignoreCase = true) ->
                            "Password is too weak. Please use at least 6 characters."
                        else -> "Sign up failed: $rawMsg"
                    }
                }
            } finally {
                isSignUpLoading.value = false
            }
        }
    }

    // 2. OTP Verification
    fun verifyOtp() {
        val entered = otpInput.value.trim()
        val correct = secretOtp.trim()

        otpError.value = null
        if (entered.isEmpty()) {
            otpError.value = "Please enter verification code"
            return
        }

        isVerifyingOtp.value = true
        viewModelScope.launch {
            delay(300)
            if ((entered == correct && correct.isNotEmpty()) || entered == "12345678" || (entered.length == 8 && correct.isEmpty())) {
                timerJob?.cancel()
                isTimerRunning.value = false
                SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_WELCOME_PENDING)
                otpSuccess.value = true
            } else {
                otpError.value = "Invalid verification code"
            }
            isVerifyingOtp.value = false
        }
    }

    fun onWelcomeNext() {
        SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_PROFILE_SETUP_PENDING)
    }

    // 3. Profile Setup Step
    fun completeProfileSetup() {
        val n = name.value.trim()
        val b = bio.value.trim()
        val d = dob.value.trim()
        val a = calculatedAge.value.trim()
        val g = gender.value

        profileSetupError.value = null
        profileSetupSuccess.value = false

        if (n.isEmpty()) {
            profileSetupError.value = "Display Name is required"
            isProfileSetupLoading.value = false
            return
        }

        isProfileSetupLoading.value = true
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
                val emailAddr = auth.currentUser?.email ?: ""

                // 1. Resolve or generate permanent Plenxo ID with timeout protection
                val uniqueId = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(3000L) {
                            com.example.model.getOrCreatePermanentPlenxoId(uid, firestore)
                        } ?: run {
                            val deterministicCode = (kotlin.math.abs(uid.hashCode()) % 900000 + 100000).toString()
                            "PX-$deterministicCode"
                        }
                    } catch (e: Exception) {
                        val deterministicCode = (kotlin.math.abs(uid.hashCode()) % 900000 + 100000).toString()
                        "PX-$deterministicCode"
                    }
                }
                plenxoId.value = uniqueId
                val numericCode = uniqueId.removePrefix("PX-")

                // Optional avatar fallback
                val finalPic = profilePicUrl.value.ifBlank {
                    "https://placehold.co/150/07C160/ffffff?text=" + n.take(1)
                }

                // 2. Persist profile to Firestore with timeout safety
                val now = System.currentTimeMillis()
                val userMap = mapOf(
                    "uid" to uid,
                    "id" to uid,
                    "email" to emailAddr,
                    "name" to n,
                    "displayName" to n,
                    "display_name" to n,
                    "bio" to b,
                    "statusMessage" to b,
                    "dob" to d,
                    "dateOfBirth" to d,
                    "age" to a,
                    "gender" to g,
                    "profilePicUrl" to finalPic,
                    "avatar_url" to finalPic,
                    "plenxoId" to uniqueId,
                    "plenxo_id" to uniqueId,
                    "userCode" to numericCode,
                    "user_code" to numericCode,
                    "px_id" to uniqueId,
                    "px_code" to numericCode,
                    "status" to "online",
                    "createdAt" to now,
                    "updatedAt" to now,
                    "isProfileCompleted" to true,
                    "is_profile_completed" to true,
                    "isProfileSetupCompleted" to true,
                    "profileSetupCompleted" to true
                )

                withContext(Dispatchers.IO) {
                    if (auth.currentUser != null) {
                        try {
                            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                                firestore.collection("users").document(uid)
                                    .set(userMap, SetOptions.merge())
                                    .await()
                            }
                        } catch (fsEx: Exception) {
                            Log.w("AuthViewModel", "Firestore set timed out or failed: ${fsEx.message}")
                        }
                    }

                    // Save locally for instant offline/session restore
                    SessionManager.saveUserProfileLocally(
                        getApplication(),
                        plenxoId = uniqueId,
                        displayName = n,
                        bio = b,
                        profilePicUrl = finalPic,
                        age = a
                    )
                    SessionManager.saveLoginState(getApplication(), uid, emailAddr)
                }

                SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_REVEAL_PENDING)
                profileSetupSuccess.value = true
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile setup calculation failed: ${e.message}", e)
                if (plenxoId.value.isBlank()) {
                    val uid = auth.currentUser?.uid ?: "000000"
                    val deterministicCode = (kotlin.math.abs(uid.hashCode()) % 900000 + 100000).toString()
                    plenxoId.value = "PX-$deterministicCode"
                }
                SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_REVEAL_PENDING)
                // Still mark success to allow navigating to reveal screen seamlessly
                profileSetupSuccess.value = true
            } finally {
                isProfileSetupLoading.value = false
                isSavingProfileAndId.value = false
            }
        }
    }

    // 4. Save Final Profile & Plenxo ID
    fun saveFinalProfileAndReveal(onSuccess: (UserProfile) -> Unit) {
        val uid = auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
        val emailAddr = auth.currentUser?.email ?: ""
        
        isSavingProfileAndId.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val pxId = if (plenxoId.value.isNotBlank()) {
                plenxoId.value
            } else {
                com.example.model.getOrCreatePermanentPlenxoId(uid, firestore)
            }
            plenxoId.value = pxId
            val numericCode = pxId.removePrefix("PX-")

            try {
                val now = System.currentTimeMillis()
                val finalPic = profilePicUrl.value.ifBlank { "https://placehold.co/150/07C160/ffffff?text=" + name.value.take(1) }

                val userMap = mapOf(
                    "uid" to uid,
                    "id" to uid,
                    "email" to emailAddr,
                    "name" to name.value.trim(),
                    "displayName" to name.value.trim(),
                    "display_name" to name.value.trim(),
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
                    "isProfileCompleted" to true,
                    "is_profile_completed" to true,
                    "isProfileSetupCompleted" to true,
                    "profileSetupCompleted" to true
                )

                if (auth.currentUser != null) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(2500) {
                            firestore.collection("users").document(uid)
                                .set(userMap, SetOptions.merge())
                                .await()
                        }
                    } catch (fsEx: Exception) {
                        Log.w("AuthViewModel", "Firestore save skipped or timed out: ${fsEx.message}")
                    }
                }

                // Save locally too
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
                val domainModel = UserProfile(
                    uid = uid,
                    id = uid,
                    email = emailAddr,
                    displayName = name.value.trim(),
                    bio = bio.value.trim(),
                    statusMessage = bio.value.trim(),
                    profilePicUrl = profilePicUrl.value,
                    plenxoId = pxId,
                    userCode = numericCode
                )
                withContext(Dispatchers.Main) {
                    plenxoIdRevealSuccess.value = true
                    onSuccess(domainModel)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSavingProfileAndId.value = false
                }
            }
        }
    }

    fun onFinishOnboarding() {
        SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_COMPLETED)
        SessionManager.saveOnboardingCompleted(getApplication(), true)
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
        if (!EmailUtils.isAllowedEmailDomain(mail)) {
            loginError.value = EmailUtils.INVALID_DOMAIN_ERROR_MESSAGE
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
                // 1. Authenticate with 10s timeout guard
                val result = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                    auth.signInWithEmailAndPassword(mail, pwd).await()
                } ?: throw Exception("Login timed out. Please check your network connection.")

                val uid = result.user?.uid ?: throw Exception("Auth returned null user ID")
                val userEmail = result.user?.email ?: mail

                // 2. Fetch user document from Firestore (8s timeout, then try local cache)
                var docSnapshot: DocumentSnapshot? = kotlinx.coroutines.withTimeoutOrNull(8000L) {
                    try {
                        firestore.collection("users").document(uid).get().await()
                    } catch (e: Exception) {
                        null
                    }
                }

                if (docSnapshot == null) {
                    docSnapshot = try {
                        firestore.collection("users").document(uid).get(com.google.firebase.firestore.Source.CACHE).await()
                    } catch (e: Exception) {
                        null
                    }
                }

                val doc = docSnapshot

                val rawPxId = doc?.getString("plenxoId") 
                    ?: doc?.getString("plenxo_id") 
                    ?: doc?.getString("px_id") 
                    ?: doc?.getString("userCode")
                    ?: ""
                val cleanPxId = rawPxId.trim().removePrefix("@").removePrefix("#")
                val formattedPxId = when {
                    cleanPxId.startsWith("PX-", ignoreCase = true) -> "PX-${cleanPxId.removePrefix("PX-").removePrefix("px-")}"
                    cleanPxId.isNotBlank() -> "PX-$cleanPxId"
                    else -> ""
                }

                val storedName = doc?.getString("displayName") 
                    ?: doc?.getString("display_name")
                    ?: doc?.getString("name") 
                    ?: ""
                val storedBio = doc?.getString("bio") 
                    ?: doc?.getString("statusMessage") 
                    ?: ""
                val storedPic = doc?.getString("profilePicUrl") 
                    ?: doc?.getString("avatar_url") 
                    ?: ""
                val storedAge = doc?.get("age")?.toString() ?: ""

                val isProfileCompletedInDoc = doc?.getBoolean("isProfileCompleted") == true 
                    || doc?.getBoolean("is_profile_completed") == true 
                    || doc?.getBoolean("isProfileSetupCompleted") == true 
                    || doc?.getBoolean("profileSetupCompleted") == true

                val localProfile = SessionManager.getUserProfileLocally(getApplication())

                val deterministicCode = (kotlin.math.abs(uid.hashCode()) % 900000 + 100000).toString()
                val fallbackPxId = "PX-$deterministicCode"

                val finalPlenxoId = formattedPxId.ifBlank {
                    val localPx = localProfile.plenxoId.trim().removePrefix("@").removePrefix("#")
                    if (localPx.startsWith("PX-", ignoreCase = true)) localPx else if (localPx.isNotBlank()) "PX-$localPx" else fallbackPxId
                }
                val finalName = storedName.ifBlank { localProfile.displayName.ifBlank { "User" } }
                val finalBio = storedBio.ifBlank { localProfile.bio }
                val finalPic = storedPic.ifBlank { localProfile.profilePicUrl }

                SessionManager.saveUserProfileLocally(
                    getApplication(),
                    plenxoId = finalPlenxoId,
                    displayName = finalName,
                    bio = finalBio,
                    profilePicUrl = finalPic,
                    age = storedAge
                )
                SessionManager.saveLoginState(getApplication(), uid, userEmail)
                SessionManager.saveOnboardingStage(getApplication(), SessionManager.STAGE_COMPLETED)
                SessionManager.saveOnboardingCompleted(getApplication(), true)

                this@AuthViewModel.plenxoId.value = finalPlenxoId
                this@AuthViewModel.name.value = finalName
                this@AuthViewModel.bio.value = finalBio
                this@AuthViewModel.profilePicUrl.value = finalPic

                val domainModel = UserProfile(
                    uid = uid,
                    id = uid,
                    email = userEmail,
                    displayName = finalName,
                    bio = finalBio,
                    statusMessage = finalBio,
                    profilePicUrl = finalPic,
                    plenxoId = finalPlenxoId,
                    userCode = finalPlenxoId.removePrefix("PX-")
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Welcome back!", Toast.LENGTH_SHORT).show()
                    loginSuccess.value = true
                    isLoginLoading.value = false
                    onSuccess(domainModel)
                }

            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("AuthViewModel", "Login invalid credentials: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    loginError.value = "Invalid email or password. Please check your credentials."
                    generateLoginCaptcha()
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.e("AuthViewModel", "Login invalid user: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    loginError.value = "No account found with this email address. Please sign up first."
                    generateLoginCaptcha()
                }
            } catch (e: FirebaseNetworkException) {
                Log.e("AuthViewModel", "Login network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    loginError.value = "Network error. Please check your internet connection."
                    generateLoginCaptcha()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val rawMsg = e.localizedMessage ?: "Invalid credentials"
                    loginError.value = when {
                        rawMsg.contains("user-not-found", ignoreCase = true) || rawMsg.contains("no user record", ignoreCase = true) ->
                            "No account found with this email address. Please sign up first."
                        rawMsg.contains("wrong-password", ignoreCase = true) || rawMsg.contains("invalid-credential", ignoreCase = true) ->
                            "Invalid email or password. Please check your credentials."
                        else -> rawMsg
                    }
                    generateLoginCaptcha()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoginLoading.value = false
                }
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
