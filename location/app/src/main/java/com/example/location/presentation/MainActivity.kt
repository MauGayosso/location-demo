/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.location.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.location.R
import com.example.location.presentation.theme.LocationTheme
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import  com.google.android.gms.location.LocationRequest
import android.content.Context
import android.location.Geocoder
import com.google.firebase.FirebaseApp
import java.io.IOException
import java.util.Locale
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    var locationCallback: LocationCallback? = null
    var fusedLocationClient: FusedLocationProviderClient? = null
    var locationRequired = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            FirebaseApp.initializeApp(this)
            LocationTheme {
                val contex = LocalContext.current
                var currentLocation by remember {
                    mutableStateOf(LocationDetails(0.toDouble(), 0.toDouble()))
                }
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(p0: LocationResult) {
                        for (location in p0.locations) {
                            currentLocation = LocationDetails(location.latitude, location.longitude)
                        }
                    }
                }
                val launcherMultiplePermissions = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsMap ->
                    val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                    if (areGranted) {
                        locationRequired = true
                        Toast.makeText(contex, "Location Permission Granted", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(contex, "Location Permission Denied", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val permissions = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    Button(onClick = {
                        if (permissions.all {
                                ContextCompat.checkSelfPermission(
                                    contex,
                                    it
                                ) == PackageManager.PERMISSION_GRANTED

                            }) {
                            //startlocationsupdate
                            
                            startLocationUpdates()
                            getAdress(latitude= currentLocation.latitude,longitude = currentLocation.longitude, context = contex)
                            val add = getAdress(latitude= currentLocation.latitude,longitude = currentLocation.longitude, context = contex)
                            writeDB(currentLocation.latitude, currentLocation.longitude, add.toString())
                        } else {
                            launcherMultiplePermissions.launch(permissions)
                        }
                    }) {
                        Text(text = "GET")
                    }
                    Text(text = "Latitud: " + currentLocation.latitude)
                    Text(text = "Longitud: " + currentLocation.longitude)
                    Text(text = getAdress(latitude= currentLocation.latitude,longitude = currentLocation.longitude, context = contex).toString() )
                }
            }
        }
    }
    
    fun writeDB(latitude: Double,longitude: Double,adress: String?){
        val database = Firebase.database("https://fir-pyrebase-b3338-default-rtdb.firebaseio.com/")
        val myRef = database.getReference("location")
        
        listOf(adress).forEach(){
            myRef.child("address").setValue(adress)
            myRef.child("latitude").setValue(latitude)
            myRef.child("longitude").setValue(longitude)
        }
    }
    fun getAdress(context : Context, latitude : Double, longitude: Double): String?{
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val adresses = geocoder.getFromLocation(latitude,longitude,1)
            if (adresses != null && adresses.size > 0 ) {
                val adress = adresses[0]
                print("UBICACION: $adress")
                return adress.getAddressLine(0) }
        } catch (e: IOException){
            e.printStackTrace()
        }
        return null
    }
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        locationCallback?.let {
            val locationRequest = LocationRequest.create().apply {
                interval = 3000
                fastestInterval = 1500
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient?.requestLocationUpdates(
                locationRequest, it, Looper.getMainLooper()
            )
        }
    }
    override fun onResume(){
        super.onResume()
        if (locationRequired){
            startLocationUpdates()
        }
    }
    override fun onPause(){
        super.onPause()
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }
}
@Composable
fun WearApp(greetingName: String) {
    
    LocationTheme {
        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
         * version of LazyColumn for wear devices with some added features. For more information,
         * see d.android.com/wear/compose.
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    
    WearApp("Preview Android")
}