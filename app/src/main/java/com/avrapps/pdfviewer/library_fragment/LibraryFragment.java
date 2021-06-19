package com.avrapps.pdfviewer.library_fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.library_fragment.data.LastOpenDocuments;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class LibraryFragment extends Fragment {
    private static final String TAG = "LibraryFragment";

    MainActivity activity;
    Toolbar toolbar;
    ViewPager2 viewPager;
    FileListingAdapter fileListingAdapter;
    BrowseFilesFragment browseFilesFragment;
    List<String> tabHead, tabHeadExtensions, toolbarHeads;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fileopen_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        toolbar = activity.findViewById(R.id.my_toolbar);
        tabHead = Arrays.asList(getString(R.string.recent_title), getString(R.string.favorite_title), getString(R.string.browse_title), getString(R.string.pdf_library_title), getString(R.string.epub_library_title), getString(R.string.tiff_library_title), getString(R.string.xps_library_title));
        tabHeadExtensions = Arrays.asList("", "", "", ".pdf", ".epub", ".tif,.tiff", ".xps,.oxps,.cbz,.fb2");
        toolbarHeads = Arrays.asList(getString(R.string.recent_files), getString(R.string.favorites), getString(R.string.browse_files), getString(R.string.pdf_library), getString(R.string.epub_library), getString(R.string.tiff_library), getString(R.string.xps_library));
        showViewPager(view);
    }

    private void showViewPager(View view) {
        fileListingAdapter = new FileListingAdapter(this);
        viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(fileListingAdapter);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabHead.get(position))
        ).attach();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                toolbar.setTitle(toolbarHeads.get(position));
            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                activity.showGridListViewOption(position == 1 || position == 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewPager != null) {
            viewPager.postDelayed(() -> {
                if (new LastOpenDocuments().getLastOpenDocuments().size() == 0) {
                    viewPager.setCurrentItem(2, true);
                }
            }, 100);
        }
    }

    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 2) {
            fileListingAdapter.onBackPressed();
        } else {
            activity.finish();
        }
    }

    public void onRefresh() {
        if (fileListingAdapter != null) {
            fileListingAdapter.onRefresh();
        }
    }

    public class FileListingAdapter extends FragmentStateAdapter {
        public FileListingAdapter(Fragment fragment) {
            super(fragment);
            browseFilesFragment = new BrowseFilesFragment();
        }


        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0 || position == 1) {
                Fragment fragment = new ListRecentsFragment();
                Bundle args = new Bundle();
                args.putInt(ListRecentsFragment.ARG_OBJECT, position);
                fragment.setArguments(args);
                return fragment;
            } else if (position == 2) {
                return browseFilesFragment;
            } else {
                Fragment fragment = new FilesListFragment();
                Bundle args = new Bundle();
                args.putString(ListRecentsFragment.ARG_OBJECT, tabHeadExtensions.get(position));
                fragment.setArguments(args);
                return fragment;
            }

        }

        @Override
        public int getItemCount() {
            return tabHead.size();
        }

        public void onBackPressed() {
            browseFilesFragment.onBackPressed();
        }

        public void onRefresh() {
            if (viewPager.getCurrentItem() < 2) {
                ListRecentsFragment fragment = (ListRecentsFragment)getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                if (fragment != null) {
                    fragment.onRefresh();
                }
            }
        }
    }

}
