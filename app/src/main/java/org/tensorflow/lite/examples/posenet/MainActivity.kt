package org.tensorflow.lite.examples.posenet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.tensorflow.lite.examples.posenet.databinding.ActivityMainBinding
import org.tensorflow.lite.examples.posenet.poseestimate.CameraActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        checkMicrophonePermission()
        val view = binding.root
        setContentView(view)

        binding.btnBack3.setOnClickListener {
            val intent = Intent(this, SelectActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnGocamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
    }
//    private fun checkMicrophonePermission() {
//        if (ActivityCompat.checkSelfPermission(
//                Activity(),
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                Activity(), arrayOf(
//                    Manifest.permission.RECORD_AUDIO
//                ), 0x123
//            )
//        }
//    }
}