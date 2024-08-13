package com.waqf.bewithme;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestGuide extends AppCompatActivity {

    private LocationManager locationManager;
    private TextView guideInfoTextView;
    private Button requestGuideButton;
    private DatabaseReference requestsRef, guidesRef;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_guide);

        guideInfoTextView = findViewById(R.id.guideInfoTextView);
        requestGuideButton = findViewById(R.id.requestGuideButton);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        guidesRef = FirebaseDatabase.getInstance().getReference("guides");

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        requestGuideButton.setOnClickListener(v -> findNearestAvailableGuide());

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void findNearestAvailableGuide() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تحتاج إلى منح صلاحيات الموقع للوصول إلى أقرب مرشد", Toast.LENGTH_SHORT).show();
            return;
        }

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {
            double userLat = lastKnownLocation.getLatitude();
            double userLon = lastKnownLocation.getLongitude();

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(userID);

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userName = snapshot.child("name").getValue(String.class);
                        int userPhoneNumber = snapshot.child("phoneNumber").getValue(int.class);

                        guidesRef.orderByChild("available").equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    double minDistance = Double.MAX_VALUE;
                                    Guide nearestGuide = null;
                                    String nearestGuideId = null;

                                    for (DataSnapshot guideSnapshot : snapshot.getChildren()) {
                                        Double guideLat = guideSnapshot.child("latitude").getValue(Double.class);
                                        Double guideLon = guideSnapshot.child("longitude").getValue(Double.class);

                                        if (guideLat != null && guideLon != null) {
                                            double distance = calculateDistance(userLat, userLon, guideLat, guideLon);

                                            if (distance < minDistance) {
                                                minDistance = distance;
                                                nearestGuide = guideSnapshot.getValue(Guide.class);
                                                nearestGuideId = guideSnapshot.getKey();
                                            }
                                        }
                                    }

                                    if (nearestGuide != null && nearestGuideId != null) {
                                        guideInfoTextView.setText("الاسم: " + nearestGuide.getName() + "\nرقم الهاتف: " + nearestGuide.getPhoneNumber());
                                        speak("تم العثور على أقرب مرشد. الاسم: " + nearestGuide.getName() + " رقم الهاتف: " + nearestGuide.getPhoneNumber());

                                        sendGuideRequest(nearestGuide, nearestGuideId, userName, String.valueOf(userPhoneNumber), userLat, userLon);
                                    } else {
                                        Toast.makeText(RequestGuide.this, "لا يوجد مرشدون متاحون حاليًا", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(RequestGuide.this, "لا يوجد مرشدون متاحون حاليًا", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(RequestGuide.this, "فشل في العثور على مرشد", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(RequestGuide.this, "فشل في الحصول على بيانات المستخدم", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(RequestGuide.this, "فشل في جلب بيانات المستخدم", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "لا يمكن الحصول على الموقع الحالي", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendGuideRequest(Guide guide, String guideId, String userName, String userPhoneNumber, double userLat, double userLon) {
        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference("requests").push();
        String requestId = requestRef.getKey();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("guideId", guideId);
        requestData.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        requestData.put("status", "pending");
        requestData.put("name", userName);
        requestData.put("phoneNumber", userPhoneNumber);
        requestData.put("latitude", userLat);
        requestData.put("longitude", userLon);

        requestRef.setValue(requestData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RequestGuide.this, "تم إرسال الطلب بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RequestGuide.this, "فشل في إرسال الطلب: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "الصلاحيات غير ممنوحة", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
