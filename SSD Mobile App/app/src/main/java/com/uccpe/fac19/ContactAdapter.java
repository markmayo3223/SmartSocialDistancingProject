package com.uccpe.fac19;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ContactAdapter extends FirestoreRecyclerAdapter<Contact, ContactAdapter.ContactHolder> {

    public ContactAdapter(@NonNull FirestoreRecyclerOptions<Contact> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ContactHolder holder, int position, @NonNull Contact model) {
        holder.lat.setText(String.valueOf(model.getLat()));
        holder.lng.setText(String.valueOf(model.getLng()));
        holder.userCon.setText(model.getContactUsername());
        holder.loc.setText(model.getLocation());
        holder.date.setText(model.getDate());
        holder.time.setText(String.valueOf(model.getMsTime()));
    }

    @NonNull
    @Override
    public ContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact,
                parent, false);
        return new ContactHolder(v);
    }

    class ContactHolder extends RecyclerView.ViewHolder {

        TextView time;
        TextView userCon;
        TextView lat;
        TextView lng;
        TextView loc;
        TextView date;

        public ContactHolder(@NonNull View itemView) {
            super(itemView);
            lat = itemView.findViewById(R.id.latTextView);
            lng = itemView.findViewById(R.id.lngTextView);
            date = itemView.findViewById(R.id.dateTextView);
            loc = itemView.findViewById(R.id.loc_text);
            time = itemView.findViewById(R.id.timeTextView);
            userCon = itemView.findViewById(R.id.userConTextView);
        }
    }
}
