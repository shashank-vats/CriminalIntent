package com.example.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity implements CrimeFragment.Callbacks {
    private ViewPager mViewPager;
    private Button mFirstButton;
    private Button mLastButton;
    private boolean mNewCrime;

    private List<Crime> mCrimes;

    private static final String EXTRA_CRIME_ID = "com.example.criminalintent.crime_id";
    private static final String EXTRA_NEW_CRIME = "com.example.criminalintent.new_crime";

    public static Intent newIntent(Context packageContext, UUID crimeId, boolean newCrime) {
        Intent intent = new Intent(packageContext, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        intent.putExtra(EXTRA_NEW_CRIME, newCrime);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);
        UUID crimeId = (UUID) getIntent().getSerializableExtra(EXTRA_CRIME_ID);
        mNewCrime = getIntent().getBooleanExtra(EXTRA_NEW_CRIME, false);

        mViewPager = findViewById(R.id.crime_view_pager);

        mCrimes = CrimeLab.get(this).getCrimes();
        FragmentManager fragmentManager = getSupportFragmentManager();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                Crime crime = mCrimes.get(position);
                boolean newCrime = mNewCrime;
                mNewCrime = false;
                return CrimeFragment.newInstance(crime.getId(), newCrime);
            }

            @Override
            public int getCount() {
                return mCrimes.size();
            }

        });

        mFirstButton = findViewById(R.id.first_button);
        mFirstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(0);
            }
        });

        mLastButton = findViewById(R.id.last_button);
        mLastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mCrimes.size() - 1);
            }
        });

        if (mNewCrime) {
            Crime crime = new Crime(crimeId);
            mCrimes.add(crime);
            Objects.requireNonNull(mViewPager.getAdapter()).notifyDataSetChanged();
            mViewPager.setCurrentItem(mCrimes.size() - 1);
            if (mCrimes.size() == 1) {
                mFirstButton.setEnabled(false);
            }
            mLastButton.setEnabled(false);
        } else {
            for (int i = 0; i < mCrimes.size(); i++) {
                if (mCrimes.get(i).getId().equals(crimeId)) {
                    mViewPager.setCurrentItem(i);
                    if (i == 0) {
                        mFirstButton.setEnabled(false);
                    }
                    if (i == mCrimes.size() - 1) {
                        mLastButton.setEnabled(false);
                    }
                    break;
                }
            }
        }

        mViewPager.addOnPageChangeListener(new MyOnPageChangeListener());
    }

    @Override
    public void onCrimeUpdated(Crime crime) {

    }

    private class MyOnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            if (position == 0) {
                mFirstButton.setEnabled(false);
            } else {
                mFirstButton.setEnabled(true);
            }
            if (position == mCrimes.size() - 1) {
                mLastButton.setEnabled(false);
            } else {
                mLastButton.setEnabled(true);
            }
        }
    }
}
