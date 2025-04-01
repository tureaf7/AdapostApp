package com.example.adapostapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends ArrayAdapter<User> {
    private List<User> users;
    private List<User> filteredUsers;

    public UserAdapter(@NonNull Context context, @NonNull List<User> users) {
        super(context, 0, users);
        this.users = new ArrayList<>(users);
        this.filteredUsers = new ArrayList<>(users);
        Log.d("UserAdapter", "Constructor: Total utilizatori inițiali: " + users.size());
    }

    // Metodă pentru a actualiza lista de utilizatori
    public void updateUsers(List<User> newUsers) {
        this.users.clear();
        this.users.addAll(newUsers);
        this.filteredUsers.clear();
        this.filteredUsers.addAll(newUsers);
        Log.d("UserAdapter", "updateUsers: Total utilizatori actualizați: " + users.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown_user, parent, false);
        }

        User user = getItem(position);
        if (user != null) {
            // Setăm textul
            TextView textViewName = convertView.findViewById(R.id.textViewName);
            textViewName.setText(user.getName());

            // Setăm imaginea
            CircleImageView imageView = convertView.findViewById(R.id.imageView5);
            Glide.with(this.getContext())
                    .load(user.getProfileImageUrl())
                    .error(R.drawable.ic_profile)
                    .into(imageView);

            Log.d("UserAdapter", "Afișare utilizator: " + user.getName() + " la poziția " + position);
        } else {
            Log.e("UserAdapter", "Utilizator null la poziția " + position);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        Log.d("UserAdapter", "getCount: " + filteredUsers.size());
        return filteredUsers.size();
    }

    @Override
    public User getItem(int position) {
        return filteredUsers.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<User> suggestions = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    suggestions.addAll(users);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (User user : users) {
                        if (user.getName() != null && user.getName().toLowerCase().contains(filterPattern)) {
                            suggestions.add(user);
                        }
                    }
                }

                Log.d("UserAdapter", "performFiltering: " + suggestions.size() + " sugestii găsite pentru constraint: " + constraint);
                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredUsers.clear();
                filteredUsers.addAll((List<User>) results.values);
                Log.d("UserAdapter", "publishResults: " + filteredUsers.size() + " utilizatori filtrați");
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return ((User) resultValue).getName();
            }
        };
    }
}