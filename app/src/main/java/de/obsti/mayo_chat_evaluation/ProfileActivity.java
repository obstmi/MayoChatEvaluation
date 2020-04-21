package de.obsti.mayo_chat_evaluation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserId, senderUserId, currentState;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestButton, declineMessageRequestButton;

    private DatabaseReference userRef, chatRequestRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("chat_requests");

        // übernehme die User-ID des übergebenen Users
        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();
        // hole die User.ID des eingeloggten Users (also des Users, der eine Nachricht senden möchte)
        senderUserId = mAuth.getCurrentUser().getUid();

        // initialisiere Members:
        userProfileImage = (CircleImageView)findViewById(R.id.visit_profile_image);
        userProfileName = (TextView)findViewById(R.id.visit_user_name);
        userProfileStatus = (TextView)findViewById(R.id.visit_profile_status);
        sendMessageRequestButton = (Button)findViewById(R.id.send_message_request_button);
        declineMessageRequestButton = (Button)findViewById(R.id.decline_message_request_button);

        currentState = "new";

        // retrieve information:
        retrieveUserInfo();

    }
    // hole Userdaten und verbinde sie mit Aktivität
    private void retrieveUserInfo() {
        userRef.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    String userName = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    if(dataSnapshot.hasChild("image")) {
                        String userImage = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    }
                    
                    manageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // buttons entsprechend anzeigen und darauf reagieren
    private void manageChatRequest() {
        // zuerst den status des Buttons richtig setzen:
        chatRequestRef.child(senderUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(receiverUserId)) {
                            String requestType = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();

                            // nach dem Absenden Button-Text ändern
                            if(requestType.equals("sent")) {
                                currentState = "request_sent";
                                sendMessageRequestButton.setText("cancel chat request");
                            } else if(requestType.equals("received")) {
                                currentState = "request_received";
                                sendMessageRequestButton.setText("accept chat request");

                                //der Empfänger kann den Chatrequest auch ablehnen
                                declineMessageRequestButton.setVisibility(View.VISIBLE);
                                declineMessageRequestButton.setEnabled(true);

                                declineMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        cancelChatRequest();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
        // Button nur anzeigen und darauf reagieren, wenn nicht das eigene Profil angezeigt wird
        if(!senderUserId.equals(receiverUserId)) {
            sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessageRequestButton.setEnabled(false);

                    // Chatrequest entweder senden oder löschen
                    if(currentState.equals("new")) {
                        sendChatRequest();
                    } else if(currentState.equals("request_sent")) {
                    cancelChatRequest();
                }
                }
            });
        } else {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    private void cancelChatRequest() {
        //Chat-Reqeust des Senders entfernen
        chatRequestRef.child(senderUserId).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            //Chat-Request beim Empfänger entfernen
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                sendMessageRequestButton.setEnabled(true);
                                                currentState = "new";
                                                sendMessageRequestButton.setText("send message");

                                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                declineMessageRequestButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void sendChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                sendMessageRequestButton.setEnabled(true);
                                                currentState = "request_sent";
                                                sendMessageRequestButton.setText("cancel chat request");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
