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
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.examples.posenet.ObjectDetect.Camera2BasicFragment
import org.tensorflow.lite.examples.posenet.R

class CameraActivity : AppCompatActivity() {

  //퍼미션 응답 처리 코드
  private val multiplePermissionsCode = 100

  // 필요한 퍼미션 리스트
  // 원하는 퍼미션을 이곳에 추가
  private val requiredPermissions = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.INTERNET
  )
  private val RC_SIGN_IN = 9001

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.tfe_pn_activity_camera)
    checkPermissions()
    savedInstanceState ?: supportFragmentManager.beginTransaction()
      .replace(R.id.container,PosenetActivity())
      .commit()
  }

  private fun checkPermissions() {
    //거절되었거나 아직 수락하지 않은 권한을 저장할 문자열 배열 리스트
    val rejectedPermissionList = ArrayList<String> ()

    for(permission in requiredPermissions) {
      if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        // 만약 권한이 없다면 rejectedPermissionList에 추가
        rejectedPermissionList.add(permission)
      }
    }

    // 거절된 퍼미션이 있다면
    if(rejectedPermissionList.isNotEmpty()) {
      // 권한 요청
      val array = arrayOfNulls<String>(rejectedPermissionList.size)
      ActivityCompat.requestPermissions(this, rejectedPermissionList.toArray(array), multiplePermissionsCode)
    }
  }

  // 권한 요청 결과 함수
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    when(requestCode) {
      multiplePermissionsCode -> {
        if(grantResults.isNotEmpty()) {
          for((i, permission) in permissions.withIndex()) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
              // 권한 획득 실패
              Log.i("TAG", "The user has denied to $permission")
            }
          }
        }
      }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }
}
