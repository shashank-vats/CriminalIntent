package com.example.criminalintent;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
    private static CrimeLab sCrimeLab;
    private LinkedHashMap<UUID, Crime> mCrimeIdMap;
    private List<Crime> mCrimes;

    static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    private CrimeLab(Context context) {
        mCrimeIdMap = new LinkedHashMap<>();
        mCrimes = new ArrayList<>();
    }

    List<Crime> getCrimes() {
        return mCrimes;
    }

    Crime getCrime(UUID id) {
        return mCrimeIdMap.get(id);
    }

    void addCrime(Crime c) {
        mCrimeIdMap.put(c.getId(), c);
        mCrimes.add(c);
    }

    void deleteCrime(UUID id) {
        mCrimeIdMap.remove(id);
        for (int i = 0; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(id)) {
                mCrimes.remove(i);
                break;
            }
        }
    }

    int getSize() {
        return mCrimes.size();
    }
}
