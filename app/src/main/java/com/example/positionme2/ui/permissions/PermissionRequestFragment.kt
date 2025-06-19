package com.example.positionme2.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.positionme2.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionRequestFragment : Fragment() {

    // All required permissions for the app
    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ).apply {
        // Add ACTIVITY_RECOGNITION permission for Android Q and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        // For Android R and above, add new storage permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            remove(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    // Permission status indicators
    private lateinit var ivLocationStatus: ImageView
    private lateinit var ivSensorStatus: ImageView
    private lateinit var ivStorageStatus: ImageView
    private lateinit var ivCameraStatus: ImageView
    private lateinit var btnGrantPermissions: Button

    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        var someRationale = false

        // Process location permissions
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!locationGranted) {
            allGranted = false
            // Check if we should show rationale
            someRationale = someRationale || shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        updatePermissionStatus(ivLocationStatus, locationGranted)

        // Process sensor permissions (Activity Recognition)
        var sensorGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sensorGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
            if (!sensorGranted) {
                allGranted = false
                someRationale = someRationale || shouldShowRequestPermissionRationale(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
        }
        updatePermissionStatus(ivSensorStatus, sensorGranted)

        // Process storage permissions
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val storageGranted = permissions[storagePermission] == true
        if (!storageGranted) {
            allGranted = false
            someRationale = someRationale || shouldShowRequestPermissionRationale(storagePermission)
        }
        updatePermissionStatus(ivStorageStatus, storageGranted)

        // Process camera permissions
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (!cameraGranted) {
            allGranted = false
            someRationale = someRationale || shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            )
        }
        updatePermissionStatus(ivCameraStatus, cameraGranted)

        // Handle overall result
        if (allGranted) {
            proceedToNextScreen()
        } else if (someRationale) {
            // Some permissions were denied but we can ask again
            showPermissionRationaleDialog()
        } else {
            // User has selected "Don't ask again" for at least one permission
            showSettingsDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_permission_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        ivLocationStatus = view.findViewById(R.id.ivLocationStatus)
        ivSensorStatus = view.findViewById(R.id.ivSensorStatus)
        ivStorageStatus = view.findViewById(R.id.ivStorageStatus)
        ivCameraStatus = view.findViewById(R.id.ivCameraStatus)
        btnGrantPermissions = view.findViewById(R.id.btnGrantPermissions)

        // Set button click listener
        btnGrantPermissions.setOnClickListener {
            requestPermissions()
        }

        // Check current permission status
        checkPermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Update statuses in case user granted permissions from settings
        checkPermissionStatus()
    }

    private fun checkPermissionStatus() {
        // Check location permission status
        val locationGranted = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        updatePermissionStatus(ivLocationStatus, locationGranted)

        // Check sensor permission status
        var sensorGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sensorGranted = checkPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        updatePermissionStatus(ivSensorStatus, sensorGranted)

        // Check storage permission status
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val storageGranted = checkPermission(storagePermission)
        updatePermissionStatus(ivStorageStatus, storageGranted)

        // Check camera permission status
        val cameraGranted = checkPermission(Manifest.permission.CAMERA)
        updatePermissionStatus(ivCameraStatus, cameraGranted)

        // Check if all permissions are granted
        val allGranted = locationGranted && sensorGranted && storageGranted && cameraGranted

        // If all permissions are granted, proceed to next screen
        if (allGranted) {
            btnGrantPermissions.text = "Continue"
            btnGrantPermissions.setOnClickListener {
                proceedToNextScreen()
            }
        } else {
            btnGrantPermissions.text = "Grant Permissions"
            btnGrantPermissions.setOnClickListener {
                requestPermissions()
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionStatus(imageView: ImageView, granted: Boolean) {
        if (granted) {
            imageView.setImageResource(android.R.drawable.ic_menu_set_as)
            imageView.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
        } else {
            imageView.setImageResource(android.R.drawable.ic_delete)
            imageView.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            )
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("These permissions are essential for the app to function properly. Without them, certain features may not work as expected.")
            .setPositiveButton("Try Again") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Not Now") { _, _ ->
                Toast.makeText(
                    context,
                    "You can grant permissions later from Settings",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("To use all features of this app, you need to grant the required permissions. Please go to Settings to enable them.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Not Now") { _, _ ->
                Toast.makeText(
                    context,
                    "Some features may not be available without permissions",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun proceedToNextScreen() {
        // Navigate to the compass calibration screen
        findNavController().navigate(R.id.action_permissionFragment_to_calibrationFragment)
    }
}
