package com.waqf.bewithme;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
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
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private final LocationListener locationListener = new LocationListener() {
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
    };

    private void toggleAvailability() {
        isAvailable = !isAvailable;
        guidesRef.child("available").setValue(isAvailable)
                .addOnSuccessListener(aVoid -> {
                    if (isAvailable) {
                        statusTextView.setText("أنت متاح الآن");
                        toggleAvailabilityButton.setText("اجعلني غير متاح");
                        listenForBlindUser();
                    } else {
                        resetGuideStatus();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GuideActivity.this, "فشل في تحديث الحالة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForBlindUser() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        requestsRef.orderByChild("guideId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String status = requestSnapshot.child("status").getValue(String.class);
                    if ("pending".equals(status)) {
                        currentRequestId = requestSnapshot.getKey();
                        String userName = requestSnapshot.child("name").getValue(String.class);
                        String userPhoneNumber = requestSnapshot.child("phoneNumber").getValue(String.class);
                        double userLat = requestSnapshot.child("latitude").getValue(Double.class);
                        double userLon = requestSnapshot.child("longitude").getValue(Double.class);

                        userInfoTextView.setText("اسم المستخدم: " + userName + "\nرقم الهاتف: " + userPhoneNumber);
                        map.setText("موقع المستخدم: https://www.google.com/maps/search/?api=1&query=" + userLat + "," + userLon);
                        completeHelpButton.setVisibility(View.VISIBLE);

                        requestSnapshot.getRef().child("status").setValue("accepted");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuideActivity.this, "فشل في متابعة طلب المستخدم: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeHelp() {
        if (currentRequestId != null) {
            DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference("requests").child(currentRequestId);
            requestRef.child("completed").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "تم إتمام المساعدة بنجاح", Toast.LENGTH_SHORT).show();
                        incrementHelpedBlindCount();
                        resetGuideStatus();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "فشل في إتمام المساعدة: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "لا يوجد طلب حاليًا", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetGuideStatus() {
        statusTextView.setText("أنت غير متاح الآن");
        toggleAvailabilityButton.setText("اجعلني متاح");
        userInfoTextView.setText("");
        map.setText("");
        completeHelpButton.setVisibility(View.INVISIBLE);
    }

    private void incrementHelpedBlindCount() {
        helpedBlindCount++;
        guidesRef.child("helpedBlindCount").setValue(helpedBlindCount)
                .addOnSuccessListener(aVoid -> helpedBlindCountTextView.setText("عدد المكفوفين المساعدين: " + helpedBlindCount))
                .addOnFailureListener(e -> Toast.makeText(GuideActivity.this, "فشل في تحديث عدد المكفوفين المساعدين: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
