package org.tensorflow.lite.examples.posenet.poseestimate

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.posenet.databinding.ActivityCaptureBinding

class CaptureActivity: AppCompatActivity() {
    private lateinit var binding : ActivityCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
//        val filename = intent.getStringExtra("image")
//        val iss = openFileInput(filename)
//        val bmp = BitmapFactory.decodeStream(iss)
//        iss.close()
//        binding.imageView.setImageBitmap(bmp)
        val urivalue = intent.getParcelableExtra<Uri>("uri")

//        val byteArray = intent.getByteArrayExtra("image")
//        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        Log.d("캡처", urivalue.toString())
        binding.imageView.setImageURI(urivalue)
//        binding.imageView.setImageBitmap(bitmap)
    }
}