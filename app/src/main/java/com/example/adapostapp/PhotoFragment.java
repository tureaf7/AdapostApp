package com.example.adapostapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class PhotoFragment extends androidx.fragment.app.Fragment {
    private static final String ARG_PHOTO_URL = "photo_url";

    public static PhotoFragment newInstance(String photoUrl) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_URL, photoUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        ImageView imageView = view.findViewById(R.id.imageViewPhoto);
        if (getArguments() != null) {
            String photoUrl = getArguments().getString(ARG_PHOTO_URL);
            if (photoUrl != null) {
                Glide.with(this).load(photoUrl).into(imageView);
            }
        }
        return view;
    }
}
