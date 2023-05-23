package com.lovestory.lovestory.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import android.app.Notification
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.*
import com.google.android.gms.location.*
import com.lovestory.lovestory.R
import com.lovestory.lovestory.broadcasts.LocationToPhoto.ACTION_START_PHOTO_PICKER_SERVICE
import com.lovestory.lovestory.broadcasts.LocationToPhoto.ACTION_STOP_PHOTO_PICKER_SERVICE
import com.lovestory.lovestory.module.checkNearby
import com.lovestory.lovestory.module.getToken
import com.lovestory.lovestory.module.saveLocation
import com.lovestory.lovestory.module.shared.saveDistanceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Objects

class LocationService : Service() {
    private val locationLogName = "[SERVICE] LOCATION"
    val context = this
    private var isPhotoServiceRunning = false
    private var currentInterval: Long = 1 * 60 * 1000

//    private lateinit var token : String

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var backgroundHandler: Handler
    private lateinit var handlerThread: HandlerThread

    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(locationLogName, "onStartCommand : start location foreground service")
        createNotificationChannel()

        val notification: Notification = createNotificationForLocationService()

        startForeground(NOTIFICATION_ID, notification)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val permissionResult = ContextCompat.checkSelfPermission(this, permission[0]) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, permission[1]) != PackageManager.PERMISSION_GRANTED

        if (!permissionResult) {
            startLocationUpdates(currentInterval)
        }else{
//            getLocationPermission()
        }
        return START_STICKY
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(interval : Long) {
        Log.e(locationLogName, "startLocationUpdates : start location updates")

        handlerThread = HandlerThread("LocationServiceBackground")
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if(location != null){
                Log.d(locationLogName, "${location.longitude} ${location.latitude}")

            }
            else{Log.e(locationLogName, "startLocationUpdates : get location error (value is null)")

            }
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, interval).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val token = getToken(context)
                for (location in locationResult.locations) {
                    location.latitude
                    location.longitude

                    saveLocation(token, location.latitude, location.longitude)

                    CoroutineScope(Dispatchers.IO).launch {
                        val nearbyResponse = checkNearby(token)
                        if (nearbyResponse != null) {
                            saveDistanceInfo(context, nearbyResponse.distance.toInt())
                            updateLocationRequestInterval( (nearbyResponse.distance.toLong()/500 +1L) * 60 * 1000)
                            if(nearbyResponse.isNearby){
                                sendBroadcastToSecondService(ACTION_START_PHOTO_PICKER_SERVICE)
                            }else{
                                sendBroadcastToSecondService(ACTION_STOP_PHOTO_PICKER_SERVICE)
                            }
                        }
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            backgroundHandler.looper
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationRequestInterval(newInterval: Long) {
        Log.d(locationLogName, "updateLocationRequestInterval : currentInterval : $currentInterval / newInterval : $newInterval")
        if (newInterval != currentInterval){
            Log.d(locationLogName, "updateLocationRequestInterval : change interval update location")
            currentInterval = newInterval

            fusedLocationClient.removeLocationUpdates(locationCallback)

            locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, newInterval).build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                backgroundHandler.looper
            )
        }
    }

    private fun sendBroadcastToSecondService(action: String) {
        val intent = Intent(this, PhotoService::class.java)
        intent.action = action
        when (intent.action) {
            ACTION_START_PHOTO_PICKER_SERVICE -> {
                if (!isPhotoServiceRunning) {
                    isPhotoServiceRunning = true
                    startForegroundService(intent)
                }
            }
            ACTION_STOP_PHOTO_PICKER_SERVICE -> {
                if (isPhotoServiceRunning) {
                    isPhotoServiceRunning = false
                    stopService(intent)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, "Lovestory", importance)
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotificationForLocationService():Notification{
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LoveStory")
            .setContentText("연인을 만나 추억을 기록해보세요!")
            .setSmallIcon(R.drawable.ic_status_bar_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "Lovestory"
        private const val NOTIFICATION_ID = 1
    }
}





