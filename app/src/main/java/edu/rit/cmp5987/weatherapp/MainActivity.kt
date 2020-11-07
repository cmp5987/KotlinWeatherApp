package edu.rit.cmp5987.weatherapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //CallAPILoginAsyncTask("Catherine", "12345").execute()

        //initialize
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!isLocationEnabled()){
            Toast.makeText(this, "Your location provider is turned off. Please turn it on.", Toast.LENGTH_SHORT).show()

            //Lead the user to where they can turn location on
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            //Check for permissions
            Dexter.withActivity(this).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
                .withListener(object : MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()){
                            requestLocationData()
                        }
                        if(report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity, "You have denied location permission. Please allow location.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread().check()
        }

    }
    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permissions for this feature. It can be enabled under Application Settings.")
            .setPositiveButton("Go TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    //link to application so the right settings are open
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    //start activity
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                dialog, _ -> dialog.dismiss()
            }.show()
    }

    /* Get the location of the user */
    private fun isLocationEnabled(): Boolean {

        //this provides acess to the ystem location services
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current latitude", "$latitude")
            val longitude = mLastLocation.longitude
            Log.i("Current longitude", "$longitude")

        }
    }

    /*
    private inner class CallAPILoginAsyncTask(val username: String, val password: String): AsyncTask<Any, Void, String>(){

        private lateinit var customProgressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }


        override fun doInBackground(vararg params: Any?): String {
            var result: String

            //develop connection
            var connection: HttpURLConnection? = null

            //incase something goes wrong
            try{
                val url = URL("https://run.mocky.io/v3/256f47b8-b718-4919-90bc-58e2bede4c69")
                connection =  url.openConnection() as HttpURLConnection
                connection.doInput = true

                /*
                //by default doInput is true but doOutput is false

                connection.doOutput = true

                //follow redirect
                connection.instanceFollowRedirects = false

                //accept the request method
                //GET POST HEAD
                connection.requestMethod = "POST"

                //Request Details
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                //set if we want to use cache
                connection.useCaches = false

                //we can use this to write data
                val writeDataOutputStream = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                jsonRequest.put("username", username)
                jsonRequest.put("password", password)

                writeDataOutputStream.writeBytes(jsonRequest.toString())
                writeDataOutputStream.flush()
                writeDataOutputStream.close()

                */

                //how to recieve data
                val httpResult: Int = connection.responseCode
                //check for 200 value
                if(httpResult === HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    //this can go wrong again
                    try{
                        while(reader.readLine().also{line = it} != null){
                            stringBuilder.append(line + "\n")
                        }
                    }catch(e: IOException){
                        e.printStackTrace()
                    }finally {
                        //close input stream
                        try{
                            inputStream.close()
                        }catch (e: IOException){
                            e.printStackTrace()
                        }
                    }
                    result = stringBuilder.toString()
                }else{
                    result = connection.responseMessage
                }
            }catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }catch (e: Exception){
                result = "Error: " + e.message
            }finally {
                connection?.disconnect()
            }
            return result
        }


        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()


            Log.i("JSON RESPONSE RESULT", result)
            val responseData = Gson().fromJson(result, ResponseData::class.java)
            Log.i("Message", responseData.message)
            Log.i("Rating", "${responseData.profile_details.is_profile_completed}")
            for(item in responseData.data_list.indices){
                Log.i("ID", "${responseData.data_list[item].id}")
            }

            /*
            val jsonObject = JSONObject(result)
            val message = jsonObject.optString("message")
            Log.i("Message", message)

            val userId = jsonObject.optInt("user_id")
            Log.i("UserId", "$userId")

            val profileDetailsObject = jsonObject.optJSONObject("profile_details")
            val isProfileCompleted = profileDetailsObject.optBoolean("is_profile_completed")
            Log.i("Is Profile Completed", "$isProfileCompleted")

            val dataListArray = jsonObject.optJSONArray("data_list")
            Log.i("DataList Size", "${dataListArray.length()}")
            for(item in 0 until dataListArray.length()){
                Log.i("Value $item", "${dataListArray[item]}")
                val dataItemObject: JSONObject = dataListArray[item] as JSONObject
                val id = jsonObject.optInt("id")
                Log.i("ID", "$id")
                val value = jsonObject.optString("value")
                Log.i("InnerValue", "$value")
            }

             */
        }

        private fun showProgressDialog(){
            customProgressDialog = Dialog(this@MainActivity)
            customProgressDialog.setContentView(R.layout.dialog_custom_progress)
            customProgressDialog.show()
        }
        private fun cancelProgressDialog(){
            customProgressDialog.dismiss()
        }

    }

     */

}