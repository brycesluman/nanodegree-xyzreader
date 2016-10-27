package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_ALBUM_POSITION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_ALBUM_POSITION;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_PAGE_POSITION = "state_current_page_position";
    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private int mCurrentPosition;
    private int mStartingPosition;
    private boolean mIsReturning;
    private ArticleDetailFragment mCurrentDetailsFragment;
    private SharedElementCallback mCallback;

    @TargetApi(21)
    private void setCallback() {
        mCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mIsReturning) {
                        ImageView sharedElement = null;
                        if (mCurrentDetailsFragment != null) {
                            sharedElement = mCurrentDetailsFragment.getPhotoView();
                        }
                        if (sharedElement == null) {
                            // If shared element is null, then it has been scrolled off screen and
                            // no longer visible. In this case we cancel the shared element transition by
                            // removing the shared element from the shared elements map.
                            names.clear();
                            sharedElements.clear();
                        } else if (mStartingPosition != mCurrentPosition) {
                            // If the user has swiped to a different ViewPager page, then we need to
                            // remove the old shared element and replace it with the new shared element
                            // that should be transitioned instead.
                            names.clear();
                            names.add(sharedElement.getTransitionName());
                            sharedElements.clear();
                            sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                        }
                    }
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            setCallback();
            postponeEnterTransition();

            setEnterSharedElementCallback(mCallback);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
            supportActionBar.setHomeButtonEnabled(true);
        }
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            startPostponedEnterTransition();
                        }
                    }
                },
                1000);

        mStartingPosition = getIntent().getIntExtra(EXTRA_STARTING_ALBUM_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }
        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mCurrentPosition);

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mCurrentPosition = position;
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
            }
        });


        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_STARTING_ALBUM_POSITION, mStartingPosition);
        data.putExtra(EXTRA_CURRENT_ALBUM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);

        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            mCurrentDetailsFragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            if (mCurrentDetailsFragment != null) {
                mCurrentDetailsFragment.setTransitionName("anim_" + position);
            }
            return mCurrentDetailsFragment;
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
