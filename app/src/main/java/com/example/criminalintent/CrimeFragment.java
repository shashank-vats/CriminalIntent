package com.example.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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
    private boolean mContactAppInstalled;
    private File mPhotoFile;
    private boolean mCanTakePhoto;
    private Callbacks mCallbacks;

    private Button mDateButton;
    private EditText mTitleField;
    private CheckBox mSolvedCheckBox;
    private Button mTimeButton;
    private TextView mEditAlert;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mCallSuspectButton;
    private ImageView mPhotoButton;
    private ImageView mPhotoView;

    private static final String DATE_FORMAT = "EEEE, MMM dd, yyyy";
    private static final String TIME_FORMAT = "hh:mm a";

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String ARG_NEW_CRIME = "new_crime";

    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final String DIALOG_PHOTO = "DialogPhoto";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;
    private static final int REQUEST_PHOTO = 3;

    private static final String KEY_NEW_CRIME = "new_crime";
    private static final String KEY_EDITABLE = "editable";

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 5;

    public interface Callbacks {
        void onCrimeUpdated(Crime crime);
    }

    static CrimeFragment newInstance(UUID crimeId, boolean newCrime) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);
        args.putBoolean(ARG_NEW_CRIME, newCrime);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mNewCrime = getArguments().getBoolean(ARG_NEW_CRIME);
        if (mNewCrime) {
            mCrime = new Crime(crimeId);
            mCrime.setDate(new Date());
        } else {
            assert crimeId != null;
            mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        }
        setHasOptionsMenu(true);
        mEditable = mNewCrime;
        if (savedInstanceState != null) {
            mNewCrime = savedInstanceState.getBoolean(KEY_NEW_CRIME);
            mEditable = savedInstanceState.getBoolean(KEY_EDITABLE);
        }
        mDf = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        mTf = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        mReportButton = v.findViewById(R.id.crime_report_button);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEditable) {
                    ShareCompat.IntentBuilder intentBuilder =
                            ShareCompat.IntentBuilder.from(Objects.requireNonNull(getActivity()));
                    intentBuilder.setType("text/plain");
                    intentBuilder.setText(getCrimeReport());
                    intentBuilder.setSubject(getString(R.string.crime_report_subject));
                    intentBuilder.setChooserTitle(R.string.send_report);
                    Intent i = intentBuilder.createChooserIntent();
                    startActivity(i);
                } else {
                    Toast.makeText(getActivity(), R.string.send_helper_text, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mSuspectButton = v.findViewById(R.id.crime_suspect_button);
        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()),
                        Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.READ_CONTACTS)) {
                        Toast.makeText(getActivity(), R.string.read_contact_permission_reason, Toast.LENGTH_SHORT).show();
                    } else {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.READ_CONTACTS},
                                PERMISSIONS_REQUEST_READ_CONTACTS);
                    }
                } else {
                    startActivityForResult(pickContact, REQUEST_CONTACT);
                }
            }
        });

        PackageManager packageManager = Objects.requireNonNull(getActivity()).getPackageManager();
        mContactAppInstalled = packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) != null;

        mCallSuspectButton = v.findViewById(R.id.call_suspect_button);
        mCallSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditable) {
                    Toast.makeText(getActivity(), R.string.call_suspect_helper_text, Toast.LENGTH_SHORT).show();
                } else {
                    String phoneNumber = getPhoneNumber();
                    if (phoneNumber == null) return;
                    Intent i = new Intent(Intent.ACTION_DIAL);
                    Uri number = Uri.parse("tel:" + phoneNumber);
                    i.setData(number);
                    startActivity(i);
                }
            }
        });

        mPhotoButton = v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        mCanTakePhoto = mPhotoFile != null && captureImage.resolveActivity(packageManager) != null;

        mPhotoView = v.findViewById(R.id.crime_photo);
        ViewTreeObserver observer = mPhotoView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updatePhotoView();
            }
        });
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEditable) {
                    FragmentManager manager = getFragmentManager();
                    PhotoViewerFragment dialog = PhotoViewerFragment.newInstance(mPhotoFile.getPath());
                    assert manager != null;
                    dialog.show(manager, DIALOG_PHOTO);
                } else {
                    Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getActivity()),
                            "com.example.criminalintent.fileprovider",
                            mPhotoFile);
                    captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                    List<ResolveInfo> cameraActivities = getActivity()
                            .getPackageManager().queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY);

                    for (ResolveInfo activity : cameraActivities) {
                        getActivity().grantUriPermission(activity.activityInfo.packageName,
                                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    Toast.makeText(getActivity(), R.string.photo_click_advice, Toast.LENGTH_SHORT).show();
                    startActivityForResult(captureImage, REQUEST_PHOTO);
                }
            }
        });
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
                    onSave();
                    updateUI();
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
        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(getString(R.string.suspect_information, mCrime.getSuspect()));
        }
        if (mEditable) {
            mTitleField.setEnabled(true);
            mDateButton.setEnabled(true);
            mTimeButton.setEnabled(true);
            mSolvedCheckBox.setEnabled(true);
            mSuspectButton.setEnabled(true);
            mPhotoButton.setVisibility(View.VISIBLE);
            mEditAlert.setVisibility(View.INVISIBLE);
        } else {
            mTitleField.setEnabled(false);
            mDateButton.setEnabled(false);
            mTimeButton.setEnabled(false);
            mSolvedCheckBox.setEnabled(false);
            mSuspectButton.setEnabled(false);
            mPhotoButton.setVisibility(View.INVISIBLE);
            mEditAlert.setVisibility(View.VISIBLE);
        }
        if (!mContactAppInstalled) {
            mSuspectButton.setEnabled(false);
        }
        if (mCrime.getSuspect() == null) {
            mCallSuspectButton.setEnabled(false);
        }
        if (!mCanTakePhoto && mEditable) {
            mPhotoView.setEnabled(false);
        }
        if (!mEditable && (mPhotoFile == null || !mPhotoFile.exists())) {
            mPhotoView.setEnabled(false);
        }
    }

    private void onSave() {
        mCrime.setTitle(mTitleField.getText().toString());
        mCrime.setDate(mDate);
        mCrime.setSolved(mSolvedCheckBox.isChecked());
        if (!mNewCrime) {
            CrimeLab.get(getActivity()).updateCrime(mCrime);
        } else {
            CrimeLab.get(getActivity()).addCrime(mCrime);
        }
        mNewCrime = false;
        mCallbacks.onCrimeUpdated(mCrime);
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
        } else if (requestCode == REQUEST_TIME) {
            assert data != null;
            Date date = (Date) data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);
            assert date != null;
            Calendar calendarDate = addDateTime(mDate, date);
            updateDate(calendarDate.getTime());
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            String[] queryFields = new String[]{
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.LOOKUP_KEY
            };
            assert contactUri != null;
            try (Cursor c = Objects.requireNonNull(getActivity()).getContentResolver().
                    query(contactUri, queryFields, null, null, null)) {
                assert c != null;
                if (c.getCount() == 0) {
                    return;
                }

                c.moveToFirst();
                String suspect = c.getString(0);
                String lookupKey = c.getString(1);
                mCrime.setSuspect(suspect);
                mCrime.setSuspectLookupKey(lookupKey);
                mSuspectButton.setText(getString(R.string.suspect_information, suspect));
                mCallSuspectButton.setEnabled(true);
            }
        } else if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getActivity()),
                    "com.example.criminalintent.fileprovider",
                    mPhotoFile);

            getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            updatePhotoView();
        }
    }

    private static Calendar addDateTime(Date date, Date time) {
        Calendar calendarDate = GregorianCalendar.getInstance();
        calendarDate.setTime(date);
        Calendar calendarTime = GregorianCalendar.getInstance();
        calendarTime.setTime(time);
        calendarDate.set(Calendar.HOUR_OF_DAY, calendarTime.get(Calendar.HOUR_OF_DAY));
        calendarDate.set(Calendar.MINUTE, calendarTime.get(Calendar.MINUTE));
        return calendarDate;
    }

    private void updateDate(Date date) {
        mDate = date;
        mDateButton.setText(mDf.format(date));
        mTimeButton.setText(mTf.format(date));
        mCrime.setDate(date);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_EDITABLE, mEditable);
        outState.putBoolean(KEY_NEW_CRIME, mNewCrime);
    }

    private String getCrimeReport() {
        String solvedString;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateString = mDf.format(mCrime.getDate());

        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        return getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);
    }

    private String getPhoneNumber() {
        String phoneNumber = null;
        Uri lookupUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String[] queryFields = new String[]{
                ContactsContract.Data.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
        };

        String selectionClause = ContactsContract.Data.LOOKUP_KEY + " = ?";
        String[] selectionArgs = {mCrime.getSuspectLookupKey()};

        ContentResolver contentResolver = Objects.requireNonNull(getActivity()).getContentResolver();

        try (Cursor c = contentResolver.query(lookupUri,
                queryFields, selectionClause, selectionArgs, null)) {
            assert c != null;
            if (c.getCount() == 0) {
                return null;
            }

            while (c.moveToNext()) {
                int phoneType = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE ||
                        phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_HOME ||
                        phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_WORK) {
                    phoneNumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    break;
                }
            }
        }
        return phoneNumber;
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), mPhotoView);
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }
}
