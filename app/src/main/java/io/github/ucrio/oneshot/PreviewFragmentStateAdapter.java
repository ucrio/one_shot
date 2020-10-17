package io.github.ucrio.oneshot;

import android.net.Uri;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class PreviewFragmentStateAdapter extends FragmentStateAdapter {

    List<Uri> uriList;

    public PreviewFragmentStateAdapter(FragmentActivity context, List<Uri> uriList) {
        super(context);
        this.uriList = uriList;
    }

    @Override
    public Fragment createFragment(int position) {
        int index;
        if (position < uriList.size()) {
            index = position;
        } else {
            index = 0;
        }

        Uri uri = uriList.get(index);
        return new PreviewFragment(uri);
    }

    @Override
    public int getItemCount() {
        return uriList.size();
    }
}
