package com.fathan.projectjmp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.fathan.projectjmp.TableActivity.Companion.CAMERA_X_RESULT
import com.fathan.projectjmp.databinding.ActivityAddUpdateBinding
import com.fathan.projectjmp.db.DatabaseContract
import com.fathan.projectjmp.db.NoteHelper
import com.fathan.projectjmp.model.Note
import com.fathan.projectjmp.utils.Utils
import com.fathan.projectjmp.utils.Utils.rotateBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddUpdateActivity : AppCompatActivity(), View.OnClickListener {
    private var isEdit = false
    private var note: Note? = null
    private var position: Int = 0
    private lateinit var noteHelper: NoteHelper
//    private lateinit var radioButtonChecked: RadioButton

    private lateinit var binding: ActivityAddUpdateBinding

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentCity: String
    private var uriImage: String? = null

    companion object {
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_POSITION = "extra_position"
        const val RESULT_ADD = 101
        const val RESULT_UPDATE = 201
        const val RESULT_DELETE = 301
        const val ALERT_DIALOG_CLOSE = 10
        const val ALERT_DIALOG_DELETE = 20
        const val PERMISSION_ID = 1000

        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    private fun checkPermission(): Boolean{
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
            return true
        }
        return false
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),PERMISSION_ID
        )
    }

    private fun isLocationEnabled():Boolean{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("AddActivity", "Debug: You Have Permission")
            }
        }
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    "Tidak mendapatkan permission.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg: Uri = result.data?.data as Uri
            val myFile = Utils.uriToFile(selectedImg, this)
            uriImage = myFile.path
            Log.d("AddUpdateActivity", "uri: ${selectedImg.path}")
            binding.previewImageView.setImageURI(selectedImg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        noteHelper = NoteHelper.getInstance(applicationContext)
        noteHelper.open()
        note = intent.getParcelableExtra(EXTRA_NOTE)
        if (note != null) {
            position = intent.getIntExtra(EXTRA_POSITION, 0)
            isEdit = true
        } else {
            note = Note()
        }
        val actionBarTitle: String
        val btnTitle: String
        if (isEdit) {
            actionBarTitle = "Ubah"
            btnTitle = "Update"
            note?.let {
                binding.edtName.setText(it.name)
                binding.edtDescription.setText(it.description)
                binding.edtEmail.setText(it.email)
                binding.edtLocation.setText(it.location)
                Log.d("AddActivity", "onCreate: ${it.gender}")
                val id = it.gender
                if (id != null) {
                    if(binding.rbMale.id == id){
                        binding.rbMale.isChecked = true
                    }else{
                        binding.rbFemale.isChecked = true
                    }
                }
                binding.previewImageView.setImageURI(it.imageUrl?.toUri())
            }
        } else {
            actionBarTitle = "Tambah"
            btnTitle = "Simpan"
        }
        supportActionBar?.title = actionBarTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.btnSubmit.text = btnTitle

        binding.btnSubmit.setOnClickListener(this)
        binding.btnCheckLocation.setOnClickListener{
            requestPermission()
            getLastLocation()
        }
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
        binding.btnGallery.setOnClickListener {
            Toast.makeText(this,"berhasil",Toast.LENGTH_SHORT).show()
            startGallery()
        }

//        binding.btnCamera.setOnClickListener { startCameraX() }

    }

    private fun startGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Choose a Picture")
        launcherIntentGallery.launch(chooser)
    }


    private fun getLastLocation(){
        if (checkPermission()){
            if (isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    val location = task.result
                    if (location == null){
                        getNewLocation()
                    }else{
                        currentCity = getCityName(location.latitude, location.longitude)

                        binding.edtLocation.setText(currentCity)
                        Toast.makeText(this, "getCity: ${getCityName(location.latitude, location.longitude)}", Toast.LENGTH_SHORT).show()
                        Log.d("AddActivity", "getLastLocation: ${location.latitude} + ${location.longitude} + ${getCityName(location.latitude, location.longitude)}")
                    }
                }
            }else{
                Toast.makeText(this, "Please Enable Yout Location Service", Toast.LENGTH_SHORT).show()
            }
        }else{
            requestPermission()
        }
    }

    fun getNewLocation(){
        val locationRequest: com.google.android.gms.location.LocationRequest = com.google.android.gms.location.LocationRequest()
        locationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,locationCallback, Looper.myLooper()
        )
    }


    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            if (lastLocation != null) {
                currentCity = getCityName(lastLocation.latitude, lastLocation.longitude)
            }
            Log.d("Debug:","your last last location: "+ lastLocation?.longitude.toString())
//            textView.text = "You Last Location is : Long: "+ lastLocation.longitude + " , Lat: " + lastLocation.latitude + "\n" + getCityName(lastLocation.latitude,lastLocation.longitude)
        }
    }

    private fun getCityName(lat: Double,long: Double):String{
        var cityName:String = ""
        var countryName = ""
        val geoCoder = Geocoder(this, Locale.getDefault())
        val Adress = geoCoder.getFromLocation(lat,long,3)

        cityName = Adress.get(0).locality
        countryName = Adress.get(0).countryName
        Log.d("Debug:","Your City: " + cityName + " ; your Country " + countryName)
        return cityName
    }
//    private fun startCameraX() {
//        val intent = Intent(this, CameraActivity::class.java)
//        launcherIntentCameraX.launch(intent)
//    }

//    private val launcherIntentCameraX = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) {
//        if (it.resultCode == CAMERA_X_RESULT) {
//            val myFile = it.data?.getSerializableExtra("picture") as File
//            val isBackCamera = it.data?.getBooleanExtra("isBackCamera", true) as Boolean
//
//            val result = rotateBitmap(
//                BitmapFactory.decodeFile(myFile.path),
//                isBackCamera
//            )
//
//            uriImage = myFile.path.toString()
//            Log.d("AddUpdateActivity", "uri: ${myFile}")
//            binding.previewImageView.setImageBitmap(result)
//        }
//    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_submit) {
            val name = binding.edtName.text.toString().trim()
            val description = binding.edtDescription.text.toString().trim()
            val location = binding.edtLocation.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val idGender = binding.radioGender.checkedRadioButtonId
            if (name.isEmpty()) {
                binding.edtName.error = "Field name can not be blank"
                return
            }else if (description.isEmpty()){
                binding.edtDescription.error = "Field description can not be blank"
                return
            }else if (location.isEmpty()){
                binding.btnCheckLocation.error = "Field location description can not be blank"
                return
            }else if (email.isEmpty()){
                binding.edtEmail.error = "Field email description can not be blank"
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.edtEmail.error = "Field must email"
                return
            }
            note?.name = name
            note?.description = description
            note?.location = currentCity
            note?.email = email
            note?.gender = idGender
            note?.imageUrl = uriImage
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).also {
                it.putExtra(EXTRA_NOTE, note)
                it.putExtra(EXTRA_POSITION, position)
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

            val values = ContentValues()
            Log.d("AddUpdateActivity", "onClick: $uriImage")
            values.put(DatabaseContract.NoteColumns.NAME, name)
            values.put(DatabaseContract.NoteColumns.DESCRIPTION, description)
            values.put(DatabaseContract.NoteColumns.EMAIL, email)
            values.put(DatabaseContract.NoteColumns.LOCATION, currentCity)
            values.put(DatabaseContract.NoteColumns.GENDER, idGender)
            values.put(DatabaseContract.NoteColumns.IMAGE_URL, uriImage)
            if (isEdit) {
                val result = noteHelper.update(note?.id.toString(), values).toLong()
                if (result > 0) {
                    setResult(RESULT_UPDATE, intent)
                    finish()
                } else {
                    Toast.makeText(this, "Gagal mengupdate data", Toast.LENGTH_SHORT).show()
                }
            } else {
                note?.date = getCurrentDate()
                values.put(DatabaseContract.NoteColumns.DATE, getCurrentDate())
                val result = noteHelper.insert(values)
                if (result > 0) {
                    note?.id = result.toInt()
                    setResult(RESULT_ADD, intent)
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menambah data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isEdit) {
            menuInflater.inflate(R.menu.menu_form, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> showAlertDialog(ALERT_DIALOG_DELETE)
            android.R.id.home -> showAlertDialog(ALERT_DIALOG_CLOSE)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        showAlertDialog(ALERT_DIALOG_CLOSE)
    }

    private fun showAlertDialog(type: Int) {
        val isDialogClose = type == ALERT_DIALOG_CLOSE
        val dialogTitle: String
        val dialogMessage: String
        if (isDialogClose) {
            dialogTitle = "Batal"
            dialogMessage = "Apakah anda ingin membatalkan perubahan pada form?"
        } else {
            dialogMessage = "Apakah anda yakin ingin menghapus item ini?"
            dialogTitle = "Hapus Note"
        }
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                if (isDialogClose) {
                    finish()
                } else {
                    val result = noteHelper.deleteById(note?.id.toString()).toLong()
                    if (result > 0) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_POSITION, position)
                        setResult(RESULT_DELETE, intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }
}