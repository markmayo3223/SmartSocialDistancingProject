package com.uccpe.fac19;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class TrackingAdapter extends FirestoreRecyclerAdapter<Tracking, TrackingAdapter.TrackingHolder> {

    public TrackingAdapter(@NonNull FirestoreRecyclerOptions<Tracking> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull TrackingHolder holder, int position, @NonNull Tracking model) {
        holder.lat.setText(String.valueOf(model.getLat()));
        holder.lng.setText(String.valueOf(model.getLng()));
        holder.location.setText(model.getLocation());
        holder.date.setText(model.getDate());
        holder.time.setText(String.valueOf(model.getTime()));
    }

    @NonNull
    @Override
    public TrackingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tracking,
                parent, false);
        return new TrackingHolder(v);

    }

    class TrackingHolder extends RecyclerView.ViewHolder {

        TextView lat;
        TextView lng;
        TextView location;
        TextView date;
        TextView time;

        public TrackingHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.track_time_text);
            lat = itemView.findViewById(R.id.track_latitude);
            location = itemView.findViewById(R.id.location_text);
            lng = itemView.findViewById(R.id.track_longitude);
            date = itemView.findViewById(R.id.track_date_text);
        }
    }
}
