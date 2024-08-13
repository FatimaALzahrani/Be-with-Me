package com.waqf.bewithme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Umrah extends AppCompatActivity {

    private WebView webView;
    private TextToSpeech textToSpeech;
    private FusedLocationProviderClient fusedLocationClient;
    private String selectedDestination = "";
    private Location destinationLocation;
    private TextView usernameTextView;
    private TextView saiCountTextView;
    private TextView tawafCountTextView;
    private DatabaseReference databaseReference;
    private DatabaseReference databaseReference2;
    private DatabaseReference databaseReference3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_umrah);

        usernameTextView = findViewById(R.id.usernameTextView);
        saiCountTextView = findViewById(R.id.sai);
        tawafCountTextView = findViewById(R.id.twaf);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        databaseReference2 = FirebaseDatabase.getInstance().getReference("sai_records").child(userId);
        databaseReference3 = FirebaseDatabase.getInstance().getReference("tawaf_records").child(userId);

        // Get today's date in the format used in Firebase
        String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Retrieve Sai records
        databaseReference2.child(dateString).child("lapCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer saiLaps = dataSnapshot.getValue(Integer.class);
                if (saiLaps != null) {
                    saiCountTextView.setText(String.format("سعي: %d/%d", saiLaps,7));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Umrah.this, "فشل في قراءة بيانات السعي", Toast.LENGTH_SHORT).show();
            }
        });

        // Retrieve Tawaf records
        databaseReference3.child(dateString).child("lapCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer tawafLaps = dataSnapshot.getValue(Integer.class);
                if (tawafLaps != null) {
                    tawafCountTextView.setText(String.format("طواف: %d/%d", tawafLaps,7));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Umrah.this, "فشل في قراءة بيانات الطواف", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(view -> openMenu(view));

        // Initialize WebView and load Mappedin map
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://app.mappedin.com/map/66b41acd26fe2b000a504630?embedded=true");

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("ar"));
            }
        });

        // Initialize FusedLocationProviderClient for GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Start voice recognition to choose the destination
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "اختر وجهتك");

        try {
            startActivityForResult(intent, 10);
        } catch (Exception e) {
            Toast.makeText(this, "جهازك لا يدعم التعرف على الصوت", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            processVoiceCommand(spokenText);
        }
    }

    private void processVoiceCommand(String spokenText) {
        if (spokenText.contains("الكعب")) {
            selectedDestination = "الكعبة";
            destinationLocation = new Location("destination");
            destinationLocation.setLatitude(21.4225);
            destinationLocation.setLongitude(39.8262);
        } else if (spokenText.contains("الصفا")) {
            selectedDestination = "الصفا";
            destinationLocation = new Location("destination");
            destinationLocation.setLatitude(21.4187);
            destinationLocation.setLongitude(39.8260);
        } else if (spokenText.contains("المرو")) {
            selectedDestination = "المروة";
            destinationLocation = new Location("destination");
            destinationLocation.setLatitude(21.4223);
            destinationLocation.setLongitude(39.8259);
        } else {
            speak("لم أتمكن من فهم وجهتك. حاول مرة أخرى.");
            startVoiceRecognition();
            return;
        }
        updateMapWithDestination();
        startLocationUpdates();
    }

    private void updateMapWithDestination() {
        webView.evaluateJavascript("javascript:highlightDestination('" + selectedDestination + "')", null);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    navigateUser(location);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void navigateUser(Location currentLocation) {
        if (currentLocation.distanceTo(destinationLocation) < 5) {
            speak("لقد وصلت إلى " + selectedDestination);
        } else {
            speak("استمر في السير نحو " + selectedDestination);
        }
    }

    private void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void openMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.support) {
                Toast.makeText(Umrah.this, "الدعم", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.logout) {
                logout();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void logout() {
        Intent intent = new Intent(Umrah.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void Twaf(View view) {
        Intent intent = new Intent(Umrah.this, Tawaf.class);
        startActivity(intent);
    }

    public void Sai(View view) {
        Intent intent = new Intent(Umrah.this, Sai.class);
        startActivity(intent);
    }

    public void Guide(View view) {
        Intent intent = new Intent(Umrah.this, RequestGuide.class);
        startActivity(intent);
    }

    public void translate(View view) {
        Intent intent = new Intent(Umrah.this, translate.class);
        startActivity(intent);
    }

    public void services(View view) {
        Intent intent = new Intent(Umrah.this, services.class);
        startActivity(intent);
    }
}
