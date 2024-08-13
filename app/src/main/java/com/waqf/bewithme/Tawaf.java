package com.waqf.bewithme;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Locale;

public class Tawaf extends AppCompatActivity {

    private LocationManager locationManager;
    private TextToSpeech textToSpeech;
    private final double targetLatitude = 20.2879036;
    private final double targetLongitude = 41.3168808;
    private int lapCount = 0;
    private ProgressBar progressBar;
    private TextView lapCountTextView;
    private boolean isInsideTarget = false;
    private boolean hasLeftTarget = false;
    private boolean hasStartedTawaf = false;
    private LocationListener locationListener;
    private DatabaseReference databaseReference;
    private String userId;
    private String dateString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tawaf);

        progressBar = findViewById(R.id.progressBar);
        lapCountTextView = findViewById(R.id.lapCountTextView);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("ar"));
            } else {
                Log.e("TextToSpeech", "Initialization failed");
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("tawaf_records").child(userId);

        // Generate a date string for the current date
        dateString = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // Retrieve saved Tawaf data for the current day
        retrieveSavedTawafData();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double currentLatitude = location.getLatitude();
                double currentLongitude = location.getLongitude();

                double tolerance = 0.0001;
                boolean currentlyInsideTarget = Math.abs(currentLatitude - targetLatitude) < tolerance &&
                        Math.abs(currentLongitude - targetLongitude) < tolerance;

                Log.d("LocationCheck", "Current Lat: " + currentLatitude + ", Lon: " + currentLongitude);
                Log.d("LocationCheck", "Currently Inside Target: " + currentlyInsideTarget);
                Log.d("LocationCheck", "isInsideTarget: " + isInsideTarget + ", hasLeftTarget: " + hasLeftTarget);
                Log.d("LocationCheck", "Lap Count: " + lapCount);

                if (currentlyInsideTarget && !isInsideTarget && hasLeftTarget && hasStartedTawaf) {
                    String[] arr = {"الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع"};
                    lapCount = Math.min(lapCount + 1, 7);
                    textToSpeech.speak("انهيت الآن الشوط " + arr[lapCount - 1], TextToSpeech.QUEUE_FLUSH, null, null);
                    progressBar.setProgress(lapCount);
                    lapCountTextView.setText(lapCount + "/7");

                    saveTawafData(); // Save the progress to Firebase

                    if (lapCount == 7) {
                        textToSpeech.speak("انتهيت من الطواف، سيتم إرجاعك للخريطة لإرشادك إلى المسعى", TextToSpeech.QUEUE_FLUSH, null, null);
                        Intent intent = new Intent(Tawaf.this, Umrah.class);
                        intent.putExtra("Tawaf", 7);
                        Toast.makeText(Tawaf.this, "انتهيت من الطواف!", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        finish();
                    }

                    hasLeftTarget = false;
                }

                if (!hasStartedTawaf && currentlyInsideTarget) {
                    textToSpeech.speak("بدأت الطواف الآن", TextToSpeech.QUEUE_FLUSH, null, null);
                    Toast.makeText(Tawaf.this, "بدأت الطواف الآن!", Toast.LENGTH_SHORT).show();
                    hasStartedTawaf = true;
                }

                if (!currentlyInsideTarget) {
                    hasLeftTarget = true;
                    Log.i("LocationCheck", "User left the target area.");
                }

                isInsideTarget = currentlyInsideTarget;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        // التحقق من أذونات الموقع
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        }
    }

    private void retrieveSavedTawafData() {
        databaseReference.child(dateString).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer savedLapCount = dataSnapshot.child("lapCount").getValue(Integer.class);
                    if (savedLapCount != null) {
                        lapCount = savedLapCount;
                        progressBar.setProgress(lapCount);
                        lapCountTextView.setText(lapCount + "/7");
                        hasStartedTawaf = lapCount > 0;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Tawaf.this, "فشل في استرجاع بيانات الطواف", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTawafData() {
        databaseReference.child(dateString).child("lapCount").setValue(lapCount);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // طلب التحديثات إذا تم منح الأذونات
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void Sai(View view) {
        Intent intent = new Intent(Tawaf.this, Sai.class);
        startActivity(intent);
    }
}
