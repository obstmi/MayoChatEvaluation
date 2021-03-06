package de.obsti.mayo_chat_evaluation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

// ToDo: BUG: Profilbild verschwindet, wenn Status 2x geändert wird,
public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName, userStatus;
    // Profilbild in Kreisdarstellung
    private CircleImageView userProfileImage;
    private String currentUserID;
    private FirebaseAuth mAuth;
    //Referenz zur Datenbank:
    private DatabaseReference rootRef;
    private StorageReference userProfileImagesRef;
    private ProgressDialog loadingBar;

    //Bugfix:
    private String photoUrl = "";

    private static final int GALLERY_PIC = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();
        userProfileImagesRef = FirebaseStorage.getInstance().getReference().child("profile_images");

        initializeFields();

        // default Eingabefeld 'Username': nicht anzeigen (nur, wenn er noch nicht gesetzt wurde)
        userName.setVisibility(View.INVISIBLE);

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSettings();
            }
        });

        retrieveUserInformation();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            // Beim Klick Bildergalerie öffnen
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_PIC);
            }
        });
    }

    // Vorhandene Daten auslesen
    private void retrieveUserInformation() {
        rootRef.child("users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("status")) {
                            String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                            userName.setText(retrieveUsername);
                            userStatus.setText(retrieveStatus);

                            // das Profilbild ist optional
                            if (dataSnapshot.hasChild("image")) {
                                String retrieveProfileImage = dataSnapshot.child("image").getValue().toString();
                                Picasso.get().load(retrieveProfileImage).into(userProfileImage);
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

    // Eingegebene Daten auslesen und abspeichern
    private void updateSettings() {
        String settingsUserName = userName.getText().toString();
        String settingsStatus = userStatus.getText().toString();

        if (TextUtils.isEmpty(settingsUserName)) {
            Toast.makeText(this, "Please insert your username..", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(settingsStatus)) {
            Toast.makeText(this, "Please update your status..", Toast.LENGTH_SHORT).show();
        }
        if (!TextUtils.isEmpty(settingsUserName) && !TextUtils.isEmpty(settingsStatus)) {
            HashMap<String, String> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", settingsUserName);
            profileMap.put("status", settingsStatus);
            if(!TextUtils.isEmpty(photoUrl)) {
                profileMap.put("image", photoUrl);
            }

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

    // Oberfläche mit Membervariablen verknüpfen
    private void initializeFields() {
        updateAccountSettings = (Button) findViewById(R.id.update_settings_button);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
    }

    // Profilbild aktualisieren
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PIC && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            // Bild mit CropImage bearbeiten
            //CropImage.activity() --> FileBrowser wird 2x aufgerufen
            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile image is updating");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri = result.getUri();

                // Filename in der Firebase-DB. Ist die jpg-Endung wirklich sinnvoll?
                // StorageReference filePath = userProfileImagesRef.child(currentUserID + ".jpg");
                StorageReference filePath = userProfileImagesRef.child(currentUserID);

// start original
//                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                        if(task.isSuccessful()) {
//                            Toast.makeText(SettingsActivity.this, "Profile image uploaded", Toast.LENGTH_SHORT).show();
//
//                            // liefert keine URL :-(
//                            final String downloadUrl = task.getResult().getStorage().getDownloadUrl().toString();
//                            // besser?
//                            final String downloadUrl = task.getResult().getMetadata().getReference().getDownloadUrl().toString()
//
//                            // URL zum im Storage gespeicherten Bild in DB-Feld 'image' ablegen
//                            rootRef.child("users").child(currentUserID).child("image")
//                                    .setValue(downloadUrl)
//                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                                        @Override
//                                        public void onComplete(@NonNull Task<Void> task) {
//                                            if (task.isSuccessful()) {
//                                                Toast.makeText(SettingsActivity.this, "Image saved in database", Toast.LENGTH_SHORT).show();
//                                                loadingBar.dismiss();
//                                            } else {
//                                                String message = task.getException().toString();
//                                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
//                                                loadingBar.dismiss();
//                                            }
//                                        }
//                                    });
// ende original

// start neu: Workaround aus Youtube, da 'task.getResult().getStorage().getDownloadUrl()' in höheren Firebase-Klassen nicht mehr funktioniert
                filePath.putFile(resultUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                final Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
                                firebaseUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        final String downloadUrl = uri.toString();
                                        // Bugfix:
                                        photoUrl = downloadUrl;

                                        rootRef.child("users").child(currentUserID).child("image")
                                                .setValue(downloadUrl)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(SettingsActivity.this, "Image saved in database", Toast.LENGTH_SHORT).show();
                                                            loadingBar.dismiss();
                                                        } else {
                                                            String message = task.getException().toString();
                                                            Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                            loadingBar.dismiss();
                                                        }
                                                    }
                                                });
                                    }
                                });
                            }
                        });
// ende neu
// start original
//                        } else {
//                            String message = task.getException().getMessage();
//                            Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
//                            loadingBar.dismiss();
//                        }
//                    }
//                });
// ende original
            }

        }
    }

    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        // GoBack-Button nicht erlaubt
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
