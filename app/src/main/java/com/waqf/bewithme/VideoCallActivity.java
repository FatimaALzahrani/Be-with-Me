//package com.waqf.bewithme;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.view.SurfaceView;
//import android.widget.FrameLayout;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import io.agora.rtc2.Constants;
//import io.agora.rtc2.IRtcEngineEventHandler;
//import io.agora.rtc2.RtcEngine;
//import io.agora.rtc2.RtcEngineConfig;
//import io.agora.rtc2.video.VideoCanvas;
//import io.agora.rtc2.ChannelMediaOptions;
//import io.agora.rtc2.video.VideoEncoderConfiguration;
//
//public class VideoCallActivity extends AppCompatActivity {
//
//    private RtcEngine rtcEngine;
//    private SurfaceView localView;
//    private SurfaceView remoteView;
//    private static final String APP_ID = "7d65353c605e4c9e8b6a856ccf831077";
//    private static final String CHANNEL_NAME = "test_channel";
//    private static final int PERMISSION_REQ_ID = 22;
//    private static final String[] REQUESTED_PERMISSIONS = {
//            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.CAMERA,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_video_call);
//
//        // Check for permissions
//        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
//                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
//                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
//            initializeAgoraEngine();
//        }
//    }
//
//    private boolean checkSelfPermission(String permission, int requestCode) {
//        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQ_ID) {
//            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
//                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
//                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Permissions needed for video call", Toast.LENGTH_LONG).show();
//                finish();
//            } else {
//                initializeAgoraEngine();
//            }
//        }
//    }
//
//    private void initializeAgoraEngine() {
//        try {
//            RtcEngineConfig config = new RtcEngineConfig();
//            config.mContext = getApplicationContext();
//            config.mAppId = APP_ID;
//            config.mEventHandler = mRtcEventHandler;
//            rtcEngine = RtcEngine.create(config);
//
//            setupVideoConfig();
//            setupLocalVideo();
//            joinChannel();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void setupVideoConfig() {
//        rtcEngine.enableVideo();
//        VideoEncoderConfiguration config = new VideoEncoderConfiguration(
//                VideoEncoderConfiguration.VD_640x480,
//                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
//                VideoEncoderConfiguration.STANDARD_BITRATE,
//                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
//        );
//        rtcEngine.setVideoEncoderConfiguration(config);
//    }
//
//    private void setupLocalVideo() {
//        SurfaceView localView = RtcEngine.CreateRendererView(getBaseContext());
//        FrameLayout localContainer = findViewById(R.id.local_video_view_container);
//        localContainer.addView(localView);
//        rtcEngine.setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
//    }
//
//    private void joinChannel() {
//        ChannelMediaOptions options = new ChannelMediaOptions();
//        options.autoSubscribeAudio = true;
//        options.autoSubscribeVideo = true;
//
//        rtcEngine.joinChannel(null, CHANNEL_NAME, "Extra Optional Data", 0, options);
//    }
//
//    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
//        @Override
//        public void onUserJoined(int uid, int elapsed) {
//            runOnUiThread(() -> setupRemoteVideo(uid));
//        }
//
//        @Override
//        public void onUserOffline(int uid, int reason) {
//            runOnUiThread(() -> removeRemoteVideo());
//        }
//    };
//
//    private void setupRemoteVideo(int uid) {
//        remoteView = RtcEngine.CreateRendererView(getBaseContext());
//        FrameLayout remoteContainer = findViewById(R.id.remote_video_view_container);
//        remoteContainer.addView(remoteView);
//        rtcEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
//    }
//
//    private void removeRemoteVideo() {
//        FrameLayout remoteContainer = findViewById(R.id.remote_video_view_container);
//        remoteContainer.removeAllViews();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (rtcEngine != null) {
//            rtcEngine.leaveChannel();
//            RtcEngine.destroy();
//            rtcEngine = null;
//        }
//    }
//}
