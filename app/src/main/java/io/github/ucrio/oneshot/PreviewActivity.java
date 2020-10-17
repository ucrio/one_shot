package io.github.ucrio.oneshot;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

public class PreviewActivity extends FragmentActivity {

    ArrayList<Uri> uriList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        AdView mAdView = findViewById(R.id.adViewPreview);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // receive image uris and the initial index to preview
        uriList = getIntent().getParcelableArrayListExtra("image");
        int tag = getIntent().getIntExtra("tag", 0);

        if (uriList != null && uriList.size() > 0) {
            // set view pager
            ViewPager2 viewPager = (ViewPager2) findViewById(R.id.pager);
            viewPager.setAdapter(
                    new PreviewFragmentStateAdapter(
                            this, uriList));
            // set the initial index
            viewPager.setCurrentItem(tag, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uriList != null) {
            for (Uri uri: uriList) {
                getContentResolver().delete(uri, null, null);
            }
        }
    }
}