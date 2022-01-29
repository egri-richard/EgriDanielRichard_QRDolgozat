package com.school.qrcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.school.qrcodescanner.databinding.ActivityMainBinding
import com.vmadalin.easypermissions.EasyPermissions
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var latitude: String = ""
    private var longitude: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        writeScanResult()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnShowCoords.setOnClickListener {
            getCoords()
        }

        binding.btnScan.setOnClickListener {
            launchQrScanner()
        }

        binding.btnWrite.setOnClickListener {
            writeToStorage()
        }
    }

    //Ha nem működik akkor nyissák meg a google maps-et az emulátoron,
    //mert a fusedLocationProvider lastLocation property-e csak akkor frissul
    //ha egy másik app(PL.: google maps) felismeri hogy frissült a helyzetünk
    @SuppressLint("MissingPermission")
    private fun getCoords() {
        if (EasyPermissions.hasPermissions(this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                latitude = it.latitude.round(2).toString()
                longitude = it.longitude.round(2).toString()

                binding.tvResult.text = latitude.plus(", ").plus(longitude)

                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap("$longitude, $latitude", BarcodeFormat.QR_CODE, 512, 512)

                binding.ivShowQrCode.setImageBitmap(bitmap)
            }
        } else {
            EasyPermissions.requestPermissions(
                this,
                "please allow location access",
                2,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    private fun writeScanResult() {
        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val result = IntentIntegrator.parseActivityResult(it.resultCode, it.data)

                if (result.contents != null) {
                    binding.tvResult.text = result.contents.toString()

                    if (result.toString().contains("http")) {
                        val alert = AlertDialog.Builder(this)
                        alert.setTitle("Szeretné megnyitni a beolvasott url-t?")
                        alert.setPositiveButton("Igen") { _, _ ->
                            val url = result.toString()
                            var webpage = Uri.parse(url)

                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                webpage = Uri.parse("http://$url")
                            }

                            startActivity(Intent(Intent.ACTION_VIEW, webpage))
                        }
                        alert.setNegativeButton("Nem") {_,_ ->}
                        alert.show()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun writeToStorage() {
        val name = "scannedCodes"

        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val formatted = now.format(formatter)

        getCoords()
        val coords = latitude.plus(", ").plus(longitude)

        val data = "${binding.tvResult.text.toString()}, Dátum: $formatted, Koordináták: $coords"

        val fileOutputStream: FileOutputStream

        try {
            fileOutputStream = openFileOutput(name, MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
            Toast.makeText(applicationContext,"Kiírva",Toast.LENGTH_SHORT).show()
            //binding.tvSaveLocation.text = this.filesDir.absolutePath
        } catch (e: FileNotFoundException){
            e.printStackTrace()
            Toast.makeText(applicationContext,"Hiba kiíráskor",Toast.LENGTH_SHORT).show()
        }catch (e: NumberFormatException){
            e.printStackTrace()
            Toast.makeText(applicationContext,"Hiba kiíráskor",Toast.LENGTH_SHORT).show()
        }catch (e: IOException){
            e.printStackTrace()
            Toast.makeText(applicationContext,"Hiba kiíráskor",Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(applicationContext,"Hiba kiíráskor",Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchQrScanner() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            val scanner = IntentIntegrator(this)
            scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            scanner.setPrompt("scan")
            scannerLauncher.launch(scanner.createScanIntent())
        } else {
            EasyPermissions.requestPermissions(
                this,
                "please allow this app to use the camera",
                2,
                Manifest.permission.CAMERA
            )
        }
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

}