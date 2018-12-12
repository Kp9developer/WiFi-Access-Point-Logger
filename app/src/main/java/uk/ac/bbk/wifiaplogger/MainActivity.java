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

    /* Block of constants for intent extras */
    public static final String EXTRA_APP_INSTANCE_ID = "extra_app_instance_id";
    public static final String EXTRA_USER_EMAIL = "extra_user_email";
    public static final String EXTRA_DISPLAY_NAME = "extra_display_name";

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

    /**
     * This helper method handles response from sign-in activity and
     * is being called from {@code onActivityResult(int, int, Intent)} method.
     *
     * @param resultCode the activity result code
     * @param data       an intent containing result data
     */
    private void handleSignInResponse(final int resultCode, final Intent data) {
        final IdpResponse response = IdpResponse.fromResultIntent(data);

        final FirebaseUser currentUser = getCurrentUser();
        if (currentUser != null && resultCode == Activity.RESULT_OK) {

            /* This block of code will execute if user has been successfully signed in */

            final String firebaseInstanceId = getFirebaseInstanceId();
            final long firebaseInstanceIdCreationTime = getFirebaseInstanceIdCreationTime();
            final String userEmail = currentUser.getEmail();

            Map<String, Object> user = getSignedInUserInfo(currentUser, firebaseInstanceId, firebaseInstanceIdCreationTime);

            mFirestore.collection(requireNonNull(userEmail)).document(firebaseInstanceId).set(user, SetOptions.merge())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull final Task<Void> task) {
                            if (task.isSuccessful()) {
                                startSignedInActivity(MainActivity.this, response);
                            } else {
                                Log.w(TAG, "Error adding document", task.getException());
                                Toast.makeText(MainActivity.this, TOAST_UNKNOWN_ERROR, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {

            /* This block of code deals with unsuccessful sign in attempts */

            if (response == null) {
                Toast.makeText(this, TOAST_SIGN_IN_CANCELLED, Toast.LENGTH_SHORT).show();
                return;
            }

            if (response.getError() == null) {
                Toast.makeText(this, TOAST_UNKNOWN_ERROR, Toast.LENGTH_SHORT).show();
                return;
            }

            final int errorCode = response.getError().getErrorCode();

            if (errorCode == ErrorCodes.NO_NETWORK) {
                Toast.makeText(this, TOAST_NO_INTERNET, Toast.LENGTH_SHORT).show();
                return;
            }

            if (errorCode == ErrorCodes.UNKNOWN_ERROR) {
                Toast.makeText(this, TOAST_UNKNOWN_ERROR, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Helper method to get the unique ID of this app instance.
     * Instance ID is stable except when:
     *
     * <ul><li>App deletes Instance ID</li>
     * <li>App is restored on a new device</li>
     * <li>User uninstalls/reinstall the app</li>
     * <li>User clears app data</li></ul>
     *
     * @return unique ID of this app instance
     */
    private String getFirebaseInstanceId() {
        return mFirebaseInstanceId.getId();
    }

    /**
     * Helper method to get the time when this instance
     * of the app has been installed.
     *
     * @return the time when this app has been installed (millis since Epoch)
     */
    private long getFirebaseInstanceIdCreationTime() {
        return mFirebaseInstanceId.getCreationTime();
    }

    /**
     * Helper method to get current authenticated user.
     * Will return null if user is not authenticated.
     *
     * @return current user or null otherwise
     */
    private FirebaseUser getCurrentUser() {
        return mFirebaseAuth.getCurrentUser();
    }

    /**
     * Helper method to create a map object with information about
     * currently signed-in user to be recorded to the database.
     *
     * @param firebaseUser                   currently signed-in user
     * @param firebaseInstanceId             unique ID of currently installed app
     * @param firebaseInstanceIdCreationTime the time when this app has been installed
     * @return a map object with information about signed-in user
     */
    private Map<String, Object> getSignedInUserInfo(final FirebaseUser firebaseUser,
                                                    final String firebaseInstanceId,
                                                    final long firebaseInstanceIdCreationTime) {
        final String userEmail = firebaseUser.getEmail();

        final Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", userEmail);
        userInfo.put("uid", firebaseUser.getUid());
        userInfo.put("displayName", firebaseUser.getDisplayName());
        userInfo.put("metaCreationTimestamp", firebaseUser.getMetadata().getCreationTimestamp());
        userInfo.put("metaLastSignInTimestamp", firebaseUser.getMetadata().getLastSignInTimestamp());

        final Map<String, Object> user = new HashMap<>();
        user.put("firebaseInstanceIdCreationTime", firebaseInstanceIdCreationTime);
        user.put("firebaseInstanceId", firebaseInstanceId);
        user.put("userInfo", userInfo);

        return user;
    }

    public static Intent createIntent(final Context context) {
        final Intent intent = new Intent();
        intent.setClass(context, MainActivity.class);
        return intent;
    }

}
