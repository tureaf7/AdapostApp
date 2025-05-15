package com.example.adapostapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder> {
    private List<Object> applications;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
    private FirebaseFirestore db;

    public ApplicationsAdapter(Context context) {
        this.context = context;
        this.applications = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    public void setApplications(List<Object> applications) {
        this.applications = applications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_application, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        Object application = applications.get(position);
        if (application instanceof AdoptionApplication) {
            bindAdoptionApplication(holder, (AdoptionApplication) application);
        } else if (application instanceof VolunteerApplications) {
            bindVolunteerApplication(holder, (VolunteerApplications) application);
        }
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    private void bindAdoptionApplication(ApplicationViewHolder holder, AdoptionApplication application) {
        db.collection("Animals").document(application.getAnimalId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Animal animal = documentSnapshot.toObject(Animal.class);
                        if (animal != null) {
                            holder.nameTextView.setText(animal.getName());
                            holder.requestDateTextView.setText("Cerere trimisă: " + dateFormat.format(application.getApplicationDate()));
                            if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
                                FirebaseStorage.getInstance().getReferenceFromUrl(animal.getPhoto())
                                        .getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            if (!((ApplicationsListActivity) context).isDestroyed() && !((ApplicationsListActivity) context).isFinishing()) {
                                                Glide.with(context).load(uri).error(R.drawable.cat).into(holder.applicationImageView);
                                            }
                                        });
                            }
                        }
                    }
                });

        setStatusAndDetails(holder, application.getStatus(), application.getAdminId(), application.getDateAnswer());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdoptionApplicationDetailsActivity.class);
            intent.putExtra("adoptionApplication", application.getId());
            context.startActivity(intent);
        });
    }

    private void bindVolunteerApplication(ApplicationViewHolder holder, VolunteerApplications application) {
        db.collection("users")
                .document(application.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            holder.nameTextView.setText(user.getName());
                            holder.requestDateTextView.setText("Cerere trimisă: " + dateFormat.format(application.getSubmittedAt()));
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                FirebaseStorage.getInstance().getReferenceFromUrl(user.getProfileImageUrl())
                                        .getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            if (!((ApplicationsListActivity) context).isDestroyed() && !((ApplicationsListActivity) context).isFinishing()) {
                                                Glide.with(context).load(uri).error(R.drawable.cat).into(holder.applicationImageView);
                                            }
                                        });
                            }
                        }
                    }
                });

        setStatusAndDetails(holder, application.getStatus(), application.getAdminId(), application.getDateAnswer());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VolunteerApplicationDetailsActivity.class);
            intent.putExtra("volunteerApplication", application.getId());
            context.startActivity(intent);
        });
    }

    private void setStatusAndDetails(ApplicationViewHolder holder, String status, String adminId, java.util.Date dateAnswer) {
        if ("Respins".equals(status)) {
            holder.statusRejectedTextView.setVisibility(View.VISIBLE);
            holder.statusApprovedTextView.setVisibility(View.GONE);
            holder.statusPendingTextView.setVisibility(View.GONE);
            if (dateAnswer != null) {
                holder.dateAnswerTextView.setText(dateFormat.format(dateAnswer));
            }
            updateAdminName(adminId, holder.adminNameTextView, "Respins de: ");
        } else if ("Aprobat".equals(status)) {
            holder.statusApprovedTextView.setVisibility(View.VISIBLE);
            holder.statusRejectedTextView.setVisibility(View.GONE);
            holder.statusPendingTextView.setVisibility(View.GONE);
            if (dateAnswer != null) {
                holder.dateAnswerTextView.setText(dateFormat.format(dateAnswer));
            }
            updateAdminName(adminId, holder.adminNameTextView, "Aprobat de: ");
        } else {
            holder.statusPendingTextView.setVisibility(View.VISIBLE);
            holder.statusApprovedTextView.setVisibility(View.GONE);
            holder.statusRejectedTextView.setVisibility(View.GONE);
            holder.dateAnswerTextView.setText("");
            holder.adminNameTextView.setText("");
        }
    }

    private void updateAdminName(String adminId, TextView adminNameTextView, String prefix) {
        if (adminId != null) {
            db.collection("users")
                    .document(adminId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User admin = documentSnapshot.toObject(User.class);
                            if (admin != null) {
                                adminNameTextView.setText(prefix + admin.getName());
                            }
                        }
                    });
        }
    }

    static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, requestDateTextView, adminNameTextView, dateAnswerTextView;
        TextView statusApprovedTextView, statusRejectedTextView, statusPendingTextView;
        ImageView applicationImageView;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            requestDateTextView = itemView.findViewById(R.id.requestDateTextView);
            adminNameTextView = itemView.findViewById(R.id.adminNameTextView);
            dateAnswerTextView = itemView.findViewById(R.id.dateAnswerTextView);
            statusApprovedTextView = itemView.findViewById(R.id.statusApprovedTextView);
            statusRejectedTextView = itemView.findViewById(R.id.statusRejectedTextView);
            statusPendingTextView = itemView.findViewById(R.id.statusPendingTextView);
            applicationImageView = itemView.findViewById(R.id.applicationImageView);
        }
    }
}