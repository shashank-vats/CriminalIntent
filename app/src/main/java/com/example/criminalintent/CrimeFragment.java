package com.example.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private Crime mCrime;
    private boolean mEditable;
    private Date mDate;
    private boolean mNewCrime;
    private SimpleDateFormat mDf;
    private SimpleDateFormat mTf;

    private Button mDateButton;
    private EditText mTitleField;
    private CheckBox mSolvedCheckBox;
    private Button mTimeButton;
    private TextView mEditAlert;

    private static final String DATE_FORMAT = "EEEE, MMM dd, yyyy";
    private static final String TIME_FORMAT = "hh:mm a";

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String ARG_NEW_CRIME = "new_crime";

    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;

    private static final String KEY_NEW_CRIME = "new_crime";
    private static final String KEY_EDITABLE = "editable";

    private static final String EXTRA_CRIME_ID = "com.example.criminalintent.crime_id";

    static CrimeFragment newInstance(UUID crimeId, boolean newCrime) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);
        args.putBoolean(ARG_NEW_CRIME, newCrime);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mNewCrime = getArguments().getBoolean(ARG_NEW_CRIME);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        setHasOptionsMenu(true);
        mEditable = mNewCrime;
        if (savedInstanceState != null) {
            mNewCrime = savedInstanceState.getBoolean(KEY_NEW_CRIME);
            mEditable = savedInstanceState.getBoolean(KEY_EDITABLE);
        }
        mDf = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        mTf = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH);
        returnResult(crimeId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mDateButton = v.findViewById(R.id.crime_date);
        mDateButton.setEnabled(false);
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                assert manager != null;
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckBox = v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setEnabled(false);
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mTimeButton = v.findViewById(R.id.set_time_button);
        mTimeButton.setEnabled(false);
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                assert manager != null;
                dialog.show(manager, DIALOG_TIME);
            }
        });

        updateDate(mCrime.getDate());

        mEditAlert = v.findViewById(R.id.edit_alert_text);

        updateUI();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
        updateMenu(menu.findItem(R.id.edit_button));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_button:
                if (mEditable) {
                    mEditable = false;
                    updateMenu(item);
                    updateUI();
                    onSave();
                } else {
                    mEditable = true;
                    updateMenu(item);
                    updateUI();
                }
                return true;
            case R.id.delete_button:
                CrimeLab crimeLab = CrimeLab.get(getActivity());
                crimeLab.deleteCrime(mCrime.getId());
                Objects.requireNonNull(getActivity()).finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateMenu(MenuItem editButton) {
        if (mEditable) {
            editButton.setIcon(R.drawable.ic_menu_save);
            editButton.setTitle(R.string.save);
        } else {
            editButton.setIcon(R.drawable.ic_menu_edit);
            editButton.setTitle(R.string.edit_crime);
        }
    }

    private void updateUI() {
        if (mEditable) {
            mTitleField.setEnabled(true);
            mDateButton.setEnabled(true);
            mTimeButton.setEnabled(true);
            mSolvedCheckBox.setEnabled(true);
            mEditAlert.setVisibility(View.INVISIBLE);
        } else {
            mTitleField.setEnabled(false);
            mDateButton.setEnabled(false);
            mTimeButton.setEnabled(false);
            mSolvedCheckBox.setEnabled(false);
            mEditAlert.setVisibility(View.VISIBLE);
        }
    }

    private void onSave() {
        mCrime.setTitle(mTitleField.getText().toString());
        mCrime.setDate(mDate);
        mCrime.setSolved(mSolvedCheckBox.isChecked());
        mNewCrime = false;
        returnResult(mCrime.getId());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_DATE) {
            assert data != null;
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            assert date != null;
            updateDate(date);
        }
        if (requestCode == REQUEST_TIME) {
            assert data != null;
            Date date = (Date) data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);
            assert date != null;
            updateDate(date);
        }
    }

    private void updateDate(Date date) {
        mDate = date;
        mDateButton.setText(mDf.format(date));
        mTimeButton.setText(mTf.format(date));
    }

    private void returnResult(UUID crimeId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        if (!mNewCrime) {
            Objects.requireNonNull(getActivity()).setResult(Activity.RESULT_OK, intent);
        } else {
            Objects.requireNonNull(getActivity()).setResult(Activity.RESULT_CANCELED, intent);
        }
    }

    static UUID getCrimeId(Intent intent) {
        return (UUID) intent.getSerializableExtra(EXTRA_CRIME_ID);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_EDITABLE, mEditable);
        outState.putBoolean(KEY_NEW_CRIME, mNewCrime);
    }
}
