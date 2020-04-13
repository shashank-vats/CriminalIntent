package com.example.criminalintent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class PhotoViewerFragment extends DialogFragment {
    private static final String ARG_PHOTO_PATH = "photo_path";

    private ImageView mPhotoView;
    private String mPhotoPath;

    static PhotoViewerFragment newInstance(String photoPath) {
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, photoPath);
        PhotoViewerFragment fragment = new PhotoViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        assert getArguments() != null;
        mPhotoPath = getArguments().getString(ARG_PHOTO_PATH);
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_photo_viewer, null);
        mPhotoView = v.findViewById(R.id.photo_zoom_view);
        Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoPath, Objects.requireNonNull(getActivity()));
        mPhotoView.setImageBitmap(bitmap);
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PhotoViewerFragment.this.dismiss();
                    }
                }).create();
    }
}
