/*
 * Copyright (C) 2012-2015 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiste.greg.ptw;

import java.util.HashMap;
import java.util.Map;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.meiste.greg.ptw.view.ObservableScrollView;
import com.meiste.greg.ptw.view.ObservableScrollView.ScrollViewListener;

public class RaceFragment extends Fragment implements ScrollViewListener, OnMapReadyCallback {
    private static final String ARG_RACE_ID = "race_id";
    private static final String MAP_FRAGMENT_TAG = "map";

    private Race mRace;
    private SupportMapFragment mMapFragment;
    private ObservableScrollView mScrollView;

    public static RaceFragment newInstance(final int race_id) {
        final RaceFragment fragment = new RaceFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_RACE_ID, race_id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
            final ViewGroup container, final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final View v = inflater.inflate(R.layout.race_details, container, false);

        Util.log("RaceFragment: " + ARG_RACE_ID + "=" + args.getInt(ARG_RACE_ID));
        mRace = Race.getInstance(getActivity(), args.getInt(ARG_RACE_ID));

        mScrollView = (ObservableScrollView) v.findViewById(R.id.scroll);

        final TextView raceNum = (TextView) v.findViewById(R.id.race_num);
        final TextView inTheChase = (TextView) v.findViewById(R.id.race_in_chase);
        final TextView trackSize = (TextView) v.findViewById(R.id.race_track_size);
        final TextView startTime = (TextView) v.findViewById(R.id.race_time);
        final TextView name = (TextView) v.findViewById(R.id.race_name);
        final TextView trackLong = (TextView) v.findViewById(R.id.race_track);
        final TextView tv = (TextView) v.findViewById(R.id.race_tv);
        final TextView cityState = (TextView) v.findViewById(R.id.race_citystate);

        if (mRace.isExhibition()) {
            raceNum.setVisibility(View.GONE);
            inTheChase.setVisibility(View.GONE);
        } else {
            raceNum.setVisibility(View.VISIBLE);
            raceNum.setText(getString(R.string.race_num, mRace.getRaceNum()));

            if (mRace.isInChase())
                inTheChase.setVisibility(View.VISIBLE);
        }
        trackSize.setText(mRace.getTrackSize(getActivity()));
        startTime.setText(mRace.getStartDateTime(getActivity()));
        name.setText(mRace.getName());
        trackLong.setText(mRace.getTrack(Race.NAME_LONG));
        tv.setText(getString(R.string.details_tv, mRace.getTv()));
        cityState.setText(mRace.getCityState());

        mMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mMapFragment == null) {
            final GoogleMapOptions options = new GoogleMapOptions()
                    .mapType(GoogleMap.MAP_TYPE_SATELLITE)
                    .zoomControlsEnabled(false)
                    .rotateGesturesEnabled(false)
                    .scrollGesturesEnabled(false)
                    .tiltGesturesEnabled(false)
                    .zoomGesturesEnabled(false)
                    .mapToolbarEnabled(false);
            mMapFragment = SupportMapFragment.newInstance(options);

            final FragmentTransaction fragmentTransaction =
                    getChildFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.map_fragment_holder, mMapFragment, MAP_FRAGMENT_TAG);
            fragmentTransaction.commit();
        }

        mMapFragment.getMapAsync(this);
        return v;
    }

    private boolean isWorkaroundNeeded(final ViewGroup vg) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return true;
        }

        boolean isNeeded = true;

        for (int i = 0; (i < vg.getChildCount()) && isNeeded; ++i) {
            final View v = vg.getChildAt(i);
            if (v instanceof ViewGroup) {
                isNeeded = isWorkaroundNeeded((ViewGroup) v);
            } else {
                isNeeded = !(v instanceof TextureView);
            }
        }

        return isNeeded;
    }

    @Override
    public void onScrollChanged(final ObservableScrollView sv, final int x, final int y, final int oldx, final int oldy) {
        /* WORKAROUND: Toggle view visibility to force re-layout, preventing map
         * from blacking out part of layout.
         * See http://stackoverflow.com/q/13793483/1620158 for more details. */
        if (y != oldy) {
            sv.setVisibility(View.GONE);
            sv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isWorkaroundNeeded((ViewGroup) mMapFragment.getView())) {
            // Workaround not needed on Android 4.1+ devices using TextureView:
            // https://code.google.com/p/gmaps-api-issues/issues/detail?id=4659#c35
            Util.log("Google Map scrolling workaround needed");

            mScrollView.setScrollViewListener(this);

            if (getActivity() instanceof RaceActivity) {
                ((RaceActivity) getActivity()).onWorkaroundNeeded();
            }
        }

        if (mRace != null && mRace.isRecent()) {
            RaceAlarm.clearNotification(getActivity().getApplicationContext());
        }
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        // GoogleMap object may be ready, but on Android 4.0 test device, it was not ready to move
        // the camera. Wait until the initial camera move occurs (which will signal that the map
        // is ready for camera moves), then set the correct camera position for the track.
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(final CameraPosition cameraPosition) {
                // Only care about initial camera move, so unset the camera change listener
                map.setOnCameraChangeListener(null);

                // Move the map
                final LatLngBounds bounds = mBounds.get(mRace.getAbbr());
                if (bounds != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                }
            }
        });
    }

    private static final LatLngBounds ATLANTA = new LatLngBounds.Builder()
    .include(new LatLng(33.386643, -84.317969)).include(new LatLng(33.382935, -84.312690))
    .include(new LatLng(33.380641, -84.318140)).include(new LatLng(33.384475, -84.322947)).build();
    private static final LatLngBounds BRISTOL = new LatLngBounds.Builder()
    .include(new LatLng(36.517854, -82.257827)).include(new LatLng(36.515560, -82.254737))
    .include(new LatLng(36.513663, -82.256754)).include(new LatLng(36.516181, -82.259554)).build();
    private static final LatLngBounds CHARLOTTE = new LatLngBounds.Builder()
    .include(new LatLng(35.356069, -80.682352)).include(new LatLng(35.351816, -80.679949))
    .include(new LatLng(35.347161, -80.683704)).include(new LatLng(35.351641, -80.686494)).build();
    private static final LatLngBounds CHICAGOLAND = new LatLngBounds.Builder()
    .include(new LatLng(41.478490, -88.060061)).include(new LatLng(41.471416, -88.053195))
    .include(new LatLng(41.475049, -88.061520)).include(new LatLng(41.474052, -88.051950)).build();
    private static final LatLngBounds DARLINGTON = new LatLngBounds.Builder()
    .include(new LatLng(34.297182, -79.905413)).include(new LatLng(34.294753, -79.900907))
    .include(new LatLng(34.293052, -79.905070)).include(new LatLng(34.295853, -79.910198)).build();
    private static final LatLngBounds DAYTONA = new LatLngBounds.Builder()
    .include(new LatLng(29.191619, -81.069195)).include(new LatLng(29.187348, -81.062114))
    .include(new LatLng(29.178244, -81.070611)).include(new LatLng(29.181578, -81.075589)).build();
    private static final LatLngBounds DOVER = new LatLngBounds.Builder()
    .include(new LatLng(39.193050, -75.530752)).include(new LatLng(39.188809, -75.527447))
    .include(new LatLng(39.186198, -75.530151)).include(new LatLng(39.189541, -75.532940)).build();
    private static final LatLngBounds FONTANA = new LatLngBounds.Builder()
    .include(new LatLng(34.091940, -117.500564)).include(new LatLng(34.089221, -117.493161))
    .include(new LatLng(34.085560, -117.500521)).include(new LatLng(34.089381, -117.507730)).build();
    private static final LatLngBounds HOMESTEAD = new LatLngBounds.Builder()
    .include(new LatLng(25.455229, -80.409175)).include(new LatLng(25.452749, -80.404068))
    .include(new LatLng(25.448797, -80.410055)).include(new LatLng(25.450928, -80.413767)).build();
    private static final LatLngBounds INDIANAPOLIS = new LatLngBounds.Builder()
    .include(new LatLng(39.801909, -86.234657)).include(new LatLng(39.795150, -86.230194))
    .include(new LatLng(39.788126, -86.234485)).include(new LatLng(39.794985, -86.239378)).build();
    private static final LatLngBounds KANSAS = new LatLngBounds.Builder()
    .include(new LatLng(39.119873, -94.829914)).include(new LatLng(39.116110, -94.826566))
    .include(new LatLng(39.111382, -94.831073)).include(new LatLng(39.115811, -94.834506)).build();
    private static final LatLngBounds KENTUCKY = new LatLngBounds.Builder()
    .include(new LatLng(38.714732, -84.914921)).include(new LatLng(38.712321, -84.911273))
    .include(new LatLng(38.707030, -84.917024)).include(new LatLng(38.710446, -84.920307)).build();
    private static final LatLngBounds LAS_VEGAS = new LatLngBounds.Builder()
    .include(new LatLng(36.276403, -115.011109)).include(new LatLng(36.273497, -115.004972))
    .include(new LatLng(36.268688, -115.011023)).include(new LatLng(36.271456, -115.015829)).build();
    private static final LatLngBounds LOUDON = new LatLngBounds.Builder()
    .include(new LatLng(43.366062, -71.459330)).include(new LatLng(43.363379, -71.458107))
    .include(new LatLng(43.359369, -71.461540)).include(new LatLng(43.361647, -71.463278)).build();
    private static final LatLngBounds MARTINSVILLE = new LatLngBounds.Builder()
    .include(new LatLng(36.635934, -79.851209)).include(new LatLng(36.634608, -79.850104))
    .include(new LatLng(36.632103, -79.852132)).include(new LatLng(36.633222, -79.853462)).build();
    private static final LatLngBounds MICHIGAN = new LatLngBounds.Builder()
    .include(new LatLng(42.073030, -84.239651)).include(new LatLng(42.067168, -84.236562))
    .include(new LatLng(42.061019, -84.242012)).include(new LatLng(42.067359, -84.245531)).build();
    private static final LatLngBounds PHOENIX = new LatLngBounds.Builder()
    .include(new LatLng(33.374101, -112.314909)).include(new LatLng(33.375678, -112.307163))
    .include(new LatLng(33.376914, -112.311068)).include(new LatLng(33.372578, -112.310875)).build();
    private static final LatLngBounds POCONO = new LatLngBounds.Builder()
    .include(new LatLng(41.060683, -75.509469)).include(new LatLng(41.053693, -75.500113))
    .include(new LatLng(41.049939, -75.503847)).include(new LatLng(41.056217, -75.518052)).build();
    private static final LatLngBounds RICHMOND = new LatLngBounds.Builder()
    .include(new LatLng(37.594095, -77.419208)).include(new LatLng(37.592616, -77.416139))
    .include(new LatLng(37.590601, -77.419218)).include(new LatLng(37.592131, -77.422469)).build();
    private static final LatLngBounds SONOMA = new LatLngBounds.Builder()
    .include(new LatLng(38.166870, -122.461620)).include(new LatLng(38.161961, -122.451943))
    .include(new LatLng(38.158654, -122.454582)).include(new LatLng(38.163446, -122.463959)).build();
    private static final LatLngBounds TALLADEGA = new LatLngBounds.Builder()
    .include(new LatLng(33.574940, -86.067073)).include(new LatLng(33.563497, -86.060292))
    .include(new LatLng(33.559170, -86.064154)).include(new LatLng(33.566930, -86.071922)).build();
    private static final LatLngBounds TEXAS = new LatLngBounds.Builder()
    .include(new LatLng(33.040939, -97.281110)).include(new LatLng(33.036406, -97.278234))
    .include(new LatLng(33.032161, -97.282268)).include(new LatLng(33.036082, -97.285315)).build();
    private static final LatLngBounds WATKINS_GLEN = new LatLngBounds.Builder()
    .include(new LatLng(42.344430, -76.926061)).include(new LatLng(42.337230, -76.920397))
    .include(new LatLng(42.328505, -76.923959)).include(new LatLng(42.337420, -76.929151)).build();

    private static final Map<String, LatLngBounds> mBounds = new HashMap<>();
    static {
        mBounds.put("ams", ATLANTA);
        mBounds.put("bms", BRISTOL);
        mBounds.put("lms", CHARLOTTE);
        mBounds.put("chi", CHICAGOLAND);
        mBounds.put("dar", DARLINGTON);
        mBounds.put("dis", DAYTONA);
        mBounds.put("dov", DOVER);
        mBounds.put("cal", FONTANA);
        mBounds.put("hms", HOMESTEAD);
        mBounds.put("ims", INDIANAPOLIS);
        mBounds.put("kan", KANSAS);
        mBounds.put("ken", KENTUCKY);
        mBounds.put("las", LAS_VEGAS);
        mBounds.put("nhi", LOUDON);
        mBounds.put("mar", MARTINSVILLE);
        mBounds.put("mis", MICHIGAN);
        mBounds.put("pir", PHOENIX);
        mBounds.put("poc", POCONO);
        mBounds.put("rir", RICHMOND);
        mBounds.put("spr", SONOMA);
        mBounds.put("tal", TALLADEGA);
        mBounds.put("tms", TEXAS);
        mBounds.put("wgi", WATKINS_GLEN);
    }
}