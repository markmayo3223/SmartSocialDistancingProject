package com.uccpe.fac19;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class TrackingRecyclerActivity extends AppCompatActivity {

    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    private final String TAG = "TrackinRecyclerActivity";
    private String email = "";
    private String username = "";


    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference trackingRef = db.collection("users");

    private TrackingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_recycler);

        postInitialValues();
        setUpRecyclerView();

    }

    private void postInitialValues() {
        if(currentUser != null) {
            email = currentUser.getEmail();
        } else {
            Log.d(TAG, "No user logged in.");
        }

        DocumentReference docRef = db.collection("users")
                .document(email);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }

    private void setUpRecyclerView(){
        trackingRef = db.collection("users").document(email)
                .collection("logs")
                .document("tracking")
                .collection("location");

        Query query = trackingRef.orderBy("date", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Tracking> options = new FirestoreRecyclerOptions.Builder<Tracking>()
                .setQuery(query, Tracking.class)
                .build();

        adapter = new TrackingAdapter(options);

        RecyclerView recyclerView = findViewById(R.id.tracking_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}