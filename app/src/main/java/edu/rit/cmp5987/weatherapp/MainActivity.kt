package edu.rit.cmp5987.weatherapp

import android.app.Dialog
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CallAPILoginAsyncTask().execute()
    }

    private inner class CallAPILoginAsyncTask(): AsyncTask<Any, Void, String>(){

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
                //by default doInput is true but doOutput is false
                //connection.doOutput = true

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

}