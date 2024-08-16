package com.example.speechstruct;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Splashscreen extends AppCompatActivity {

    WebView webView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splashscreen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                boolean isLoggedIn = sharedPreferences.contains("username");
                boolean termsAccepted = getSharedPreferences("MyPrefs", MODE_PRIVATE).getBoolean("termsAccepted", false);

                Intent intent;
                if (!termsAccepted) {
                    intent = new Intent(Splashscreen.this, TermsActivity.class);
                } else if (!isLoggedIn) {
                    intent = new Intent(Splashscreen.this, LoginActivity.class);
                } else {
                    String role = sharedPreferences.getString("role", "user");
                    if ("admin".equals(role)) {
                        intent = new Intent(Splashscreen.this, AdminActivity.class);
                    } else {
                        intent = new Intent(Splashscreen.this, HomeActivity.class);
                    }
                }

                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}
