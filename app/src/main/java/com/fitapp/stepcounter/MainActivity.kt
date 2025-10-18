package com.fitapp.stepcounter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fitapp.stepcounter.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.google.android.gms.tasks.Task

// Extension function to convert Task to suspend function
suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: Exception("Unknown error"))
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var googleSignInAccount: GoogleSignInAccount? = null
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            signInToGoogleFit()
        } else {
            Toast.makeText(this, "Permissions required for step counting", Toast.LENGTH_LONG).show()
        }
    }
    
    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            googleSignInAccount = task.result
            if (googleSignInAccount != null) {
                loadStepCount()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to sign in: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissionsAndSignIn()
    }
    
    private fun setupUI() {
        binding.refreshButton.setOnClickListener {
            if (googleSignInAccount != null) {
                loadStepCount()
            } else {
                checkPermissionsAndSignIn()
            }
        }
    }
    
    private fun checkPermissionsAndSignIn() {
        val permissions = arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BODY_SENSORS
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            signInToGoogleFit()
        }
    }
    
    private fun signInToGoogleFit() {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
            .build()
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/fitness.activity.read"))
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            googleSignInAccount = account
            loadStepCount()
        } else {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }
    
    private fun loadStepCount() {
        binding.stepCountText.text = "Loading..."
        
        lifecycleScope.launch {
            try {
                val stepCount = withContext(Dispatchers.IO) {
                    getTodayStepCount()
                }
                
                binding.stepCountText.text = stepCount.toString()
                binding.lastUpdatedText.text = "Last updated: ${getCurrentTime()}"
                
            } catch (e: Exception) {
                binding.stepCountText.text = "Error"
                Toast.makeText(this@MainActivity, "Failed to load step count: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private suspend fun getTodayStepCount(): Int {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()
            
            val account = googleSignInAccount ?: throw Exception("Not signed in")
            
            try {
                val result = Fitness.getHistoryClient(this@MainActivity, account)
                    .readData(readRequest)
                    .await()
                
                var totalSteps = 0
                for (bucket in result.buckets) {
                    for (dataSet in bucket.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            for (field in dataPoint.dataType.fields) {
                                val value = dataPoint.getValue(field)
                                totalSteps += value.asInt()
                            }
                        }
                    }
                }
                
                totalSteps
            } catch (e: Exception) {
                throw Exception("Failed to read step data: ${e.message}")
            }
        }
    }
    
    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }
}
