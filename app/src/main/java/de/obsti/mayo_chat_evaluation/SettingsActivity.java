package de.obsti.mayo_chat_evaluation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;
    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        initializeFields();

        userName.setVisibility(View.INVISIBLE);

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSettings();
            }
        });

        retrieveUserInformation();
    }

    private void retrieveUserInformation() {
        rootRef.child("users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        if(dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("image")) {
//
//                        }
                        if(dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("status")) {
                            String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                            userName.setText(retrieveUsername);
                            userStatus.setText(retrieveStatus);

                            if(dataSnapshot.hasChild("image")) {
                                String retrieveProfileImage = dataSnapshot.child("image").getValue().toString();
                            }

                        } else {
                            userName.setVisibility(View.VISIBLE);
                            Toast.makeText(SettingsActivity.this, "Please set and update your profile!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void updateSettings() {
        String settingsUserName = userName.getText().toString();
        String settingsStatus = userStatus.getText().toString();

        if(TextUtils.isEmpty(settingsUserName)) {
            Toast.makeText(this, "Please insert your username..", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(settingsStatus)) {
            Toast.makeText(this, "Please update your status..", Toast.LENGTH_SHORT).show();
        }
        if(!TextUtils.isEmpty(settingsUserName) && !TextUtils.isEmpty(settingsStatus)) {
            HashMap<String, String> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", settingsUserName);
            profileMap.put("status", settingsStatus);

            rootRef.child("users").child(currentUserID).setValue(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                sendUserToMainActivity(); //kurios, dass trotzdem noch die Toast-Message angezeigt wird
                                Toast.makeText(SettingsActivity.this, "Success: Profile update.", Toast.LENGTH_SHORT).show();
                            } else {
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void initializeFields() {
        updateAccountSettings = (Button)findViewById(R.id.update_settings_button);
        userName = (EditText)findViewById(R.id.set_user_name);
        userStatus = (EditText)findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView)findViewById(R.id.set_profile_image);
    }

    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        // GoBack-Button nicht erlaubt
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
