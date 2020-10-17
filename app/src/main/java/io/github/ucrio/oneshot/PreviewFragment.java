package io.github.ucrio.oneshot;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class PreviewFragment extends Fragment {

    private Uri imageUri;

    public PreviewFragment(Uri uri) {
        this.imageUri = uri;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_preview, container, false);

        if (imageUri != null) {
            ImageView iv = view.findViewById(R.id.previewImage);
            iv.setImageURI(imageUri);
        }
        return view;
    }

}