/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.posenet.poseestimate

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.net.Uri
import android.os.*
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.util.*
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.FaceDetector.ALL_CLASSIFICATIONS
import com.google.android.gms.vision.face.FaceDetector.NO_LANDMARKS
import com.google.gson.Gson
import com.huawei.hms.mlsdk.sounddect.MLSoundDectListener
import com.huawei.hms.mlsdk.sounddect.MLSoundDector

import okhttp3.*
import org.tensorflow.lite.examples.posenet.*
import org.tensorflow.lite.examples.posenet.ObjectDetect.Camera2BasicFragment.saveBitmap
import org.tensorflow.lite.examples.posenet.ObjectDetect.GpuDelegateHelper
import org.tensorflow.lite.examples.posenet.ObjectDetect.ImageClassifier
import org.tensorflow.lite.examples.posenet.ObjectDetect.ImageClassifierFloatMobileNet
import org.tensorflow.lite.examples.posenet.databinding.TfePnActivityPosenetBinding
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Person
import org.tensorflow.lite.examples.posenet.lib.Posenet
import java.io.*
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.Pair
import kotlin.math.abs
import kotlin.math.roundToInt

class PosenetActivity :
  Fragment(),
  ActivityCompat.OnRequestPermissionsResultCallback {

//  private lateinit var binding : TfePnActivityPosenetBinding

  /** List of body joints that should be connected.    */
  private val bodyJoints = listOf(
    Pair(BodyPart.LEFT_WRIST, BodyPart.LEFT_ELBOW),
    Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_SHOULDER),
    Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
    Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
    Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
    Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
    Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
    Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_SHOULDER),
    Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
    Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
    Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
    Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)
  )

  /** Threshold for confidence score. */
  private val minConfidence = 0.5

  /** Radius of circle used to draw keypoints.  */
  private val circleRadius = 8.0f

  /** Paint class holds the style and color information to draw geometries,text and bitmaps. */
  private var paint = Paint()

  /** A shape for extracting frame data.   */
  private val PREVIEW_WIDTH = 320
  private val PREVIEW_HEIGHT = 180
//  private val PREVIEW_HEIGHT = 1080
//  private val PREVIEW_WIDTH = 1920

  /** An object for the Posenet library.    */
  private lateinit var posenet: Posenet

  /** ID of the current [CameraDevice].   */
  private var cameraId: String? = null

  /** A [SurfaceView] for camera preview.   */
  private var surfaceView: SurfaceView? = null

  /** A [CameraCaptureSession] for camera preview.   */
  private var captureSession: CameraCaptureSession? = null

  /** A reference to the opened [CameraDevice].    */
  private var cameraDevice: CameraDevice? = null

  /** The [android.util.Size] of camera preview.  */
  private var previewSize: Size? = null

  /** The [android.util.Size.getWidth] of camera preview. */
  private var previewWidth = 0

  /** The [android.util.Size.getHeight] of camera preview.  */
  private var previewHeight = 0

  /** A counter to keep count of total frames.  */
  private var frameCounter = 0

  /** The [android.util.Size] of camera preview.  */
  private var previewSize2: Size? = null

  /** An IntArray to save image data in ARGB8888 format  */
  private lateinit var rgbBytes: IntArray

  private var y1 = mutableListOf(0,0,0)

  private var y2 = mutableListOf(0,0,0)

  private var y3 = mutableListOf(0,0,0)

  private var y4 = mutableListOf(0,0,0)

  private var y5 = mutableListOf(0,0,0)


  private var con = activity?.applicationContext
  var mlSoundDetector: MLSoundDector = MLSoundDector.createSoundDector()


  /** A ByteArray to save image data in YUV format  */
  private var yuvBytes = arrayOfNulls<ByteArray>(3)

  /** An additional thread for running tasks that shouldn't block the UI.   */
  private var backgroundThread: HandlerThread? = null

  /** A [Handler] for running tasks in the background.    */
  private var backgroundHandler: Handler? = null

  /** An [ImageReader] that handles preview frame capture.   */
  private var imageReader: ImageReader? = null

  /** [CaptureRequest.Builder] for the camera preview   */
  private var previewRequestBuilder: CaptureRequest.Builder? = null

  /** [CaptureRequest] generated by [.previewRequestBuilder   */
  private var previewRequest: CaptureRequest? = null

  /** A [Semaphore] to prevent the app from exiting before closing the camera.    */
  private val cameraOpenCloseLock = Semaphore(1)

  /** Whether the current camera device supports Flash or not.    */
  private var flashSupported = false

  /** Orientation of the camera sensor.   */
  private var sensorOrientation: Int? = null

  /** Abstract interface to someone holding a display surface.    */
  private var surfaceHolder: SurfaceHolder? = null

  private var textView: TextView? = null



  /** [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.   */
  private val stateCallback = object : CameraDevice.StateCallback() {

    override fun onOpened(cameraDevice: CameraDevice) {
      cameraOpenCloseLock.release()
      this@PosenetActivity.cameraDevice = cameraDevice
      createCameraPreviewSession()
    }

    override fun onDisconnected(cameraDevice: CameraDevice) {
      cameraOpenCloseLock.release()
      cameraDevice.close()
      this@PosenetActivity.cameraDevice = null
    }

    override fun onError(cameraDevice: CameraDevice, error: Int) {
      onDisconnected(cameraDevice)
      this@PosenetActivity.activity?.finish()
    }
  }

  /**
   * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
   */
  private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureProgressed(
      session: CameraCaptureSession,
      request: CaptureRequest,
      partialResult: CaptureResult
    ) {
    }

    override fun onCaptureCompleted(
      session: CameraCaptureSession,
      request: CaptureRequest,
      result: TotalCaptureResult
    ) {
    }
  }

  /**
   * Shows a [Toast] on the UI thread.
   *
   * @param text The message to show
   */
  private fun showToast(text: String) {
    val activity = activity
    activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
  }

  private var mBinding: TfePnActivityPosenetBinding? = null
  private val binding get() = mBinding!!


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    mBinding = TfePnActivityPosenetBinding.inflate(layoutInflater, container, false)
//    binding.imgCaptured.visibility = View.VISIBLE

//    binding.btnTake.setOnClickListener {
//      //surfaceview ??????
//      val bitmap = getScreenShotFromView(binding.surfaceView)
//      Log.d("??????", "$bitmap")
//      if(bitmap != null) {
//
////        val filename = "bitmap.png"
////        val stream = FileOutputStream(filename)
////        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)
////        stream.close()
////        bitmap.recycle()
////        val intent = Intent(activity,CaptureActivity::class.java)
////        intent.putExtra("image",filename)
////        startActivity(intent)
//
//        val stream = ByteArrayOutputStream()
//        val scale = (1024/(bitmap.width).toFloat()).toFloat()
//        val image_w = (bitmap.width * scale).toInt()
//        val image_h = (bitmap.height * scale).toInt()
//        var resize = Bitmap.createScaledBitmap(bitmap, image_w, image_h, true)
//        resize.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        val byteArray = stream.toByteArray()
//        val intent = Intent(activity, CaptureActivity::class.java)
//        intent.putExtra("image", byteArray)
//        Log.d("??????", "$byteArray")
//        startActivity(intent)
//      }
//    }

    //?????????????????? ??????
    mlSoundDetector.setSoundDectListener(object : MLSoundDectListener {
      override fun onSoundSuccessResult(result: Bundle) {
        val soundType = result.getInt(MLSoundDector.RESULTS_RECOGNIZED)
        if (1 <= soundType && soundType <= 12) { // ?????? ?????? soundType in 1..12 ?????????.
          if(SoundEvent.values()[soundType] == SoundEvent.BABY_CRY){
            Toast.makeText(requireActivity(),"babycry",Toast.LENGTH_LONG).show()
          }
        }
      }

      override fun onSoundFailResult(errCode: Int) {
        Toast.makeText(Activity(),"error",Toast.LENGTH_SHORT).show()
      }
    })

    return binding.root
  }
  //??????
  private fun takepicture(bitmap: Bitmap){
    Log.d("??????", "$bitmap")

//        val filename = "bitmap.png"
//        val stream = FileOutputStream(filename)
//        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)
//        stream.close()
//        bitmap.recycle()
//        val intent = Intent(activity,CaptureActivity::class.java)
//        intent.putExtra("image",filename)
//        startActivity(intent)

    val stream = ByteArrayOutputStream()
    val scale = (1024/(bitmap.width).toFloat()).toFloat()
    val image_w = (bitmap.width * scale).toInt()
    val image_h = (bitmap.height * scale).toInt()
    val resize = Bitmap.createScaledBitmap(bitmap, image_w, image_h, true)
    resize.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray = stream.toByteArray()
    val intent = Intent(activity, CaptureActivity::class.java)
    intent.putExtra("image", byteArray)
    Log.d("??????", "$byteArray")
    startActivity(intent)
  }


  private fun getScreenShotFromView(v : View): Bitmap? {
    var screenshot: Bitmap? = null
    try {
      screenshot = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight,Bitmap.Config.ARGB_8888)
      val canvas = Canvas(screenshot)
      v.draw(canvas)
    } catch (e: Exception) {
      Log.e("GFG", "fail")
    }
    return screenshot
  }

  private var gpu: String? = null
  private var cpu: String? = null
  private var nnApi: String? = null
  private var mobilenetV1Quant: String? = null
  private var mobilenetV1Float: String? = null
  private var imageview: ImageView? =null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    surfaceView = view.findViewById(R.id.surfaceView)
    imageview = view.findViewById(R.id.img_captured2)
    surfaceHolder = surfaceView!!.holder
    gpu = getString(R.string.gpu)
    cpu = getString(R.string.cpu)
    nnApi = getString(R.string.nnapi)
    mobilenetV1Quant = getString(R.string.mobilenetV1Quant)
    mobilenetV1Float = getString(R.string.mobilenetV1Float)
    textView = view.findViewById<View>(R.id.text) as TextView
//    imageview = view.findViewById(R.id.img_captured)

    val deviceStrings = ArrayList<String?>()
    val modelStrings = ArrayList<String?>()

    // Get references to widgets.
//    textureView = view.findViewById<View>(R.id.texture) as AutoFitTextureView
//    textView = view.findViewById<View>(R.id.text) as TextView
    //deviceView = (ListView) view.findViewById(R.id.device);
    // = (ListView) view.findViewById(R.id.model);

    // Build list of models
    modelStrings.add(mobilenetV1Float)
    modelStrings.add(mobilenetV1Quant)

    // Build list of devices
    val defaultModelIndex = 0
    deviceStrings.add(cpu)
    if (GpuDelegateHelper.isGpuDelegateAvailable()) {
      deviceStrings.add(gpu)
    }
    deviceStrings.add(nnApi)
    //faceDetector??? ????????? ????????????
    faceDetector = FaceDetector.Builder(this.requireContext())
      .setMode(FaceDetector.FAST_MODE)
      .setTrackingEnabled(false)
      .setLandmarkType(NO_LANDMARKS)
      .build()
    faceDetector!!.isOperational

//    val textSizePx = TypedValue.applyDimension(
//      TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
//    )
//    borderedText = BorderedText(textSizePx)
//    borderedText.setTypeface(Typeface.MONOSPACE)
//    trackingOverlay = view.findViewById<View>(R.id.tracking_overlay1) as OverlayView
//    if (trackingOverlay != null) {
//      trackingOverlay.addCallback(
//        object : DrawCallback() {
//          fun drawCallback(canvas: Canvas) {
//            val backgroundColor = Color.argb(100, 0, 0, 0)
//            canvas.drawColor(backgroundColor)
//            val lines = Vector<String>()
//            lines.add("Test intro")
//            if (faces != null) {
//              lines.add("Test " + faces!!.size())
//              for (i in 0 until faces!!.size()) {
//                val face: Face = faces!!.valueAt(i)
//                lines.add("Test $i")
//              }
//            } else {
//              lines.add("No faces")
//            }
//            borderedText.drawLines(canvas, 10, canvas.height - 10, lines)
//          }
//        })
//    }

  }

  override fun onResume() {
    super.onResume()
    //oncreateview?????? ???????????? mlsoundDetector??? ????????????
    mlSoundDetector.start(requireActivity())
    //??????????????? ??????????????? ?????????????????? ?????????
    startBackgroundThread()
  }

  override fun onStart() {
    super.onStart()
    openCamera()
    //???????????? ??????
    posenet = Posenet(this.requireContext())
  }

  override fun onPause() {
    closeCamera()
    stopBackgroundThread()
    super.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()
    mlSoundDetector.destroy()
    posenet.close()
  }

//  fun setCallbacks(
//    onSuccess: (Bundle) -> Unit = {},
//    onError: (Int) -> Unit = {}
//  ) {
//    mlSoundDetector.setSoundDectListener(object : MLSoundDectListener {
//      override fun onSoundSuccessResult(result: Bundle) {
//        val soundType = result.getInt(MLSoundDector.RESULTS_RECOGNIZED)
//        if (1 <= soundType && soundType <= 12) { // ?????? ?????? soundType in 1..12 ?????????.
//          if(SoundEvent.values()[soundType] == SoundEvent.BABY_CRY){
//            Toast.makeText(Activity(),"babycry",Toast.LENGTH_SHORT).show();
//          }
//        }
//      }
//
//      override fun onSoundFailResult(errCode: Int) {
//        onError(errCode)
//      }
//    })
//  }


  private fun requestCameraPermission() {
    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
      ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
    } else {
      requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
  }

  private fun requestAudioPermission() {
    if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
      ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (allPermissionsGranted(grantResults)) {
        ErrorDialog.newInstance(getString(R.string.tfe_pn_request_permission))
          .show(childFragmentManager, FRAGMENT_DIALOG)
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  private fun allPermissionsGranted(grantResults: IntArray) = grantResults.all {
    it == PackageManager.PERMISSION_GRANTED
  }

  /**
   * Sets up member variables related to camera.
   */
  private fun setUpCameraOutputs() {
    val activity = activity
    val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
      for (cameraId in manager.cameraIdList) {
        val characteristics = manager.getCameraCharacteristics(cameraId)

        // We don't use a front facing camera in this sample.
        val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (cameraDirection != null &&
          cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
        ) {
          continue
        }

        previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)

        imageReader = ImageReader.newInstance(
          PREVIEW_WIDTH, PREVIEW_HEIGHT,
          ImageFormat.YUV_420_888, /*maxImages*/ 2
        )

        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        previewHeight = previewSize!!.height
        previewWidth = previewSize!!.width

        // Initialize the storage bitmaps once when the resolution is known.
        rgbBytes = IntArray(previewWidth * previewHeight)

        // Check if the flash is supported.
        flashSupported =
          characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

        this.cameraId = cameraId


        // We've found a viable camera and finished setting up member variables,
        // so we don't need to iterate through other available cameras.
        return
      }
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    } catch (e: NullPointerException) {
      // Currently an NPE is thrown when the Camera2API is used but not supported on the
      // device this code runs.
      ErrorDialog.newInstance(getString(R.string.tfe_pn_camera_error))
        .show(childFragmentManager, FRAGMENT_DIALOG)
    }
  }


  /**
   * Opens the camera specified by [PosenetActivity.cameraId].
   */

  private fun openCamera() {
    val permissionCamera = requireContext().checkPermission(
      Manifest.permission.CAMERA, Process.myPid(), Process.myUid()
    )
    if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
      requestCameraPermission()
    }
    setUpCameraOutputs()
    val manager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
      // Wait for camera to open - 2.5 seconds is sufficient
      if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw RuntimeException("Time out waiting to lock camera opening.")
      }
      manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    } catch (e: InterruptedException) {
      throw RuntimeException("Interrupted while trying to lock camera opening.", e)
    }
  }

  /**
   * Closes the current [CameraDevice].
   */

  private fun startdldetect(){
    Toast.makeText(requireContext(),"start",Toast.LENGTH_LONG).show()
    mlSoundDetector.start(requireActivity())

  }
  private fun closeCamera() {
    if (captureSession == null) {
      return
    }

    try {
      cameraOpenCloseLock.acquire()
      captureSession!!.close()
      captureSession = null
      cameraDevice!!.close()
      cameraDevice = null
      imageReader!!.close()
      imageReader = null
    } catch (e: InterruptedException) {
      throw RuntimeException("Interrupted while trying to lock camera closing.", e)
    } finally {
      cameraOpenCloseLock.release()
    }
  }

  /**
   * Starts a background thread and its [Handler].
   */
  private fun startBackgroundThread() {
    backgroundThread = HandlerThread("imageAvailableListener").also { it.start() }
    backgroundHandler = Handler(backgroundThread!!.looper)
    updateActiveModel()
  }

  /**
   * Stops the background thread and its [Handler].
   */
  private fun stopBackgroundThread() {
    backgroundThread?.quitSafely()
    try {
      backgroundThread?.join()
      backgroundThread = null
      backgroundHandler = null
    } catch (e: InterruptedException) {
      Log.e(TAG, e.toString())
    }
  }

  /** Fill the yuvBytes with data from image planes.   */
  private fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
    // Row stride is the total number of bytes occupied in memory by a row of an image.
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (i in planes.indices) {
      val buffer = planes[i].buffer
      if (yuvBytes[i] == null) {
        yuvBytes[i] = ByteArray(buffer.capacity())
      }
      buffer.get(yuvBytes[i]!!)
    }
  }

  var switching = 1
  /** A [OnImageAvailableListener] to receive frames as they are available.  */
  private var imageAvailableListener = object : OnImageAvailableListener {
    override fun onImageAvailable(imageReader: ImageReader) {
      // We need wait until we have some size from onPreviewSizeChosen
      if (previewWidth == 0 || previewHeight == 0) {
        return
      }

      val image = imageReader.acquireLatestImage() ?: return
      fillBytes(image.planes, yuvBytes)

      ImageUtils.convertYUV420ToARGB8888(
        yuvBytes[0]!!,
        yuvBytes[1]!!,
        yuvBytes[2]!!,
        previewWidth,
        previewHeight,
        /*yRowStride=*/ image.planes[0].rowStride,
        /*uvRowStride=*/ image.planes[1].rowStride,
        /*uvPixelStride=*/ image.planes[1].pixelStride,
        rgbBytes
      )

      // Create bitmap from int array
      val imageBitmap = Bitmap.createBitmap(
        rgbBytes, previewWidth, previewHeight,
        Bitmap.Config.ARGB_8888
      )
//  private val PREVIEW_HEIGHT = 1080
//  private val PREVIEW_WIDTH = 1920
//      val imageBitmap2 = Bitmap.createBitmap(
//        rgbBytes, 1920, 1080,
//        Bitmap.Config.ARGB_8888
//      )

      // Create rotated version for portrait display
      val rotateMatrix = Matrix()
      rotateMatrix.postRotate(90.0f)

      val rotatedBitmap = Bitmap.createBitmap(
        imageBitmap, 0, 0, previewWidth, previewHeight,
        rotateMatrix, true
      )
      Log.i(TAG, "size " + previewSize!!.height/2 + " " + previewSize!!.width/2)
      image.close()

      //classifyFrame() bitmap????????? facedetect?????????
      classifyFrame(rotatedBitmap)
      processImage(rotatedBitmap)
      binding.btnTake.setOnClickListener {
        if(switching == 1){
          Log.d("done","done")
          val intent = Intent(requireActivity(), org.tensorflow.lite.examples.posenet.ObjectDetect.CameraActivity::class.java)
          ActivityCompat.finishAffinity(requireActivity())
          requireActivity().overridePendingTransition(0,0)
          activity?.finish()
          startActivity(intent)
//          onDestroy()
          requireActivity().overridePendingTransition(0,0)
          switching = 0
        }else{
          Log.d("start","start")

          onPause()
          switching = 1
        }

//      takepicture(scaledBitmap)

//        Log.d("working?","working")
//        takeScreenshot(bitmap)
//        startdldetect()
      }
//      processImage(rotatedBitmap)

    }
  }

  private fun handlerTest(bitmap: Bitmap){
    val handler: Handler = object :Handler(Looper.getMainLooper()){
      override fun handleMessage(msg: Message) {
        val ob = BitmapDrawable(resources,bitmap)
        Log.d("handlermessage","done")
        imageview!!.background = ob
      }
    }
    handler.obtainMessage().sendToTarget()
  }

  private var classifier: ImageClassifier2? = null
  private var faces: SparseArray<Face>? = null
  private var faceDetector: FaceDetector? =null

  private fun classifyFrame(bitmap: Bitmap){
    if(classifier == null || activity == null || cameraDevice == null){
      return
    }

    val textToShow = SpannableStringBuilder()
    val frame: Frame = Frame.Builder().setBitmap(bitmap).build()
    faces = faceDetector!!.detect(frame)
    Log.d("faces",faces!!.size().toString())

    for(i in 0 until faces!!.size()){
      val face: Face = faces!!.valueAt(i)
      val rect = Rect(
        Math.round(face.position.x),
        Math.round(face.position.y),
        Math.round(face.width + face.position.x),
        Math.round(face.position.y + face.height)
      )

      assert(rect.left < rect.right && rect.top < rect.bottom)
      //  Create our resulting image (150--50),(75--25) = 200x100px
      val resultBmp = Bitmap.createBitmap(
        rect.right - rect.left,
        rect.bottom - rect.top,
        Bitmap.Config.ARGB_8888
      )


      //  draw source bitmap into resulting image at given position:
      Canvas(resultBmp).drawBitmap(bitmap, -rect.left.toFloat(), -rect.top.toFloat(), null)
      val resized = Bitmap.createScaledBitmap(resultBmp, 48, 48, true)
//      handlerTest(resized)
      val runnable = Runnable {
        kotlin.run {
          saveBitmap(resized, "preview.png")
        }
      }



      classifier!!.classifyFrame(resized,textToShow)
      Log.i(TAG, "faceid " + face.getPosition().x)
      Log.i(TAG, "faceid " + face.getPosition().y)
      break
    }
    Log.i(TAG,"facelen"+faces!!.size())
    showToast2(textToShow)

  }

  var currentNumThreads = 10

  private fun updateActiveModel() {
    // Get UI information before delegating to background
    //final int modelIndex = modelView.getCheckedItemPosition();
    //final int deviceIndex = deviceView.getCheckedItemPosition();
    val numThreads = 10
    backgroundHandler!!.post {
      currentNumThreads = numThreads

      // Disable classifier while updating

      if (classifier != null) {
        classifier!!.close()
        classifier = null
      }

      // Lookup names of parameters.
      val model = mobilenetV1Float
      val device = cpu
      Log.i(TAG, "Changing model to $model device $device")

      // Try to load model.
      /*if (model.equals(mobilenetV1Quant)) {
  classifier = new ImageClassifierQuantizedMobileNet(getActivity());
} else */if (model == mobilenetV1Float) {
      classifier = ImageClassifierFloatMobileNet2(activity)
      Log.i("correctloade", "correctload")
    } else {
      showToast("Failed to load model")
    }

      // Customize the interpreter to the type of device we want to use.
//            if (classifier == null) {
//                return@post
//            }
      classifier!!.setNumThreads(numThreads)
      if (device == cpu) {
      } else if (device == gpu) {
        if (!GpuDelegateHelper.isGpuDelegateAvailable()) {
          showToast("gpu not in this build.")
//                    classifier
        } else if (model == mobilenetV1Quant) {
          showToast("gpu requires float model.")
//                    classifier
        } else {
          classifier!!.useGpu()
        }
      } /*else if (device.equals(nnApi)) {
        classifier.useNNAPI();
      }*/
    }
  }

  /** Crop Bitmap to maintain aspect ratio of model input.   */
  private fun cropBitmap(bitmap: Bitmap): Bitmap {
    val bitmapRatio = bitmap.height.toFloat() / bitmap.width
    val modelInputRatio = MODEL_HEIGHT.toFloat() / MODEL_WIDTH
    var croppedBitmap = bitmap

    // Acceptable difference between the modelInputRatio and bitmapRatio to skip cropping.
    val maxDifference = 1e-5

    // Checks if the bitmap has similar aspect ratio as the required model input.
    when {
      abs(modelInputRatio - bitmapRatio) < maxDifference -> return croppedBitmap
      modelInputRatio < bitmapRatio -> {
        // New image is taller so we are height constrained.
        val cropHeight = bitmap.height - (bitmap.width.toFloat() / modelInputRatio)
        croppedBitmap = Bitmap.createBitmap(
          bitmap,
          0,
          (cropHeight / 2).toInt(),
          bitmap.width,
          (bitmap.height - cropHeight).toInt()
        )
      }
      else -> {
        val cropWidth = bitmap.width - (bitmap.height.toFloat() * modelInputRatio)
        croppedBitmap = Bitmap.createBitmap(
          bitmap,
          (cropWidth / 2).toInt(),
          0,
          (bitmap.width - cropWidth).toInt(),
          bitmap.height
        )
      }
    }
    return croppedBitmap
  }

  /**
   * Shows a [Toast] on the UI thread for the classification results.
   *
   * @param s The message to show
   */
  private fun showToast2(s: String) {
    val builder = SpannableStringBuilder()
    val str1 = SpannableString(s)
    builder.append(str1)
    showToast2(builder)
  }

  private fun showToast2(builder:SpannableStringBuilder){
    val activity = activity
    activity?.runOnUiThread { textView!!.setText(builder,TextView.BufferType.SPANNABLE) }
  }

  /** Set the paint color and size.    */
  private fun setPaint() {
    paint.color = Color.WHITE
    paint.textSize = 80.0f
    paint.strokeWidth = 8.0f
  }

  /** Draw bitmap on Canvas.   */
  private fun draw(canvas: Canvas, person: Person, bitmap: Bitmap) {
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    // Draw `bitmap` and `person` in square canvas.
    val screenWidth: Int
    val screenHeight: Int
    val left: Int
    val right: Int
    val top: Int
    val bottom: Int
    if (canvas.height > canvas.width) {
      screenWidth = canvas.width
      screenHeight = canvas.width
      left = 0
      top = (canvas.height - canvas.width) / 2
    } else {
      screenWidth = canvas.height
      screenHeight = canvas.height
      left = (canvas.width - canvas.height) / 2
      top = 0
    }
    right = left + screenWidth
    bottom = top + screenHeight

    setPaint()
    canvas.drawBitmap(
      bitmap,
      Rect(0, 0, bitmap.width, bitmap.height),
      Rect(left, top, right, bottom),
      paint
    )

    val widthRatio = screenWidth.toFloat() / MODEL_WIDTH
    val heightRatio = screenHeight.toFloat() / MODEL_HEIGHT


    // Draw key points over the image.
    for (keyPoint in person.keyPoints) {
      if (keyPoint.score > minConfidence) {
        var check = true
        val position = keyPoint.position
        val adjustedX: Float = position.x.toFloat() * widthRatio + left
        val adjustedY: Float = position.y.toFloat() * heightRatio + top
        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint)

        if(keyPoint.bodyPart.toString()== "RIGHT_EAR") {
          y1.add(0, position.y)
          println(position.y)
          println(y1[1])
          if (position.y - y1[1]   > 20 && check && y1[1]!=0) {
            check =false
            canvas.drawText(
              "falldetect",
              (15.0f * widthRatio),
              (30.0f * heightRatio + bottom),
              paint
            )
//            println("falldetected")
//            takeScreenshot(bitmap)
          }
        }

        if(keyPoint.bodyPart.toString()== "LEFT_EAR") {
          y2.add(0, position.y)
          Log.d("Left_Ear",Integer.toString(y2[2])+ "-"+position.y);
          println(position.y)
          println(y2[1])
          if (position.y - y2[1]   > 20 && check && y2[1]!=0) {
            check =false
            canvas.drawText(
              "falldetect",
              (15.0f * widthRatio),
              (30.0f * heightRatio + bottom),
              paint
            )
//            println("falldetected")
//            takeScreenshot(bitmap)
          }
        }

        if(keyPoint.bodyPart.toString()== "RIGHT_EYE") {
          y3.add(0, position.y)
          println(position.y)
          println(y3[2])
          if (position.y - y3[1]   > 20 && check && y3[1]!=0) {
            check =false
            canvas.drawText(
              "falldetect",
              (15.0f * widthRatio),
              (30.0f * heightRatio + bottom),
              paint
            )
//            println("falldetected")
//            takeScreenshot(bitmap)
          }
        }

        if(keyPoint.bodyPart.toString()=="LEFT_EYE") {
          y4.add(0, position.y)
          println(position.y)
          println(y4[2])
          if (position.y - y4[1]   > 20 && check && y4[1]!=0) {
            check =false
            canvas.drawText(
              "falldetect",
              (15.0f * widthRatio),
              (30.0f * heightRatio + bottom),
              paint
            )
//            println("falldetected")
//            takeScreenshot(bitmap)

          }
        }

        if(keyPoint.bodyPart.toString()=="NOSE"){
          y5.add(0,position.y)
          Log.d("list_nose1", y5[0].toString())
          Log.d("list_nose2", y5[1].toString())
          Log.d("list_nose3", y5[2].toString())
          if(position.y - y5[1]   > 20 && check && y5[1]!=0){
            Log.d("list_nose_falldetect","detect")
            canvas.drawText(
              "falldetected",
              (15.0f * widthRatio),
              (30.0f * heightRatio + bottom),
              paint
            )
//            println("falldetected")
//            takeScreenshot(bitmap)

          }
        }

      }

    }


    for (line in bodyJoints) {
      if (
        (person.keyPoints[line.first.ordinal].score > minConfidence) and
        (person.keyPoints[line.second.ordinal].score > minConfidence)
      ) {
        canvas.drawLine(
          person.keyPoints[line.first.ordinal].position.x.toFloat() * widthRatio + left,
          person.keyPoints[line.first.ordinal].position.y.toFloat() * heightRatio + top,
          person.keyPoints[line.second.ordinal].position.x.toFloat() * widthRatio + left,
          person.keyPoints[line.second.ordinal].position.y.toFloat() * heightRatio + top,
          paint
        )
      }
    }
    /*
    canvas.drawText(
      "Score: %.2f".format(person.score),
      (15.0f * widthRatio),
      (30.0f * heightRatio + bottom),
      paint
    )
    canvas.drawText(
      "Device: %s".format(posenet.device),
      (15.0f * widthRatio),
      (50.0f * heightRatio + bottom),
      paint
    )
    canvas.drawText(
      "Time: %.2f ms".format(posenet.lastInferenceTimeNanos * 1.0f / 1_000_000),
      (15.0f * widthRatio),
      (70.0f * heightRatio + bottom),
      paint
    )*/

    // Draw!

    surfaceHolder!!.unlockCanvasAndPost(canvas)
  }

  /** Process image using Posenet library.   */
  private fun processImage(bitmap: Bitmap) {
    // Crop bitmap.
    val croppedBitmap = cropBitmap(bitmap)

    // Created scaled version of bitmap for model input.
    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, MODEL_WIDTH, MODEL_HEIGHT, true)

    // Perform inference.
    val person = posenet.estimateSinglePose(scaledBitmap)
    val canvas: Canvas = surfaceHolder!!.lockCanvas()
    draw(canvas, person, scaledBitmap)

  }


  /**
   * Creates a new [CameraCaptureSession] for camera preview.
   */
  private fun createCameraPreviewSession() {
    try {
      // We capture images from preview in YUV format.
      imageReader = ImageReader.newInstance(
        previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2
      )


      imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

      // This is the surface we need to record images for processing.
      val recordingSurface = imageReader!!.surface

      // We set up a CaptureRequest.Builder with the output Surface.
      previewRequestBuilder = cameraDevice!!.createCaptureRequest(
        CameraDevice.TEMPLATE_PREVIEW
      )
      previewRequestBuilder!!.addTarget(recordingSurface)

      // Here, we create a CameraCaptureSession for camera preview.
      cameraDevice!!.createCaptureSession(
        listOf(recordingSurface),
        object : CameraCaptureSession.StateCallback() {
          override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            // The camera is already closed
            if (cameraDevice == null) return

            // When the session is ready, we start displaying the preview.
            captureSession = cameraCaptureSession
            try {
              // Auto focus should be continuous for camera preview.
              previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
              )
              // Flash is automatically enabled when necessary.
              setAutoFlash(previewRequestBuilder!!)

              // Finally, we start displaying the camera preview.
              previewRequest = previewRequestBuilder!!.build()
              captureSession!!.setRepeatingRequest(
                previewRequest!!,
                captureCallback, backgroundHandler
              )
            } catch (e: CameraAccessException) {
              Log.e(TAG, e.toString())
            }
          }

          override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            showToast("Failed")
          }
        },
        null
      )
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())

    }

  }

  private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
    if (flashSupported) {
      requestBuilder.set(
        CaptureRequest.CONTROL_AE_MODE,
        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
      )
    }
  }

  /**
   * Shows an error message dialog.
   */
  class ErrorDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
      AlertDialog.Builder(activity)
        .setMessage(requireArguments().getString(ARG_MESSAGE))
        .setPositiveButton(android.R.string.ok) { _, _ -> requireActivity().finish() }
        .create()

    companion object {

      @JvmStatic
      private val ARG_MESSAGE = "message"

      @JvmStatic
      fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
        arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
      }
    }
  }

  companion object {
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private val ORIENTATIONS = SparseIntArray()
    private val FRAGMENT_DIALOG = "dialog"

    init {
      ORIENTATIONS.append(Surface.ROTATION_0, 90)
      ORIENTATIONS.append(Surface.ROTATION_90, 0)
      ORIENTATIONS.append(Surface.ROTATION_180, 270)
      ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    /**
     * Tag for the [Log].
     */
    private const val TAG = "PosenetActivity"
  }
  /** Compares two `Size`s based on their areas.  */
  private class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
      // We cast here to ensure the multiplications won't overflow
      return java.lang.Long.signum(
        lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height
      )
    }
  }



  //  private fun takeScreenshot(bitmap: Bitmap) {
//    firebaseStorage = FirebaseStorage.getInstance()//?????????????????? ???????????? ????????????
//    val now = Date()
//    DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
//    try {
//      // image naming and path  to include sd card  appending name you choose for file
//      val mPath =
//        Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"
//
//      Log.d("takescreenshot","screenshot")
//      // create bitmap screen capture
//      val imageFile = File(mPath)
//      val outputStream = FileOutputStream(imageFile)
//      val quality = 100
//      val name:String = "$now.jpg"
//
//      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
//      outputStream.flush()
//      outputStream.close()
////      uploadUri(imageFile,name)
////      FcmPush().sendMessage("????????? ??????",name)
//
//      openScreenshot(imageFile)
//    } catch (e: Throwable) {
//      // Several error may come out with file handling or DOM
//      e.printStackTrace()
//    }
//  }
//  private fun openScreenshot(imageFile: File) {
//    Log.d("openscreenshot","openscreenshot")
////    val intent = Intent()
////    intent.action = Intent.ACTION_VIEW
//    val uri: Uri = Uri.fromFile(imageFile)
//    val intent = Intent(activity, CaptureActivity::class.java)
////    intent.putExtra("image", byteArray)
////    Log.d("??????", "$byteArray")
//    intent.putExtra("uri",uri)
//    startActivity(intent)
////    intent.setDataAndType(uri, "image/*")
////    startActivity(intent)
//  }

//  private fun uploadUri(imageFile:File,name:String){
//    //val storageRef = firebaseStorage.getReferenceFromUrl("gs://village-e124b.appspot.com/")
//    val file = Uri.fromFile(imageFile)
///*
//    val reversRef = storageRef.child("image/+$name")
//    var uploadTask = reversRef.putFile(file)
//    uploadTask.addOnSuccessListener {
//      showToast("????????? ??????")
//    }.addOnFailureListener {
//      showToast("??????")
//    }*/
//
//    val storageRef: StorageReference = firebaseStorage.getReferenceFromUrl("gs://village-e124b.appspot.com/").child("image/+$name")
//    storageRef.putFile(file).addOnCompleteListener{
//      if(it.isSuccessful){
//        showToast("????????? ??????")
//      }
//    }
//  }

//  data class PushDTO(
//    var to: String? = null,                             //PushToken??? ???????????? ?????? ????????? ?????? ?????????
//    var notification: Notification? = Notification()    //??????????????? ?????? ???????????? ??????
//  ) {
//    data class Notification(
//      var body: String? = null,                       //??????????????? ?????? ????????? ??????
//      var title: String? = null                       //??????????????? ?????? ?????????
//    )
//  }
//  class FcmPush() {
//    val JSON = MediaType.parse("application/json; charset=utf-8")//Post?????? JSON Type
//    val url = "https://fcm.googleapis.com/fcm/send" //FCM HTTP??? ???????????? URL
//    val serverKey =
//      "AAAAL5ziR7c:APA91bHdiavlv4yVze2eF6YXRbWF_hfbzUu7ZhFPU_cI4abn6WSkLaRaU6RliTr_LJ8UjMYw5k3VOfPMx3WACXJ3_U7reO07Y1_ma8NgkS_Wp4lmG2-SdxvvhA6vM7sitPWEIYHpuBAW"
//    //Firebase?????? ????????? ?????????
//    var okHttpClient: OkHttpClient
//    var gson: Gson
//
//    init {
//      gson = Gson()
//
//      okHttpClient = OkHttpClient()
//    }
//
//    fun sendMessage(title: String, message: String) {
//      val token = "c5dwlA83QDulINiG3WLYws:APA91bH8RX8EKdp_vnpWTSI2i_eiBie7koXP0vY2-uAiy-Qej3-kY6VMb2F3u5dRnhZxRCj5KcrwcjRYef_lQPCNzc-IfDe3NDaJ7nCKtupQDXgHxkYSi44tumohacYsIKtz6mOiKD16"
//      Log.i("????????????", token)
//      val pushDTO = PushDTO()
//      pushDTO.to = token                   //???????????? ??????
//      pushDTO.notification?.title = title  //?????? ????????? ??????
//      pushDTO.notification?.body = message //?????? ????????? ??????
//      val body = RequestBody.create(JSON, gson.toJson(pushDTO)!!)
//      val request = Request
//        .Builder()
//        .addHeader("Content-Type", "application/json")
//        .addHeader("Authorization", "key=" + serverKey)
//        .url(url)       //?????? URL ??????
//        .post(body)     //pushDTO??? ?????? body ??????
//        .build()
//      okHttpClient.newCall(request)?.enqueue(object : Callback {//?????? ??????
//      override fun onFailure(call: Call?, e: IOException?) {
//      }
//        override fun onResponse(call: Call?, response: Response?) {
//          println(response?.body()?.string()) //????????? ???????????? ?????? ????????? ??????
//
//        }
//      })
//    }
//  }
}
