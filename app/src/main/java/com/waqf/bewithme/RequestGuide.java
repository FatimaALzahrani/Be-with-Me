package com.waqf.bewithme;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class RequestGuide extends AppCompatActivity {

    private LocationManager locationManager;
    private TextView statusTextView;
    private TextView guideInfoTextView;
    private Button requestGuideButton;

    private DatabaseReference guidesRef;
    private DatabaseReference requestsRef;

    private TextToSpeech textToSpeech;
    private boolean requestSent = false;
    private boolean guideInfoSpoken = false;  // Flag to check if the guide info has been spoken

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_guide);

        statusTextView = findViewById(R.id.statusTextView);
        guideInfoTextView = findViewById(R.id.guideInfoTextView);
        requestGuideButton = findViewById(R.id.requestGuideButton);

        // إعداد مرجع قاعدة البيانات
        guidesRef = FirebaseDatabase.getInstance().getReference("guides");
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");

        // إعداد TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
//                speak("مرحبًا، يمكنك الآن استخدام الأزرار للتفاعل.");
            }
        });

        // إعداد زر طلب المرشد
        requestGuideButton.setOnClickListener(v -> {
            statusTextView.setText("جاري البحث عن مرشد قريب...");
            speak("جاري البحث عن مرشد قريب...");
            requestNearestGuide();
        });

        // إعداد GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    private void requestNearestGuide() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                findNearestGuide(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        });
    }

    private void findNearestGuide(Location userLocation) {
        guidesRef.orderByChild("available").equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Guide nearestGuide = null;
                float nearestDistance = Float.MAX_VALUE;

                for (DataSnapshot guideSnapshot : snapshot.getChildren()) {
                    double lat = guideSnapshot.child("latitude").getValue(Double.class);
                    double lon = guideSnapshot.child("longitude").getValue(Double.class);
                    String name = guideSnapshot.child("name").getValue(String.class);
                    String phoneNumber = guideSnapshot.child("phoneNumber").getValue(String.class);

                    Location guideLocation = new Location("");
                    guideLocation.setLatitude(lat);
                    guideLocation.setLongitude(lon);

                    float distance = userLocation.distanceTo(guideLocation);

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestGuide = new Guide(name, phoneNumber, lat, lon);
                    }
                }

                if (nearestGuide != null) {
                    guideInfoTextView.setText("أقرب مرشد: " + nearestGuide.getName() + " - " + nearestGuide.getPhoneNumber());
                    if (!guideInfoSpoken) {  // Check if guide info has been spoken
                        speak("أقرب مرشد: " + nearestGuide.getName() + " - " + nearestGuide.getPhoneNumber());
                        guideInfoSpoken = true;  // Set flag to true after speaking
                    }
                    sendGuideRequest(nearestGuide);
                } else {
                    guideInfoTextView.setText("لا يوجد مرشد متاح حاليًا");
                    speak("لا يوجد مرشد متاح حاليًا");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RequestGuide.this, "فشل في جلب بيانات المرشدين", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendGuideRequest(Guide guide) {
        if (requestSent) return;

        String requestId = requestsRef.push().getKey();
        if (requestId != null) {
            requestsRef.child(requestId).setValue(new Request(guide.getName(), guide.getPhoneNumber(), guide.getLatitude(), guide.getLongitude()))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "تم إرسال طلب للمرشد: " + guide.getName(), Toast.LENGTH_SHORT).show();
                        speak("تم إرسال الطلب للمرشد الأقرب.");
                        requestSent = true;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "فشل في إرسال الطلب: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "فشل في إرسال الطلب", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
