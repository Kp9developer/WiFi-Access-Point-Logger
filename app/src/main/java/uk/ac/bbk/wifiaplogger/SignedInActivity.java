package uk.ac.bbk.wifiaplogger;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignedInActivity extends AppCompatActivity {

    /* The entry point of the Firebase Authentication SDK */
    private final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in);

        /* Return to the MainActivity if user hasn't signed in */
        if (mFirebaseAuth.getCurrentUser() == null) {
            startActivity(MainActivity.createIntent(this));
            finish();
        }

    }
}
