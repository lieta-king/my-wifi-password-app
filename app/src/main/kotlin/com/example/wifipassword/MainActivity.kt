package com.example.wifipassword

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder

class MainActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiInfo: WifiInfo
    private lateinit var qrImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        qrImageView = findViewById(R.id.qrImageView)
        val generateButton = findViewById<Button>(R.id.generateButton)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiInfo = wifiManager.connectionInfo

        generateButton.setOnClickListener {
            val ssid = wifiInfo.ssid.replace("\"", "")
            val password = getWifiPassword(ssid)
            if (password.isNotEmpty()) {
                generateQRCode("$ssid:$password")
            } else {
                Log.e("MainActivity", "Failed to get Wi-Fi password")
            }
        }
    }

    private fun getWifiPassword(ssid: String): String {
        val command = "cmd /c netsh wlan show profile name=\"$ssid\" key=clear"
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val inputStream = process.inputStream
        val scanner = Scanner(inputStream).useDelimiter("\\A")
        val result = if (scanner.hasNext()) scanner.next() else ""
        val lines = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            if (line.contains("Key Content")) {
                val start = line.indexOf(":") + 1
                return line.substring(start).trim()
            }
        }
        return ""
    }

    private fun generateQRCode(content: String) {
        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            qrImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
