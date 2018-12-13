package uk.ac.bbk.wifiaplogger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class SignedInActivity extends AppCompatActivity {

    private static final String TOAST_SIGN_OUT_FAILED = "Sign out failed!";

    /* Tag for logging */
    private static final String TAG = "SignedInActivity";

    private Spinner mSpinner;

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

        /* Get a spinner view (i.e. drop-down list) and specify its default value */
        mSpinner = findViewById(R.id.logging_frequency);
        mSpinner.setSelection(0);

        /* Get signed in user email and user display name from extras */
        final String userEmailText = getIntent().getStringExtra(MainActivity.EXTRA_USER_EMAIL);
        final String displayNameText = getIntent().getStringExtra(MainActivity.EXTRA_DISPLAY_NAME);

        /* Display user email */
        final TextView userEmail = findViewById(R.id.user_email);
        userEmail.setText(userEmailText);

        /* Display user name */
        final TextView userName = findViewById(R.id.user_display_name);
        userName.setText(displayNameText);

        /* This button is responsible for signing user out */
        final Button mSignOutButton = findViewById(R.id.sign_out_button);
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                signOut();
            }
        });

        final Button scanStartButton = findViewById(R.id.scan_start_button);
        scanStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

            }
        });

        final Button scanStopButton = findViewById(R.id.scan_stop_button);
        scanStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

            }
        });
    }

    /**
     * This method signs users out of {@code SignedInActivity} and returns
     * them to the {@code MainActivity} if the task is successful.
     */
    public void signOut() {
        AuthUI.getInstance().signOut(SignedInActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull final Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(MainActivity.createIntent(SignedInActivity.this));
                            finish();
                        } else {
                            Toast.makeText(SignedInActivity.this, TOAST_SIGN_OUT_FAILED, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Creates an intent to be used by the {@code MainActivity} class
     * to launch {@code SignedInActivity}
     *
     * @param context     the launching activity
     * @param idpResponse a container that encapsulates the result of user authentication
     * @return returns an intent to create {@code SignedInActivity}
     */
    public static Intent createIntent(final Context context, final IdpResponse idpResponse) {
        Intent intent = new Intent();
        intent.putExtra(ExtraConstants.IDP_RESPONSE, idpResponse);
        intent.setClass(context, SignedInActivity.class);
        return intent;
    }
}
