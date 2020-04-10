package com.example.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class CrimeListFragment extends Fragment {

    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;
    private SimpleDateFormat mDf;
    private int mCrimePositionInList;
    private List<Crime> mCrimes;
    private boolean mCrimeInserted;
    private boolean mSubtitleVisible;
    private boolean mCrimeDeleted;
    private LinearLayout mEmptyLayout;
    private SwipeRefreshLayout mRefreshLayout;

    private static final String DATE_FORMAT = "EEEE, MMM dd, yyyy";

    private static final int REQUEST_CRIME = 1;

    private static final String KEY_CRIME_POSITION_IN_LIST = "crime_position";
    private static final String KEY_CRIME_INSERTED = "crime_inserted";
    private static final String KEY_SUBTITLE_VISIBLE = "subtitle";
    private static final String KEY_CRIME_DELETED = "crime_deleted";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCrimePositionInList = -1;

        // Retrieve saved state
        if (savedInstanceState != null) {
            mCrimePositionInList = savedInstanceState.getInt(KEY_CRIME_POSITION_IN_LIST);
            mCrimeInserted = savedInstanceState.getBoolean(KEY_CRIME_INSERTED);
            mSubtitleVisible = savedInstanceState.getBoolean(KEY_SUBTITLE_VISIBLE);
            mCrimeDeleted = savedInstanceState.getBoolean(KEY_CRIME_DELETED);
        }

        // For using toolbar
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        // Set up recycler view
        mCrimeRecyclerView = view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // For Date formatting
        mDf = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);

        // Set up SwipeRefresher layout
        mRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateUI();
                mRefreshLayout.setRefreshing(false);
            }
        });

        // set up add crime button
        Button addCrime = view.findViewById(R.id.add_crime_button);
        addCrime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewCrime();
            }
        });

        // set up empty layout
        mEmptyLayout = view.findViewById(R.id.empty_list_view);
        return view;
    }


    // Call for creating a new crime
    // Creates a new crime, adds it to CrimeLab, and calls the creation fragment
    // with appropriate parameters
    private void createNewCrime() {
        Crime crime = new Crime();
        CrimeLab.get(getActivity()).addCrime(crime);
        Intent intent = CrimePagerActivity.newIntent(getActivity(), crime.getId(), true);
        startActivityForResult(intent, REQUEST_CRIME);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);

        // Change the text of subtitle menu item according to state
        MenuItem subtitleItem = menu.findItem(R.id.show_subtitle);
        if (mSubtitleVisible) {
            subtitleItem.setTitle(R.string.hide_subtitle);
        } else {
            subtitleItem.setTitle(R.string.show_subtitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_crime:
                createNewCrime();
                return true;
            case R.id.show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                updateSubtitle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateUI() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        if (mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setCrimes(crimes);
            mAdapter.notifyDataSetChanged();
        }
        updateSubtitle();

        int crimeCount = CrimeLab.get(getActivity()).getSize();
        if (crimeCount == 0) {
            mRefreshLayout.setVisibility(View.GONE);
            mEmptyLayout.setVisibility(View.VISIBLE);
        } else {
            mRefreshLayout.setVisibility(View.VISIBLE);
            mEmptyLayout.setVisibility(View.INVISIBLE);
        }
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;
        private TextView mTitleTextView;
        private TextView mDateTextView;
        private ImageView mSolvedImageView;

        CrimeHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_crime, parent, false));
            itemView.setOnClickListener(this);

            mTitleTextView = itemView.findViewById(R.id.crime_title);
            mDateTextView = itemView.findViewById(R.id.crime_date);
            mSolvedImageView = itemView.findViewById(R.id.solved_icon);
        }

        void bind(Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle());
            String date = mDf.format(mCrime.getDate());
            mDateTextView.setText(date);
            if (!mCrime.isSolved()) {
                mSolvedImageView.setVisibility(View.INVISIBLE);
            } else {
                mSolvedImageView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId(), false);
            mCrimePositionInList = getAdapterPosition();
            startActivity(intent);
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {
        CrimeAdapter(List<Crime> crimes) {
            mCrimes = crimes;
        }

        @NonNull
        @Override
        public CrimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

            return new CrimeHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull CrimeHolder holder, int position) {
            Crime crime = mCrimes.get(position);
            holder.bind(crime);
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }

        public void setCrimes(List<Crime> crimes) {
            mCrimes = crimes;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CRIME_POSITION_IN_LIST, mCrimePositionInList);
        outState.putBoolean(KEY_CRIME_INSERTED, mCrimeInserted);
        outState.putBoolean(KEY_SUBTITLE_VISIBLE, mSubtitleVisible);
        outState.putBoolean(KEY_CRIME_DELETED, mCrimeDeleted);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CRIME) {
            // if a new crime got inserted
            mCrimePositionInList = CrimeLab.get(getActivity()).getSize();
            mCrimeInserted = true;
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == REQUEST_CRIME) {
            // if the fragment was called for a new crime but user did not save the crime
            assert data != null;
            UUID crimeId = CrimeFragment.getCrimeId(data);
            CrimeLab crimeLab = CrimeLab.get(getActivity());
            crimeLab.deleteCrime(crimeId);
            mCrimePositionInList = crimeLab.getSize();
            mCrimeDeleted = true;
        }
    }

    private void updateSubtitle() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        int crimeCount = crimeLab.getSize();
        String subtitle = getResources().getQuantityString(R.plurals.subtitle_plural, crimeCount, crimeCount);
        if (!mSubtitleVisible) {
            subtitle = null;
        }
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        assert activity != null;
        Objects.requireNonNull(activity.getSupportActionBar()).setSubtitle(subtitle);
    }
}
