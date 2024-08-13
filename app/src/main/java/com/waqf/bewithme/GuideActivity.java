package com.waqf.bewithme;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
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

public class GuideActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private TextView statusTextView, userInfoTextView, map, helpedBlindCountTextView;
    private Button toggleAvailabilityButton, completeHelpButton;
    private boolean isAvailable = false;
    private String userId;
    private DatabaseReference guidesRef;
    private int helpedBlindCount = 0;
    private String currentRequestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        statusTextView = findViewById(R.id.statusTextView);
        userInfoTextView = findViewById(R.id.userInfoTextView);
        map = findViewById(R.id.map);
        helpedBlindCountTextView = findViewById(R.id.helpedBlindCountTextView);
        toggleAvailabilityButton = findViewById(R.id.toggleAvailabilityButton);
        completeHelpButton = findViewById(R.id.completeHelpButton);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        guidesRef = FirebaseDatabase.getInstance().getReference("guides").child(userId);

        toggleAvailabilityButton.setOnClickListener(v -> toggleAvailability());
        completeHelpButton.setOnClickListener(v -> completeHelp());

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (isAvailable) {
                    guidesRef.child("latitude").setValue(location.getLatitude());
                    guidesRef.child("longitude").setValue(location.getLongitude());
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        });

        listenForBlindUser();
        loadHelpedBlindCount();
    }

    private void toggleAvailability() {
        isAvailable = !isAvailable;
        guidesRef.child("available").setValue(isAvailable);

        if (isAvailable) {
            statusTextView.setText("متاح");
            toggleAvailabilityButton.setText("أنت غير متاح الآن");
        } else {
            statusTextView.setText("غير متاح");
            toggleAvailabilityButton.setText("أنت متاح الآن");
        }
    }

    private void listenForBlindUser() {
        DatabaseReference blindUserRef = FirebaseDatabase.getInstance().getReference("requests");
        blindUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                        String name = requestSnapshot.child("name").getValue(String.class);
                        String phoneNumber = requestSnapshot.child("phoneNumber").getValue(String.class);
                        double userLat = requestSnapshot.child("latitude").getValue(Double.class);
                        double userLon = requestSnapshot.child("longitude").getValue(Double.class);

                        String userLocation = "موقع الكفيف: " + userLat + ", " + userLon;
                        String userInfo = "الاسم: " + name + ", الرقم: " + phoneNumber;

                        userInfoTextView.setText(userLocation + "\n" + userInfo);
                        String mapLink = "https://maps.google.com/?q=" + userLat + "," + userLon;
                        map.setText(mapLink);

                        if (isAvailable) {
                            toggleAvailability();
                            currentRequestId = requestSnapshot.getKey();
                            completeHelpButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuideActivity.this, "فشل في جلب بيانات الكفيف", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void incrementHelpedBlindCount() {
        helpedBlindCount++;
        guidesRef.child("helpedBlindCount").setValue(helpedBlindCount);
        helpedBlindCountTextView.setText("عدد الكفيفين المساعدين: " + helpedBlindCount);
    }

    private void loadHelpedBlindCount() {
        guidesRef.child("helpedBlindCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    helpedBlindCount = snapshot.getValue(Integer.class);
                    helpedBlindCountTextView.setText("عدد الكفيفين المساعدين: " + helpedBlindCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuideActivity.this, "فشل في تحميل عدد الكفيفين المساعدين", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeHelp() {
        if (currentRequestId != null) {
            map.setText("");
            userInfoTextView.setText("جار البحث عن كفيف يحتاج مساعدة..");

            DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("requests").child(currentRequestId);
            requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // نقل البيانات إلى جدول "done"
                        DatabaseReference doneRef = FirebaseDatabase.getInstance().getReference("done").child(currentRequestId);
                        doneRef.setValue(snapshot.getValue())
                                .addOnSuccessListener(aVoid -> {
                                    // حذف البيانات من جدول "requests"
                                    requestsRef.removeValue()
                                            .addOnSuccessListener(aVoid1 -> {
                                                Toast.makeText(GuideActivity.this, "تم نقل الطلب إلى جدول done بنجاح", Toast.LENGTH_SHORT).show();
                                                incrementHelpedBlindCount();
                                                completeHelpButton.setVisibility(View.GONE); // إخفاء الزر بعد إتمام المساعدة
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(GuideActivity.this, "فشل في حذف الطلب من جدول requests", Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(GuideActivity.this, "فشل في نقل الطلب إلى جدول done", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(GuideActivity.this, "فشل في جلب بيانات الطلب", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
