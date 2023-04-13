// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.iristracking

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.protobuf.InvalidProtocolBufferException

/** Main activity of MediaPipe iris tracking app.  */
class MainActivity : BaseActivity() {
    private var haveAddedSidePackets = false
    override fun onCameraStarted(surfaceTexture: SurfaceTexture?) {
        super.onCameraStarted(surfaceTexture)

        // onCameraStarted gets called each time the activity resumes, but we only want to do this once.
        if (!haveAddedSidePackets) {
            val focalLength = cameraHelper!!.focalLengthPixels
            if (focalLength != Float.MIN_VALUE) {
                val focalLengthSidePacket = processor!!.packetCreator.createFloat32(focalLength)
                val inputSidePackets: MutableMap<String, Packet> = HashMap()
                inputSidePackets[FOCAL_LENGTH_STREAM_NAME] = focalLengthSidePacket
                processor!!.setInputSidePackets(inputSidePackets)
            }
            haveAddedSidePackets = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            processor!!.addPacketCallback(
                    OUTPUT_LANDMARKS_STREAM_NAME
            ) { packet: Packet ->
                val landmarksRaw = PacketGetter.getProtoBytes(packet)
                try {
                    val landmarks = NormalizedLandmarkList.parseFrom(landmarksRaw)
                    if (landmarks == null) {
                        Log.v(TAG, "[TS:" + packet.timestamp + "] No landmarks.")
                        return@addPacketCallback
                    }
                    Log.v(
                            TAG,
                            "[TS:"
                                    + packet.timestamp
                                    + "] #Landmarks for face (including iris): "
                                    + landmarks.landmarkCount)
                    Log.v(TAG, getLandmarksDebugString(landmarks))
                } catch (e: InvalidProtocolBufferException) {
                    Log.e(TAG, "Couldn't Exception received - $e")
                    return@addPacketCallback
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val FOCAL_LENGTH_STREAM_NAME = "focal_length_pixel"
        private const val OUTPUT_LANDMARKS_STREAM_NAME = "face_landmarks_with_iris"
        private fun getLandmarksDebugString(landmarks: NormalizedLandmarkList): String {
            var landmarkIndex = 0
            var landmarksString = ""
            for (landmark in landmarks.landmarkList) {
                landmarksString += """		Landmark[$landmarkIndex]: (${landmark.x}, ${landmark.y}, ${landmark.z})
"""
                ++landmarkIndex
            }
            return landmarksString
        }
    }
}