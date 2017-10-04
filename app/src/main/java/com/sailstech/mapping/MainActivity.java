package com.sailstech.mapping;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.nineoldandroids.animation.Animator;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sails.engine.LocationRegion;
import com.sails.engine.SAILS;
import com.sails.engine.SAILSMapView;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int NO_MODE = 0x0000;
    private static final int POI_MAPPING = 0x0001;
    private static final int POI_MAPPING_DETAIL = 0x1001;
    private static final int CEHCK_POINT_MAPPING_DETAIL = 0x2001;
    private static final int BEACON_PATROLLING = 0x0002;
    private static final int ACCURACY_VERIFICATION = 0x004;
    private static final int QR_CODE_SCAN = 0x02;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0x01;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private ParseObject projectObject=null;
    private SAILS mSails;
    private SAILSMapView mSailsMapView;
    private MaterialSpinner spinner;
    private int currentMode=0;
    private LinearLayout poi_detail_view;
    private LinearLayout check_point_detail_view;

    private LinearLayout beacon_patrolling_subview;
    private FrameLayout fl_subwin, fl_subwin_fade;
    private Snackbar snackbar =null;
    private ImageView lockcenter;
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private Button uploadPicture;
    private String ipsmap_cloud_buildingid;
    private String hospitalName;


    @Override
    protected void onDestroy() {
        MyApplication.Mixpanel.flush();
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getResources().getBoolean(R.bool.isTablet))
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);

        toggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        try {
            String version=getString(R.string.version)+" "+getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            navigationView.getMenu().findItem(R.id.version).setTitle(version);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        initSAILS();
        initUI();
        AddToolbarProcedure();
        iconDrawableProcedure();
        userLoginProcedure();
        checkFineLocationPermissionRoutine();

    }

    private void initSAILS() {
        mSails= new SAILS(this);
        mSails.setGPSThresholdParameter(2,3,-80); //in_to_out,out_to_in,beacon min power
        mSails.setEnvironmentIsHighBeaconDensity(true);//v1.51

        mSails.setMode(SAILS.BLE_GFP_IMU|SAILS.WITH_GPS);
        mSails.setSmoothChangeFloor(true);
        mSails.setReverseFloorList(true);
        mSails.getCurrentInRegions();
        mSailsMapView = new SAILSMapView(this);
        mSailsMapView.setSAILSEngine(mSails);
        //58fae95fcba5c23cd9e3009d
        //58fae95fcba5c23cd9e3009d
        mSailsMapView.setOnRegionClickListener(new SAILSMapView.OnRegionClickListener() {
            @Override
            public void onClick(List<LocationRegion> list) {
                LocationRegion locationRegion = list.get(0);
                String type = locationRegion.type;
                String subtype1 = locationRegion.subtype;
                if (type!= null && subtype1 != null &&!type.isEmpty()&&!subtype1.isEmpty()){
                    int self = locationRegion.self;
                    //,String buildingId ,int  selfId
                    Intent intent = UploadPicturesActivity.getIntent(MainActivity.this, ipsmap_cloud_buildingid, self,hospitalName);
                    startActivity(intent);
                }
            }
        });

        mSails.setOnLocationChangeEventListener(new SAILS.OnLocationChangeEventListener() {
            boolean first=false;
            @Override
            public void OnLocationChange() {
                if (mSailsMapView.isCenterLock() && !mSailsMapView.isInLocationFloor() && mSails.isLocationFix()) {
                    //set the map that currently location engine recognize.
                    mSailsMapView.loadCurrentLocationFloorMap();
                    mSailsMapView.setAnimationToZoom((byte) 19);
                }
                if (mSails.isLocationFix()) {
                    if(!first) {
                        mSailsMapView.setMode(mSailsMapView.getMode() | SAILSMapView.LOCATION_CENTER_LOCK);
                        first=true;
                    }

                }
            }
        });
    }

    private void initUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ((FrameLayout) findViewById(R.id.frameLayout)).addView(mSailsMapView);
        spinner = (MaterialSpinner) findViewById(R.id.spFloor);
        fl_subwin =(FrameLayout)findViewById(R.id.fl_poi);
        fl_subwin_fade =(FrameLayout)findViewById(R.id.fl_poi_fade);
        lockcenter=(ImageView)findViewById(R.id.lockcenter);
//        uploadPicture.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getBaseContext(), UploadPicturesActivity.class);
////                intent.setClassName()
//                startActivity(intent);
//            }
//        });
        lockcenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set map mode.
                //FOLLOW_PHONE_HEADING: the map follows the phone's heading.
                //LOCATION_CENTER_LOCK: the map locks the current location in the center of map.
                //ALWAYS_LOCK_MAP: the map will keep the mode even user moves the map.
                if(!mSails.isLocationEngineStarted()) {
                    startPositioningProcedure();
                    return;
                }
                if (mSailsMapView.isCenterLock()) {
                    if ((mSailsMapView.getMode() & SAILSMapView.FOLLOW_PHONE_HEADING) == SAILSMapView.FOLLOW_PHONE_HEADING)
                        //if map control mode is follow phone heading, then set mode to location center lock when button click.
                        mSailsMapView.setMode(mSailsMapView.getMode() & ~SAILSMapView.FOLLOW_PHONE_HEADING);
                    else {
                        //if map control mode is location center lock, then set mode to follow phone heading when button click.
                        mSailsMapView.setMode(mSailsMapView.getMode() | SAILSMapView.FOLLOW_PHONE_HEADING);
                    }
                } else {
                    //if map control mode is none, then set mode to loction center lock when button click.
                    mSailsMapView.setMode(mSailsMapView.getMode() | SAILSMapView.LOCATION_CENTER_LOCK);
                }
            }
        });

    }
    public void onZoomInClick(View v) {

        mSailsMapView.zoomIn();
//        ((FrameLayout)placeholderFragment.rootView.findViewById(R.id.frameLayout)).removeView(mSailsMapView);
    }

    public void onZoomOutClick(View v) {
        mSailsMapView.zoomOut();
//        ((FrameLayout)placeholderFragment.rootView.findViewById(R.id.frameLayout)).addView(mSailsMapView);

    }
    private void AddToolbarProcedure() {
        findViewById(R.id.b_poi_layers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                showPOISelectLayers();
            }
        });
        findViewById(R.id.b_poi_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                POI poi=POIMapping.AddNewPOI(projectObject,mSailsMapView,generalIconMap,highlightIconMap);
                showPOIDetailView(poi);
            }
        });
        findViewById(R.id.b_poi_add_check_point).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                POI poi=POIMapping.AddNewCheckPoint(projectObject,mSailsMapView,generalIconMap,highlightIconMap,checkIconMap);
                showCheckPointDetailView(poi);
            }
        });
        findViewById(R.id.b_poi_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                showSearchView();
            }
        });
        findViewById(R.id.b_check_point_email).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                emailMeasurementResults();
            }
        });
        findViewById(R.id.b_clear_measurement_results).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                clearMeasurementResults();
            }
        });

        //beacon patrolling toolbar
        findViewById(R.id.b_beacon_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(snackbar!=null)
                    snackbar.dismiss();
                showBeaconSearchView();
            }
        });

    }

    private void clearMeasurementResults() {
        new MaterialDialog.Builder(this)
                .title(R.string.dialog_delete_result)
                .positiveText(R.string.dialog_yes)
                .negativeText(R.string.dialog_no)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        AccVerify.ClearMeasurementResult(mSailsMapView);
                        ((TextView)findViewById(R.id.tvSample)).setText("0");
                        ((TextView)findViewById(R.id.tvDeviation)).setText("-");
                        ((TextView)findViewById(R.id.tvAccuracy)).setText("-");

                    }
                })
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void showBeaconSearchView() {
        new MaterialDialog.Builder(this)
                .input(R.string.beacon_search_hint, 0, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        BeaconPatrolling.Beacon beacon=BeaconPatrolling.getBeaconByMACLast4byte(input.toString());
                        if(beacon!=null) {
                            BeaconPatrolling.ShowThisBeacon(beacon,mSailsMapView);
                        } else {
                            GeneralUsage.ShowErrorDialog(MainActivity.this,getString(R.string.beacon_not_found));
                        }
                    }
                })
                .inputRange(4,4)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .positiveText(R.string.dialog_search)
                .negativeText(R.string.dialog_cancel)
                .show();
    }

    private void emailMeasurementResults() {
        new MaterialDialog.Builder(this)
                .title(R.string.measurement_title_hint)
                .input(0, 0, true, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        String fileName=input.toString();
                        if(fileName.length()==0)
                            fileName="output";
                        File file=AccVerify.SaveToCSVFile(MainActivity.this,projectObject,fileName);
                        Uri path = Uri.fromFile(file);
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
// set the type to 'email'
                        emailIntent .setType("vnd.android.cursor.dir/email");
// the attachment
                        emailIntent .putExtra(Intent.EXTRA_STREAM, path);
// the mail subject
                        emailIntent .putExtra(Intent.EXTRA_SUBJECT, projectObject.getString("name")+" Measurement Result");
                        startActivity(Intent.createChooser(emailIntent , "Send email..."));
                    }
                })
                .inputType(InputType.TYPE_CLASS_TEXT)
                .positiveText(R.string.dialog_ok)
                .negativeText(R.string.dialog_cancel)
                .show();
    }

    private void showSearchView() {
        new MaterialDialog.Builder(this)
                .input(R.string.poi_search_hint, 0, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        POI poi=POIMapping.getCheckPointById(input.toString());
                        if(poi!=null) {
                            final HashMap<String, Boolean> layers = POIMapping.getShowLayers();
                            layers.put("check_point",true);
                            mSailsMapView.loadFloorMap(poi.getString("floor"));
                            mSailsMapView.setAnimationMoveMapTo(poi.getGeoPoint());
                            mSailsMapView.redraw();
                        } else {
                            GeneralUsage.ShowErrorDialog(MainActivity.this,getString(R.string.check_point_not_found));
                        }
                    }
                })
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .positiveText(R.string.dialog_search)
                .negativeText(R.string.dialog_cancel)
                .show();
    }

    private void showPOISelectLayers() {
        final HashMap<String, Boolean> layers = POIMapping.getShowLayers();
        List<String> layerNames=new ArrayList<>();
        ArrayList<Integer> on=new ArrayList<>();
        int i=0;
        for(Map.Entry<String, Boolean> entry : layers.entrySet()) {
            String key = entry.getKey();
            boolean value = entry.getValue();
            layerNames.add(key);
            if(value) {
                on.add(i);
            }
            i++;
        }
        Integer[] onArray = new Integer[on.size()];
        onArray = on.toArray(onArray);
        new MaterialDialog.Builder(this)
                .title(R.string.poi_visible_layer_title)
                .items(layerNames)
                .itemsCallbackMultiChoice(onArray, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        int i=0;
                        for(Map.Entry<String, Boolean> entry : layers.entrySet()) {
                            String key = entry.getKey();
                            boolean select=false;
                            for(int a : which) {
                                if(i==a) {
                                    select = true;
                                    if(key.equals("rss_fingerprint")) {
                                        POIMapping.setRssfingerprintList(mSails.getFingerPrintLists());
                                    }
                                }
                            }
                            layers.put(key,select);
                            i++;
                        }
                        POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                        return false;
                    }
                })
                .positiveText(R.string.dialog_ok)
                .negativeText(R.string.dialog_cancel)
                .show();
    }


    private void fetchProjectParameter(String id, final boolean selectProjectIfFail) {
        ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("MappingProject");
        query.fromLocalDatastore();
        query.getInBackground(id, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if(e==null&&object!=null) {
                    projectObject=object;
                    loadProjectProcedure(object);
                } else if(selectProjectIfFail){
                    SharedPreferences sp = getSharedPreferences("cloud", MODE_PRIVATE);
                    sp.edit().putString("projectId","").commit();
                    selectProjectProcedure();
                }
            }
        });

    }

    private void loadProjectProcedure(ParseObject object) {
        View view=navigationView.getHeaderView(0);
        SimpleDraweeView icon = (SimpleDraweeView) view.findViewById(R.id.profile_image);
        SimpleDraweeView background = (SimpleDraweeView) view.findViewById(R.id.header_background);
        RoundingParams roundingParams = RoundingParams.fromCornersRadius(5f);
        roundingParams.setBorder(0xffffffff, GeneralUsage.dpToPx(MainActivity.this,2));
        roundingParams.setRoundAsCircle(true);
        icon.getHierarchy().setRoundingParams(roundingParams);
        if(object.containsKey("icon")) {
            icon.setImageURI(object.getString("icon"));
        }
        if(object.containsKey("header_img")) {
            background.setImageURI(object.getString("header_img"));
        }
        ActionBar actionBar=getSupportActionBar();
        hospitalName = object.getString("name");
        Log.e("hospitalName",object.getString("name"));
        actionBar.setTitle(object.getString("name"));
        object.pinInBackground("project");
        SharedPreferences sp = getSharedPreferences("cloud", MODE_PRIVATE);
        sp.edit().putString("projectId",object.getObjectId()).apply();
        loadBuildingProcedure(object);

    }

    private void loadBuildingProcedure(final ParseObject object) {
        if(object.getBoolean("use_ipsmap_cloud")) {
            if(!object.containsKey("ipsmap_cloud_token")||
                    !object.containsKey("ipsmap_cloud_buildingid")) {
                new MaterialDialog.Builder(this)
                        .content(R.string.no_buildingid_or_token)
                        .positiveText(R.string.dialog_ok)
                        .show();
                return;
            }
            final MaterialDialog progressing = GeneralUsage.ShowProgressing(MainActivity.this,R.string.dialog_load_map);
            ipsmap_cloud_buildingid = object.getString("ipsmap_cloud_buildingid");
            Log.e("buidnId ", ipsmap_cloud_buildingid);
            mSails.loadCloudBuilding(object.getString("ipsmap_cloud_token"),
                    ipsmap_cloud_buildingid, new SAILS.OnFinishCallback() {
                        @Override
                        public void onSuccess(String s) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    progressing.dismiss();
                                    mSails.setGPSFloorLayer(object.getString("gps_layer"));
                                    mapViewInitial();
                                    generateFloorSpinnerProcedure();
                                }
                            });

                        }

                        @Override
                        public void onFailed(String s) {
                            progressing.dismiss();

                        }
                    });
        }
    }
    void generateFloorSpinnerProcedure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSailsMapView == null)
                    return;
                List<String> floorList = mSails.getFloorDescList();
                if(floorList.size()<=1)
                    spinner.setVisibility(View.GONE);
                else
                    spinner.setVisibility(View.VISIBLE);

                spinner.setItems(floorList);
                spinner.setSelectedIndex(0);
                spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                        mSailsMapView.clear();
                        mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(position));
                        mSailsMapView.getMapViewPosition().setZoomLevel((byte) 18);

                    }
                });
            }
        });
    }
    void mapViewInitial() {
        //establish a connection of SAILS engine into SAILS MapView.
//            mSailsMapView.setSAILSEngine(mSails);
        //set location pointer icon.
//            LocationRegion.FONT_LANGUAGE = LocationRegion.NORMAL;
        Paint accuracyCircleFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        accuracyCircleFill.setStyle(Paint.Style.FILL);
        accuracyCircleFill.setColor(Color.rgb(53, 179, 229));//);
        accuracyCircleFill.setAlpha(0);
        accuracyCircleFill.setStrokeWidth(0);
        accuracyCircleFill.setStrokeJoin(Paint.Join.ROUND);

        mSailsMapView.setLocationMarker(R.drawable.myloc_cir, R.drawable.myloc_arr, accuracyCircleFill, 100);
        //set location marker visible.
        //load first floor map in package.
//        final Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);

        if (!mSails.getFloorNameList().isEmpty()) {
            mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(0));
            mSailsMapView.getMapViewPosition().setZoomLevel((byte)18);
        }
        mSailsMapView.setOnFloorChangedListener(new SAILSMapView.OnFloorChangedListener() {
            @Override
            public void onFloorChangedBefore(String s) {

            }

            @Override
            public void onFloorChangedAfter(String s) {
                switch(currentMode) {
                    case POI_MAPPING_DETAIL:
                    case POI_MAPPING:
                        POIMapping.ShowFloorPOIs(mSailsMapView,s);
                        break;
                    case BEACON_PATROLLING:
                        BeaconPatrolling.ShowFloorBeacons(mSailsMapView,s);
                        break;
                    case ACCURACY_VERIFICATION:
                        AccVerify.ShowFloorPOIs(mSailsMapView,s);
                        break;
                }
                spinner.setSelectedIndex(mSails.getFloorNameList().indexOf(s));
            }
        });
        mSailsMapView.setOnMapClickListener(new SAILSMapView.OnMapClickListener() {
            @Override
            public boolean onClick(int i, int i1) {

                mapTouchEventProcedure(i,i1);
                return false;
            }
        });
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            boolean first=true;
//            int position=1;
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                mSailsMapView.clear();
//
//                if(FLOOR_GUIDE&&i==0&&position!=0) {
//                    int p=position;
//                    position=0;
//                    spinner.setSelection(p);
//                    List<LocationRegion> locationRegionList=mSails.findRegionByLabel("floor_guide");
//
//                    if(locationRegionList!=null&&locationRegionList.size()>0&&!first)
//                        showWebView(mSails.findRegionByLabel("floor_guide").get(0).url);
//                    first=false;
//                    return;
//                }
//                position=i;
//                if(FLOOR_GUIDE) {
//                    i--;
//                }
////                    i--;
//                if(i<0)
//                    return;
//                mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(i));
//                mSailsMapView.getMapViewPosition().setZoomLevel((byte) 18);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });
//        mSailsMapView.setOnRegionClickListener(regionClickListener);
//        mSailsMapView.setOnClickNothingListener(new SAILSMapView.OnClickNothingListener() {
//            @Override
//            public void onClick() {
//                if (mode == MULTIPLE_MARKER_MODE || mode == MULTIPLE_MARKER_WITH_SINGLE_MODE || mode == NAVIGATION_MODE || mode == ARRANGE_MODE)
//                    return;
//                mSailsMapView.getMarkerManager().clear();
//                notificationManager.closeNotification();
//            }
//        });
//
        //design some action in mode change call back.
        mSailsMapView.setOnModeChangedListener(new SAILSMapView.OnModeChangedListener() {
            @Override
            public void onModeChanged(int lockmode) {
                if (((lockmode & SAILSMapView.LOCATION_CENTER_LOCK) == SAILSMapView.LOCATION_CENTER_LOCK) &&
                        ((lockmode & SAILSMapView.FOLLOW_PHONE_HEADING) == SAILSMapView.FOLLOW_PHONE_HEADING)) {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center3));
                } else if ((lockmode & SAILSMapView.LOCATION_CENTER_LOCK) == SAILSMapView.LOCATION_CENTER_LOCK) {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center2));
                } else {
//                    if (mSails.isInThisBuilding() && mode == NAVIGATION_MODE)
//                        backNavi.setVisibility(View.VISIBLE);

                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center1));
                }
            }
        });
    }

    private void mapTouchEventProcedure(int x, int y) {
        switch(currentMode) {
            case POI_MAPPING:
                final POI poi=POIMapping.CheckPOIIsTouched(x,y,mSailsMapView);
                if(poi!=null) {
                    String snackbarName;

                    if(poi.containsKey("type")&&poi.getString("type").equals("check_point")) {
                        if(poi.getString("comment")!=null&&poi.getString("comment").length()>0)
                            snackbarName=poi.getString("comment");
                        else
                            snackbarName=poi.getString("id");
                    } else {
                        snackbarName=poi.getString("name");
                    }
                    snackbar =Snackbar.make(findViewById(R.id.frameLayout), snackbarName, 20000)
                            .setAction(R.string.poi_detail, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if(poi.containsKey("type")&&poi.getString("type").equals("check_point")){
                                        showCheckPointDetailView(poi);
                                    } else {
                                        showPOIDetailView(poi);
                                    }
                                }
                            });
                    snackbar.show();
                }
                break;
            case BEACON_PATROLLING:
                final BeaconPatrolling.Beacon beacon=BeaconPatrolling.CheckBeaconIsTouched(x,y,mSailsMapView);
                if(beacon!=null) {
                    snackbar =Snackbar.make(findViewById(R.id.frameLayout), beacon.getSnackbarMsg(), Snackbar.LENGTH_LONG);
                    snackbar.show();

                }
                break;
            case ACCURACY_VERIFICATION:
                String length=AccVerify.CheckCheckPointIsTouched(x,y,mSails,mSailsMapView);
                if(length.length()!=0) {
                    snackbar =Snackbar.make(findViewById(R.id.frameLayout), length+" m", Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.acc_verify_sample, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AccVerify.Sample(mSailsMapView);
                            ((TextView)findViewById(R.id.tvSample)).setText(AccVerify.Samples);
                            ((TextView)findViewById(R.id.tvDeviation)).setText(AccVerify.Deviation);
                            ((TextView)findViewById(R.id.tvAccuracy)).setText(AccVerify.Accuracy);
                        }
                    });
                    snackbar.show();

                } else {
                    if(snackbar!=null)
                        snackbar.dismiss();
                }

                break;

        }
    }

    private void selectProjectProcedure() {
        SharedPreferences sp = getSharedPreferences("cloud", MODE_PRIVATE);
        String id=sp.getString("projectId","");
        if(id.length()!=0) {
            fetchProjectParameter(id,true);
            return;
        }
        ParseObject.unpinAllInBackground("project", new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                final MaterialDialog progressing = GeneralUsage.ShowProgressing(MainActivity.this,R.string.dialog_progressing);
                ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("MappingProject");
                query.whereEqualTo("visible",true);
                if(ParseUser.getCurrentUser().getBoolean("no_access_public")) {
                   query.whereEqualTo("public",false);
                }
                query.setLimit(1000);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(final List<ParseObject> objects, ParseException e) {
                        progressing.dismiss();
                        if(e!=null&&e.getCode()!=ParseException.OBJECT_NOT_FOUND) {
                            new MaterialDialog.Builder(MainActivity.this)
                                    .title(R.string.dialog_internet_error_title)
                                    .content(R.string.dialog_internet_error_content)
                                    .positiveText(R.string.dialog_ok)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            selectProjectProcedure();

                                        }
                                    })
                                    .negativeText(R.string.dialog_log_out_title)
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            userLogoutProcedure();
                                        }
                                    })
                                    .cancelable(false)
                                    .show();

                            return;
                        }
                        List<String> names=new ArrayList<>();
                        if(e!=null||objects.size()==0) {
                            addProjectProcedure();
                            return;
                        }
                        int i=0;
                        for(ParseObject obj : objects) {
                            names.add(obj.getString("name"));
                            i++;
                        }
                        new MaterialDialog.Builder(MainActivity.this).items(names)
                                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                        projectObject=objects.get(which);
                                        loadProjectProcedure(objects.get(which));
                                        return false;
                                    }
                                })
                                .title(R.string.dialog_project_title)
                                .positiveText(R.string.dialog_select)
                                .cancelable(false)
                                .show();

                    }
                });
            }
        });
    }

    private void addProjectProcedure() {
        new MaterialDialog.Builder(this)
                .content(R.string.add_project_content)
                .positiveText(R.string.dialog_ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .cancelable(false)
                .show();
    }

    private void userLoginProcedure() {
        if(ParseUser.getCurrentUser()==null) {
            GeneralUsage.ShowSignInActivity(this);
        } else {
            ParseUser user=ParseUser.getCurrentUser();
            View view=navigationView.getHeaderView(0);
            SimpleDraweeView drawee = (SimpleDraweeView) view.findViewById(R.id.profile_image);
            RoundingParams roundingParams = RoundingParams.fromCornersRadius(5f);
            roundingParams.setBorder(0xffffffff, GeneralUsage.dpToPx(MainActivity.this,2));
            roundingParams.setRoundAsCircle(true);

            drawee.getHierarchy().setRoundingParams(roundingParams);

            ((TextView)view.findViewById(R.id.tv_user_name)).setText(user.getUsername());
            ((TextView)view.findViewById(R.id.tv_user_email)).setText(user.getEmail());
            selectProjectProcedure();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GeneralUsage.PARSE_LOGIN) {
            if(resultCode!=RESULT_OK) {
                finish();
            } else {
                userLoginProcedure();
            }
        }
        if (requestCode == QR_CODE_SCAN) {
            if (resultCode == RESULT_OK) {
                setQRCodeContent(data.getStringExtra("SCAN_RESULT"));
            }
        }
    }

    private void setQRCodeContent(String scan_result) {
        if(poi_detail_view!=null) {
            EditText et_poi_link=(EditText)poi_detail_view.findViewById(R.id.et_poi_link);
            et_poi_link.setText(scan_result);
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu=menu;
        return true;
    }
    boolean inLocating=false;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
       if(id==R.id.action_positioning) {
           if(!inLocating) {
               startPositioningProcedure();
           } else {
               stopPositioningProcedure();

           }

        }

        return super.onOptionsItemSelected(item);
    }

    private void stopPositioningProcedure() {
        if(!inLocating)
            return;
        mSails.stopLocatingEngine();
        MenuItem menuItem=menu.findItem(R.id.action_positioning);
        menuItem.setTitle(R.string.action_start_positioning);
        menuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_start_positioning));
        inLocating=false;
        mSailsMapView.setLocatorMarkerVisible(false);
        mSailsMapView.invalidate();
        mSailsMapView.setMode(SAILSMapView.GENERAL);


    }

    private void startPositioningProcedure() {
        if(inLocating)
            return;
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return;

        mSails.startLocatingEngine();
        MenuItem menuItem=menu.findItem(R.id.action_positioning);
        menuItem.setTitle(R.string.action_stop_positioning);
        menuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_stop_positioning));

        inLocating=true;
        mSailsMapView.setLocatorMarkerVisible(true);
        mSailsMapView.setMode(SAILSMapView.FOLLOW_PHONE_HEADING|SAILSMapView.LOCATION_CENTER_LOCK);


    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_switch_project) {
            clearMode();
            SharedPreferences sp = getSharedPreferences("cloud", MODE_PRIVATE);
            sp.edit().putString("projectId","").commit();
            selectProjectProcedure();
        }
        else if (id == R.id.nav_logout) {
            userLogoutProcedure();
        }
        else if(id==R.id.nav_poi_mapping) {
            changeMode(POI_MAPPING);
        }
        else if(id==R.id.nav_beacon_patrolling) {
            changeMode(BEACON_PATROLLING);
        }
        else if(id==R.id.nav_accuracy_verification) {
            changeMode(ACCURACY_VERIFICATION);
        }
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void userLogoutProcedure() {
        new MaterialDialog.Builder(this)
                .title(R.string.dialog_confirm_logout)
                .positiveText(R.string.dialog_yes)
                .negativeText(R.string.dialog_no)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        clearMode();
                        final MaterialDialog md= GeneralUsage.ShowProgressing(MainActivity.this,R.string.dialog_progressing);
                        ParseUser.logOutInBackground(new LogOutCallback() {
                            @Override
                            public void done(ParseException e) {
                                md.dismiss();
                                if(e!=null) {
                                    GeneralUsage.ShowErrorDialog(MainActivity.this);
                                    return;
                                }
                                SharedPreferences sp = getSharedPreferences("cloud", MODE_PRIVATE);
                                sp.edit().putString("projectId","").apply();
                                GeneralUsage.ShowSignInActivity(MainActivity.this);
                            }
                        });
                    }
                })
                .show();
    }

    private void clearMode() {
        int size = navigationView.getMenu().size();
        for (int i = 0; i < size; i++) {
            navigationView.getMenu().getItem(i).setChecked(false);
        }
        navigationView.setCheckedItem(R.id.nav_no_mode);
        changeMode(NO_MODE);
        stopPositioningProcedure();
    }

    boolean changeMode(int mode) {
        if((mode&0x0fff)==(currentMode&0xfff)) {
            currentMode=mode;
            return true;
        }
        if(snackbar!=null)
            snackbar.dismiss();
        switch(currentMode) {
            case CEHCK_POINT_MAPPING_DETAIL:
            case POI_MAPPING_DETAIL:
                return false;
            case POI_MAPPING:

                POIMapping.StopPOIMappingProcedure(mSailsMapView);
                findViewById(R.id.poi_mapping_toolbar).setVisibility(View.GONE);

                break;
            case BEACON_PATROLLING:
                BeaconPatrolling.StopBeaconPatrollingProcedure(mSailsMapView);
                findViewById(R.id.beacon_patrolling_toolbar).setVisibility(View.GONE);
                hideBeaconPatrollingView();
                break;
            case ACCURACY_VERIFICATION:
                AccVerify.StopAccVerifyProcedure(mSailsMapView);
                findViewById(R.id.acc_verify_toolbar).setVisibility(View.GONE);

                break;
            default:
                findViewById(R.id.poi_mapping_toolbar).setVisibility(View.GONE);

                break;

        }

        currentMode=mode;
        switch(mode) {
            case(POI_MAPPING):
                POIMapping.StartPOIMappingProcedure(MainActivity.this,projectObject
                        ,generalIconMap,highlightIconMap,checkIconMap,mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());

                mSails.loadAllRssFingerPrint(new SAILS.OnFinishCallback() {
                    @Override
                    public void onSuccess(String s) {
                        POIMapping.setRssfingerprintList(mSails.getFingerPrintLists());
                        POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                    }

                    @Override
                    public void onFailed(final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                GeneralUsage.ShowErrorDialog(MainActivity.this,s);

                            }
                        });
                    }
                });

                findViewById(R.id.poi_mapping_toolbar).setVisibility(View.VISIBLE);
                break;

            case BEACON_PATROLLING:
                showBeaconPatrollingView();
                findViewById(R.id.beacon_patrolling_toolbar).setVisibility(View.VISIBLE);

                break;
            case ACCURACY_VERIFICATION:
                ((TextView)findViewById(R.id.tvSample)).setText("0");
                ((TextView)findViewById(R.id.tvDeviation)).setText("-");
                ((TextView)findViewById(R.id.tvAccuracy)).setText("-");

                AccVerify.StartAccVerify(MainActivity.this,projectObject
                        ,generalIconMap,highlightIconMap,checkIconMap,mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                findViewById(R.id.acc_verify_toolbar).setVisibility(View.VISIBLE);
                break;

            default:
                findViewById(R.id.poi_mapping_toolbar).setVisibility(View.GONE);

                break;

        }
        return true;
    }

    private void hideBeaconPatrollingView() {
        fl_subwin.setVisibility(View.GONE);
        fl_subwin_fade.setVisibility(View.GONE);
        hideSubviewAnimate();

    }

    private void showBeaconPatrollingView() {
        fl_subwin.removeAllViews();
        beacon_patrolling_subview=(LinearLayout)getLayoutInflater().inflate(R.layout.beacon_patrolling_subview,null);
        fl_subwin.addView(beacon_patrolling_subview);
        showSubviewAnimate();
        beacon_patrolling_subview.post(new Runnable() {
            @Override
            public void run() {
                BeaconPatrolling.StartBeaconPatrollingProcedure(mSails,mSailsMapView,
                        mSails.getBLEBeaconList(),beaconStatusIconMap,
                        mSailsMapView.getCurrentBrowseFloorName(),
                        (TextView)beacon_patrolling_subview.findViewById(R.id.tv_beacons),
                        (TextView)beacon_patrolling_subview.findViewById(R.id.tv_low_bat));
                if(!BeaconPatrolling.IsAbleToUpload(projectObject)) {
                    beacon_patrolling_subview.findViewById(R.id.b_beacon_patrolling_upload).setVisibility(View.GONE);
                } else {
                    beacon_patrolling_subview.findViewById(R.id.b_beacon_patrolling_upload).setVisibility(View.VISIBLE);
                }
                beacon_patrolling_subview.findViewById(R.id.b_beacon_patrolling_upload).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BeaconPatrolling.UploadToCloud(MainActivity.this,projectObject,mSails.getBeaconPatrollingList());
                    }
                });
            }
        });
        startPositioningProcedure();

    }
    private void showSubviewAnimate() {
        fl_subwin.setVisibility(View.VISIBLE);
        fl_subwin_fade.setVisibility(View.VISIBLE);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            YoYo.with(Techniques.SlideInLeft).duration(500).playOn(fl_subwin);
            YoYo.with(Techniques.SlideInLeft).duration(500).playOn(fl_subwin_fade);

            //Do some stuff
        } else {
            YoYo.with(Techniques.SlideInUp).duration(500).playOn(fl_subwin);
            YoYo.with(Techniques.SlideInUp).duration(500).playOn(fl_subwin_fade);

        }
    }
    private void hideSubviewAnimate() {
        Techniques techniques;
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            techniques=Techniques.SlideOutLeft;
        } else {
            techniques=Techniques.SlideOutDown;

        }

        YoYo.with(techniques).duration(500).playOn(fl_subwin_fade);
        YoYo.with(techniques).duration(500).withListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                fl_subwin.setVisibility(View.GONE);
                fl_subwin_fade.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).playOn(fl_subwin);
    }

    HashMap<String,Drawable> generalIconMap,highlightIconMap,checkIconMap,beaconStatusIconMap;
    void iconDrawableProcedure() {
        generalIconMap = new HashMap<>();
        highlightIconMap = new HashMap<>();
        beaconStatusIconMap=new HashMap<>();
        checkIconMap=new HashMap<>();
        generalIconMap.put("default", Marker.boundCenter(getResources().getDrawable(R.drawable.poi_default_general)));
        highlightIconMap.put("default", Marker.boundCenter(getResources().getDrawable(R.drawable.poi_default_highlight)));
        checkIconMap.put("default", Marker.boundCenter(getResources().getDrawable(R.drawable.poi_check_green)));
        generalIconMap.put("check_point", Marker.boundCenter(getResources().getDrawable(R.drawable.poi_check_gray)));
        highlightIconMap.put("check_point", Marker.boundCenter(getResources().getDrawable(R.drawable.poi_check_red)));
        checkIconMap.put("check_point", Marker.boundCenter(getResources().getDrawable(R.drawable.poi_check_green)));
        beaconStatusIconMap.put("general", Marker.boundCenter(getResources().getDrawable(R.drawable.beacon_general)));
        beaconStatusIconMap.put("lowBat", Marker.boundCenter(getResources().getDrawable(R.drawable.beacon_lowbat)));
        beaconStatusIconMap.put("noScan", Marker.boundCenter(getResources().getDrawable(R.drawable.beacon_noscan)));
    }
    private void showCheckPointDetailView(final POI poi) {
        changeMode(CEHCK_POINT_MAPPING_DETAIL);
        final HashMap<String, Boolean> layers = POIMapping.getShowLayers();
        layers.put("check_point",true);
        POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
        ((ImageView)findViewById(R.id.iv_place_poi)).setImageDrawable(getResources().getDrawable(R.drawable.poi_check_red));
        poi.setIconType(POI.Type.HIGHLIGHT);
        findViewById(R.id.poi_mapping_toolbar).setVisibility(View.GONE);
        final double poi_lon,poi_lat;
        final String poi_floor;
        poi_lon=poi.getDouble("lon");
        poi_lat=poi.getDouble("lat");
        poi_floor=poi.getString("floor");
        fl_subwin.removeAllViews();
        check_point_detail_view=(LinearLayout)getLayoutInflater().inflate(R.layout.check_point_detail,null);
        final EditText et_check_point_comment=(EditText)check_point_detail_view.findViewById(R.id.et_check_point_comment);
        TextView tvCheckPointName=(TextView)check_point_detail_view.findViewById(R.id.tv_poi_check_id);

        if(poi.containsKey("comment"))
            et_check_point_comment.setText(poi.getString("comment"));
        String id="";
        if(poi.containsKey("id")) {
            id=poi.getString("id");
        } else {
            Random rnd = new Random();
            int n = 100000 + rnd.nextInt(900000);
            id=Integer.toString(n);
            poi.put("id",id);
        }
        final String checkPointId=id;
        tvCheckPointName.setText(checkPointId);


        fl_subwin.addView(check_point_detail_view);
        showSubviewAnimate();
        if(poi.getGeoPoint()!=null)
            mSailsMapView.setAnimationMoveMapTo(poi.getGeoPoint());

        check_point_detail_view.findViewById(R.id.b_poi_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                poi.setLocation(new GeoPoint(poi_lat,poi_lon),poi_floor);
                POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                if(poi.local) {
                    POIMapping.DeleteLocalPOI(poi);

                }
                closePOIDetailViewProcedure(poi);
            }
        });

        check_point_detail_view.findViewById(R.id.b_poi_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.dialog_poi_delete_title)
                        .positiveText(R.string.dialog_yes)
                        .negativeText(R.string.dialog_no)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                deletePOIProcedure(poi);
                            }
                        })
                        .show();
            }
        });
        check_point_detail_view.findViewById(R.id.b_poi_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RelativeLayout mask=(RelativeLayout)findViewById(R.id.place_poi_mask);
                if(mask.getVisibility()==View.VISIBLE) {
                    poi.setLocation(mSailsMapView.getMapViewPosition().getCenter(),mSailsMapView.getCurrentBrowseFloorName());
                }
                poi.put("comment",et_check_point_comment.getText().toString());
                poi.put("id",checkPointId);
                savePOIProcedure(poi);
            }
        });
        check_point_detail_view.findViewById(R.id.b_poi_move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMoveCheckPointProcedure(poi);
            }
        });
        if(poi.local) {//if this poi is new, go to set its location immediately.
            startMoveCheckPointProcedure(poi);
            check_point_detail_view.findViewById(R.id.b_poi_save).setVisibility(View.GONE);
            check_point_detail_view.findViewById(R.id.b_poi_delete).setVisibility(View.GONE);
        }

    }

    void showPOIDetailView(final POI poi) {
        changeMode(POI_MAPPING_DETAIL);
        final HashMap<String, Boolean> layers = POIMapping.getShowLayers();
        layers.put("poi",true);
        POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());

        ((ImageView)findViewById(R.id.iv_place_poi)).setImageDrawable(getResources().getDrawable(R.drawable.poi_default_highlight));
        poi.setHighlight(true);
        findViewById(R.id.poi_mapping_toolbar).setVisibility(View.GONE);
        final double poi_lon,poi_lat;
        final String poi_floor;
        poi_lon=poi.getDouble("lon");
        poi_lat=poi.getDouble("lat");
        poi_floor=poi.getString("floor");
        fl_subwin.removeAllViews();
        poi_detail_view=(LinearLayout)getLayoutInflater().inflate(R.layout.poi_detail,null);
        final EditText et_poi_name=(EditText)poi_detail_view.findViewById(R.id.et_poi_name);
        final EditText et_poi_link=(EditText)poi_detail_view.findViewById(R.id.et_poi_link);
        if(poi.containsKey("name"))
            et_poi_name.setText(poi.getString("name"));

        et_poi_name.setEnabled(true);
        poi_detail_view.findViewById(R.id.b_poi_delete).setEnabled(true);

        if(poi.containsKey("link"))
            et_poi_link.setText(poi.getString("link"));
        fl_subwin.addView(poi_detail_view);
        showSubviewAnimate();
        if(poi.getGeoPoint()!=null)
            mSailsMapView.setAnimationMoveMapTo(poi.getGeoPoint());

        poi_detail_view.findViewById(R.id.b_poi_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                poi.setLocation(new GeoPoint(poi_lat,poi_lon),poi_floor);
                POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                if(poi.local) {
                    POIMapping.DeleteLocalPOI(poi);

                }
                closePOIDetailViewProcedure(poi);
            }
        });
        poi_detail_view.findViewById(R.id.b_poi_qrcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

                    startActivityForResult(intent, QR_CODE_SCAN);
                } catch (Exception e) {
                    Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
                    startActivity(marketIntent);
                }
            }
        });
        poi_detail_view.findViewById(R.id.b_poi_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.dialog_poi_delete_title)
                        .positiveText(R.string.dialog_yes)
                        .negativeText(R.string.dialog_no)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                deletePOIProcedure(poi);
                            }
                        })
                        .show();
            }
        });
        poi_detail_view.findViewById(R.id.b_poi_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RelativeLayout mask=(RelativeLayout)findViewById(R.id.place_poi_mask);
                if(mask.getVisibility()==View.VISIBLE) {
                    poi.setLocation(mSailsMapView.getMapViewPosition().getCenter(),mSailsMapView.getCurrentBrowseFloorName());
                }
                poi.put("name",et_poi_name.getText().toString());
                poi.put("link",et_poi_link.getText().toString());
                savePOIProcedure(poi);
            }
        });
        poi_detail_view.findViewById(R.id.b_poi_move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             startMovePOIProcedure(poi);
            }
        });
        if(poi.local) //if this poi is new, go to set its location immediately.
            startMovePOIProcedure(poi);
    }
    private void startMoveCheckPointProcedure(POI poi) {
        poi.setLocation(new GeoPoint(0,0),"-100");
        mSailsMapView.redraw();
        RelativeLayout mask=(RelativeLayout)findViewById(R.id.place_poi_mask);
        if(mask.getVisibility()==View.VISIBLE) {
            if(poi.local) {
                poi.setLocation(new GeoPoint(mSailsMapView.getMapViewPosition().getCenter().latitude,
                        mSailsMapView.getMapViewPosition().getCenter().longitude),
                        mSailsMapView.getCurrentBrowseFloorName());
                poi.setIconType(POI.Type.GENERAL);
                final EditText et_check_point_comment=(EditText)check_point_detail_view.findViewById(R.id.et_check_point_comment);
                poi.put("comment",et_check_point_comment.getText().toString());

                saveNewCheckPointProcedure(poi);
                mask.setVisibility(View.GONE);
                return;
            }
            mask.setVisibility(View.GONE);
            ((Button)check_point_detail_view.findViewById(R.id.b_poi_move)).setText(R.string.poi_move);
            POIMapping.SetMapCenterAsPOILocation(mSailsMapView,poi);
        } else {
            mask.setVisibility(View.VISIBLE);
            ((Button)check_point_detail_view.findViewById(R.id.b_poi_move)).setText(R.string.poi_place);

        }
    }
    private void startMovePOIProcedure(POI poi) {
        poi.setLocation(new GeoPoint(0,0),"-100");
        mSailsMapView.redraw();
        RelativeLayout mask=(RelativeLayout)findViewById(R.id.place_poi_mask);
        if(mask.getVisibility()==View.VISIBLE) {
            mask.setVisibility(View.GONE);
            ((Button)poi_detail_view.findViewById(R.id.b_poi_move)).setText(R.string.poi_move);
            POIMapping.SetMapCenterAsPOILocation(mSailsMapView,poi);
        } else {
            mask.setVisibility(View.VISIBLE);
            ((Button)poi_detail_view.findViewById(R.id.b_poi_move)).setText(R.string.poi_place);

        }
    }

    private void deletePOIProcedure(final POI poi) {
        POIMapping.DeletePOI(this, projectObject,poi, new POIMapping.FinishCallback() {
            @Override
            public void onSuccess() {
                POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                closePOIDetailViewProcedure(poi);
            }

            @Override
            public void onFail(String err) {

            }
        });
    }

    private void savePOIProcedure(final POI poi) {
        POIMapping.SavePOI(this,projectObject, poi, new POIMapping.FinishCallback() {
            @Override
            public void onSuccess() {
                POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                closePOIDetailViewProcedure(poi);
            }

            @Override
            public void onFail(String err) {

            }
        });
    }
    private void saveNewCheckPointProcedure(final POI poi) {
        POIMapping.SaveCheckPoint(this,projectObject, poi, new POIMapping.FinishCallback() {
            @Override
            public void onSuccess() {
                POIMapping.ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());
                POI poi=POIMapping.AddNewCheckPoint(projectObject,mSailsMapView,generalIconMap,highlightIconMap,checkIconMap);
                showCheckPointDetailView(poi);
            }

            @Override
            public void onFail(String err) {

            }
        });
    }
    void closePOIDetailViewProcedure(POI poi) {
        poi.setHighlight(false);
        hideSubviewAnimate();
        findViewById(R.id.place_poi_mask).setVisibility(View.GONE);
        findViewById(R.id.poi_mapping_toolbar).setVisibility(View.VISIBLE);
        changeMode(POI_MAPPING);


    }
    private void checkFineLocationPermissionRoutine() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
//            afterFineLocationPermissionRoutine();
        }

    }

    private void afterFineLocationPermissionRoutine() {
       startPositioningProcedure();

    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
////            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(requestCode==MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
//            // If request is cancelled, the result arrays are empty.
//            if (grantResults.length > 0
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                afterFineLocationPermissionRoutine();
//
//            } else {
//
//            }
//            return;
//        }
//
//    }
}
