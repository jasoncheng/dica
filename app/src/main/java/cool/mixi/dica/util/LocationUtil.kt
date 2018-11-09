package cool.mixi.dica.util

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import cool.mixi.dica.App
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*
import kotlin.collections.HashMap

@SuppressLint("MissingPermission")

interface IGetAddress {
    fun done(address: Address)
}

interface IGetLocation {
    fun done(location: Location?)
}

class LocationUtil {

    companion object {
        val instance = LocationUtil()

        @SuppressLint("StaticFieldLeak")
        private val client = getFusedLocationProviderClient(App.instance.applicationContext)

        @SuppressLint("ConstantLocale")
        private val geocoder = Geocoder(App.instance.applicationContext, Locale.getDefault())
    }

    private var mLocationRequest: LocationRequest? = null
    private var callback: LocationCallback? = null
    private var isWatching = false
    private var cached = HashMap<String, Address>()
    private val updateInterval = (10 * 1000).toLong()
    private val fastestInterval: Long = 2000

    @SuppressLint("MissingPermission")
    fun getLastLocation(cb: IGetLocation) {
        client?.lastLocation?.addOnSuccessListener {
            cb.done(it)
        }?.addOnFailureListener {
            dLog(it.printStackTrace().toString())
        }
    }

    private fun getAddressKey(location: Location): String {
        return "${location.latitude},${location.longitude}"
    }

    fun getAddress(location: Location, callback: IGetAddress) {
        val key = getAddressKey(location)
        if(cached.containsKey(key)){
            dLog("getAddress from cache ${key}")
            callback.done(cached[key]!!)
            return
        }

        doAsync {
            var address = geocoder.getFromLocation(
                location.latitude, location.longitude, 1).first()
            cached[key] = address
            uiThread {
                dLog("getAddress ${address.getAddressLine(0)}")
                callback.done(address)
            }
        }
    }

    fun getAddress(callback: IGetAddress) {
        getLocation(object: IGetLocation {
            override fun done(location: Location?) {
                if(location != null) getAddress(location, callback)
            }
        })
    }

    fun getLocation(cb: IGetLocation){
        getLastLocation(object : IGetLocation {
            override fun done(location: Location?) {
                if(location != null) {
                    dLog("getLastLocation ${location.latitude}, ${location.longitude}")
                    cb.done(location)
                }
            }
        })

        startLocationUpdates(object : IGetLocation{
            override fun done(location: Location?) {
                if(location != null) {
                    dLog("getGPSLocation ${location.latitude}, ${location.longitude}")
                    cb.done(location)
                }
            }
        })
    }

    fun stopLocationUpdate(){
        isWatching = false
        client?.removeLocationUpdates(callback!!)
        dLog("stopLocationUpdate")
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(cb: IGetLocation) {
        if(isWatching) return

        isWatching = true
        mLocationRequest = LocationRequest()
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest?.interval = updateInterval
        mLocationRequest?.fastestInterval = fastestInterval
        callback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                stopLocationUpdate()
                cb.done(locationResult?.lastLocation)
            }
        }

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()
        val settingsClient = LocationServices.getSettingsClient(App.instance.applicationContext)
        settingsClient.checkLocationSettings(locationSettingsRequest)
        client.requestLocationUpdates(mLocationRequest, callback, Looper.myLooper())
    }
}