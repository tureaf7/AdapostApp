package com.example.adapostapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AnimalsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ANIMAL_USER = 0;
    private static final int TYPE_ANIMAL_ADMIN = 1;

    private List<Object> items;
    private Context context;
    private boolean isAdmin;
    private OnAdminActionListener adminActionListener;
    private List<String> favoriteAnimalIds;

    public interface OnAdminActionListener {
        void onEditClicked(String animalId);
        void onDeleteClicked(String animalId);
        void onFavoriteClicked(String animalId);
    }

    public AnimalsAdapter(Context context, boolean isAdmin, OnAdminActionListener listener, List<String> favoriteAnimalIds) {
        this.context = context;
        this.items = new ArrayList<>();
        this.isAdmin = isAdmin;
        this.adminActionListener = listener;
        this.favoriteAnimalIds = new ArrayList<>(favoriteAnimalIds != null ? favoriteAnimalIds : new ArrayList<>());
    }

    public void setItems(List<Object> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void updateFavorites(List<String> newFavoriteIds) {
        this.favoriteAnimalIds.clear();
        this.favoriteAnimalIds.addAll(newFavoriteIds);
        notifyDataSetChanged(); // Reîmprospătăm UI-ul
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof Animal) {
            return isAdmin ? TYPE_ANIMAL_ADMIN : TYPE_ANIMAL_USER;
        }
        return -1; // Nu mai avem TYPE_ADD_BUTTON, dar păstrăm logica pentru siguranță
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ANIMAL_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_animal_item, parent, false);
            return new UserAnimalViewHolder(view);
        } else if (viewType == TYPE_ANIMAL_ADMIN) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_animal_item_edit, parent, false);
            return new AdminAnimalViewHolder(view);
        }
        throw new IllegalArgumentException("Invalid view type");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof UserAnimalViewHolder) {
            UserAnimalViewHolder userHolder = (UserAnimalViewHolder) holder;
            Animal animal = (Animal) item;
            bindUserAnimal(userHolder, animal);
        } else if (holder instanceof AdminAnimalViewHolder) {
            AdminAnimalViewHolder adminHolder = (AdminAnimalViewHolder) holder;
            Animal animal = (Animal) item;
            bindAdminAnimal(adminHolder, animal);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindUserAnimal(UserAnimalViewHolder holder, Animal animal) {
        holder.animalName.setText(animal.getName());
        holder.animalBreed.setText(animal.getBreed());
        holder.animalAge.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani"));
        holder.imageGen.setImageResource(animal.getGen().equals("Mascul") ? R.drawable.ic_male : R.drawable.ic_female);

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(context).load(animal.getPhoto()).into(holder.animalPhoto);
        }

        // Afișăm starea favorite
        boolean isFavorite = favoriteAnimalIds.contains(animal.getId());
        holder.imageButtonFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite_red : R.drawable.ic_favorite);
        holder.imageButtonFavorite.setVisibility(View.VISIBLE); // Asigurăm că butonul este vizibil
        holder.imageButtonFavorite.setOnClickListener(v -> adminActionListener.onFavoriteClicked(animal.getId()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            context.startActivity(intent);
        });
    }

    private void bindAdminAnimal(AdminAnimalViewHolder holder, Animal animal) {
        holder.animalName.setText(animal.getName());
        holder.animalBreed.setText(animal.getBreed());
        holder.animalAge.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani") +
                " și " + animal.getMonths() + (animal.getMonths() == 1 ? " lună" : " luni"));
        holder.animalAdoptedStatus.setText(animal.isAdopted() ? "Adoptat" : "Disponibil");

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(context).load(animal.getPhoto()).into(holder.animalPhoto);
        }

        holder.imageEdit.setOnClickListener(v -> adminActionListener.onEditClicked(animal.getId()));
        holder.imageButtonDelete.setOnClickListener(v -> adminActionListener.onDeleteClicked(animal.getId()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            intent.putExtra("favorite", "favorite");
            context.startActivity(intent);
        });
    }

    static class UserAnimalViewHolder extends RecyclerView.ViewHolder {
        ImageView animalPhoto, imageGen;
        TextView animalName, animalBreed, animalAge;
        ImageButton imageButtonFavorite;

        public UserAnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            animalPhoto = itemView.findViewById(R.id.imageItemImageView);
            animalName = itemView.findViewById(R.id.textViewName);
            animalBreed = itemView.findViewById(R.id.textViewBreed);
            animalAge = itemView.findViewById(R.id.textViewAge);
            imageGen = itemView.findViewById(R.id.imageGen);
            imageButtonFavorite = itemView.findViewById(R.id.imageButtonFavorite);
        }
    }

    static class AdminAnimalViewHolder extends RecyclerView.ViewHolder {
        ImageView animalPhoto;
        TextView animalName, animalBreed, animalAge, animalAdoptedStatus;
        ImageButton imageEdit, imageButtonDelete;

        public AdminAnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            animalPhoto = itemView.findViewById(R.id.imageItemImageView);
            animalName = itemView.findViewById(R.id.textViewName);
            animalBreed = itemView.findViewById(R.id.textViewBreed);
            animalAge = itemView.findViewById(R.id.textViewAge);
            animalAdoptedStatus = itemView.findViewById(R.id.textViewAdoptedStatus);
            imageEdit = itemView.findViewById(R.id.imageEdit);
            imageButtonDelete = itemView.findViewById(R.id.imageButtonDelete);
        }
    }
}