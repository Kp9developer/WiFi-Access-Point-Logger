package uk.ac.bbk.wifiaplogger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class MainActivity extends AppCompatActivity {

    /* request code identifier for sign-in activity */
    private static final int RC_SIGN_IN = 123;

    /* Tag for logging */
    private static final String TAG = "MainActivity";

    /* Block of constants for toast messages */
    private static final String TOAST_SIGN_IN_CANCELLED = "Sign in was cancelled!";
    private static final String TOAST_NO_INTERNET = "You have no internet connection";
    private static final String TOAST_UNKNOWN_ERROR = "Unknown Error!";


    /* The entry point of the Firebase Authentication SDK */
    private final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    /* Represents a Firestore Database and is the entry point for all Firestore operations */
    private final FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

    /* Firebase Instance ID provides a unique identifier for each app instance */
    private final FirebaseInstanceId mFirebaseInstanceId = FirebaseInstanceId.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* this button is responsible for signing user in with firebase-ui-auth library */
        final Button signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent authUiIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(true)
                        .build();
                startActivityForResult(authUiIntent, RC_SIGN_IN);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the result comes from predefined intent and handle the response received
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
        }
    }

}
