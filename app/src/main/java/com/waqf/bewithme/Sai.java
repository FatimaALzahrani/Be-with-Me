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

public class Sai extends AppCompatActivity {

    private LocationManager locationManager;
    private TextToSpeech textToSpeech;

    // Coordinates for Safa and Marwa
    private final double safaLatitude = 20.2879036; // Safa coordinates
    private final double safaLongitude = 41.3168808;
    private final double marwaLatitude = 20.288164; // Marwa coordinates
    private final double marwaLongitude = 41.316866;

    private int lapCount = 0;
    private ProgressBar progressBar;
    private TextView lapCountTextView;
    private boolean isAtSafa = false;
    private boolean isAtMarwa = false;
    private boolean hasStartedSai = false;
    private boolean hasMovedFromSafa = false;
    private boolean hasMovedFromMarwa = false;
    private LocationListener locationListener;
    private DatabaseReference databaseReference;
    private String userId;
    private String dateString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sai);

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
        databaseReference = FirebaseDatabase.getInstance().getReference("sai_records").child(userId);

        // Generate a date string for the current date
        dateString = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // Retrieve saved Sai data for the current day
        retrieveSavedSaiData();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double currentLatitude = location.getLatitude();
                double currentLongitude = location.getLongitude();

                double tolerance = 0.0001;

                boolean currentlyAtSafa = Math.abs(currentLatitude - safaLatitude) < tolerance &&
                        Math.abs(currentLongitude - safaLongitude) < tolerance;
                boolean currentlyAtMarwa = Math.abs(currentLatitude - marwaLatitude) < tolerance &&
                        Math.abs(currentLongitude - marwaLongitude) < tolerance;

                if (currentlyAtSafa && !isAtSafa && hasStartedSai) {
                    lapCount = Math.min(lapCount + 1, 7);
                    String[] arr = {"الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع"};
                    textToSpeech.speak("أنهيت الآن الشوط " + arr[lapCount - 1], TextToSpeech.QUEUE_FLUSH, null, null);
                    progressBar.setProgress(lapCount);
                    lapCountTextView.setText(lapCount + "/7");

                    saveSaiData();

                    if (lapCount == 7) {
                        textToSpeech.speak("انتهيت من السعي، سيتم نقلك للخريطة لإرشادك إلى محطات النقل", TextToSpeech.QUEUE_FLUSH, null, null);
                        Toast.makeText(Sai.this, "انتهيت من السعي!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Sai.this, Umrah.class);
                        intent.putExtra("Sai", 7);
                        startActivity(intent);
                        finish();
                    }

                    isAtSafa = true;
                    isAtMarwa = false;
                    hasMovedFromSafa = true;
                } else if (currentlyAtMarwa && !isAtMarwa && hasStartedSai) {
                    lapCount = Math.min(lapCount + 1, 7);
                    String[] arr = {"الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع"};
                    textToSpeech.speak("أنهيت الآن الشوط " + arr[lapCount - 1], TextToSpeech.QUEUE_FLUSH, null, null);
                    progressBar.setProgress(lapCount);
                    lapCountTextView.setText(lapCount + "/7");

                    saveSaiData();

                    isAtMarwa = true;
                    isAtSafa = false;
                    hasMovedFromMarwa = true;
                }

                if (!hasStartedSai && currentlyAtSafa) {
                    textToSpeech.speak("بدأت السعي، أنت الآن في الصفا", TextToSpeech.QUEUE_FLUSH, null, null);
                    Toast.makeText(Sai.this, "بدأت السعي الآن!", Toast.LENGTH_SHORT).show();
                    hasStartedSai = true;
                    isAtSafa = true;
                }

                // Additional check to avoid repeatedly setting flags
                if (!currentlyAtSafa && !currentlyAtMarwa) {
                    hasMovedFromSafa = false;
                    hasMovedFromMarwa = false;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        }
    }

    private void retrieveSavedSaiData() {
        databaseReference.child(dateString).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    lapCount = dataSnapshot.child("lapCount").getValue(Integer.class);
                    if (lapCount > 0 && lapCount <= 7) {
                        progressBar.setProgress(lapCount);
                        lapCountTextView.setText(lapCount + "/7");
                        hasStartedSai = true;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Sai.this, "فشل في استرجاع بيانات السعي", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSaiData() {
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
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
