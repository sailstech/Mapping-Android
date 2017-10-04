package com.sailstech.mapping;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.ParseObject;
import com.sails.engine.SAILS;
import com.sails.engine.SAILSMapView;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.ListOverlay;
import com.sails.engine.overlay.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by richard on 2016/11/28.
 */

public class BeaconPatrolling {


    static public class Beacon {
        double lon,lat;
        String floor;
        String mac_addr;
        int battery;//0-100;
        Marker marker=null;
        private Drawable general,lowBat,noScan;
        boolean isThisFloor(String floor) {
            return this.floor.equals(floor);
        }
        void setMarkerIcons(Drawable generalIcon,Drawable lowBatIcon,Drawable noScanIcon) {
            general=generalIcon;
            lowBat=lowBatIcon;
            noScan=noScanIcon;
        }
        void setGeneralMarker() {
            if(marker==null)
                marker = new Marker(new GeoPoint(lat,lon),general);
            else
                marker.setDrawable(general);
        }
        void setLowBatMarker() {
            if(marker==null)
                marker = new Marker(new GeoPoint(lat,lon),lowBat);
            else
                marker.setDrawable(lowBat);
        }
        void setNoScanMarker() {
            if(marker==null)
                marker = new Marker(new GeoPoint(lat,lon),noScan);
            else
                marker.setDrawable(noScan);
        }

        public String getSnackbarMsg() {
            if(battery!=-1) {
                return mac_addr+" Battery:"+Integer.toString(battery)+"%";
            }
            return mac_addr;

        }
    }
    static List<Beacon> beaconList=new ArrayList<>();
    static private ListOverlay beaconOverlay=new ListOverlay();
    static private HashMap<String,Beacon> beaconMap=new HashMap<>();
    public static Beacon getBeaconByMACLast4byte(String s) {
        for(Beacon beacon:beaconList) {
            String mac4=beacon.mac_addr.substring(12,14).toLowerCase()+
                    beacon.mac_addr.substring(15,17).toLowerCase();
            if(s.toLowerCase().equals(mac4))
                return beacon;
        }
        return null;
    }
    public static void ShowThisBeacon(final Beacon beacon,final SAILSMapView sailsMapView) {
        sailsMapView.loadFloorMap(beacon.floor);
        sailsMapView.setAnimationMoveMapTo(beacon.marker.getGeoPoint());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sailsMapView.setAnimationToZoom((byte)21);

            }
        },500);
    }
    static private void IconMapping(HashMap<String,Drawable> iconMap) {
        Drawable generalIcon=iconMap.get("general");
        Drawable lowBatIcon=iconMap.get("lowBat");
        Drawable noScanIcon=iconMap.get("noScan");
        for(Beacon b:beaconList) {
            b.setMarkerIcons(generalIcon,lowBatIcon,noScanIcon);
            b.setNoScanMarker();
        }

    }
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    static public void StartBeaconPatrollingProcedure(SAILS sails,
                                                      SAILSMapView sailsMapView,
                                                      final List<HashMap<String,Object>> beaconMapList,
                                                      HashMap<String,Drawable> beaconDrawableMap,
                                                      String floor,
                                                      final TextView tvBeacons,
                                                      TextView tvLowBatBeacons) {
        beaconList.clear();
        beaconMap.clear();
        for(HashMap<String,Object> map:beaconMapList) {
            Beacon b=new Beacon();
            b.lon=(double)map.get("lon");
            b.lat=(double)map.get("lat");
            b.floor=(String)map.get("floor");
            b.mac_addr=(String)map.get("mac");
            b.battery=-1;
            beaconMap.put(b.mac_addr,b);
            beaconList.add(b);
        }
        if(!sailsMapView.getOverlays().contains(beaconOverlay))
            sailsMapView.getOverlays().add(beaconOverlay);
        IconMapping(beaconDrawableMap);
        ShowFloorBeacons(sailsMapView,floor);
        sails.clearBeaconPatrollingList();
        if(runLoopHandler==null) {
            runLoopHandler=new RunLoopHandler();
            runLoopHandler.sails=sails;
            runLoopHandler.sailsMapview=sailsMapView;
        }
        runLoopHandler.tvBeacons=tvBeacons;
        runLoopHandler.tvLowBatBeacons=tvLowBatBeacons;
        loop=true;
        runLoopHandler.sleep(100);
        tvBeacons.setText(Integer.toString(beaconMapList.size()));



    }
    public static Beacon CheckBeaconIsTouched(int x, int y, SAILSMapView sailsMapView) {
        x -= sailsMapView.getWidth()/2;
        y -= sailsMapView.getHeight()/2;
        String floor=sailsMapView.getCurrentBrowseFloorName();

        for(Beacon b:beaconList) {
            if(floor.equals(b.floor)&&b.marker!=null) {
                if(b.marker.isInMarker(x,y))
                    return b;
            }
        }
        return null;
    }

    static public void StopBeaconPatrollingProcedure(final SAILSMapView sailsMapView) {

        if(sailsMapView.getOverlays().contains(beaconOverlay))
            sailsMapView.getOverlays().remove(beaconOverlay);
        sailsMapView.redraw();
        loop=false;
    }
    static public void ShowFloorBeacons(SAILSMapView sailsMapView,String floor) {
        synchronized (beaconList) {
            beaconOverlay.getOverlayItems().clear();
            for(Beacon b: beaconList) {
                if(b.isThisFloor(floor)&&b.marker!=null) {
                    beaconOverlay.getOverlayItems().add(b.marker);
                }
            }
            sailsMapView.redraw();
        }

    }
    static public boolean IsAbleToUpload(ParseObject project) {
        return project.containsKey("beacon_patrolling_url");
    }

    static public void UploadToCloud(final Activity activity,
                                     ParseObject project,
                                     List<HashMap<String,Object>> beaconList) {
        if(!IsAbleToUpload(project))
            return;
        final MaterialDialog md = GeneralUsage.ShowProgressing(activity,R.string.dialog_uploading);
        final String url=project.getString("beacon_patrolling_url");
        final JSONArray jsonArray=new JSONArray();
        for(Map<String,Object> map:beaconList) {
            JSONObject json=new JSONObject();
            if(map.get("battery")==null)
                continue;
            try {
                json.put("BeaconSerial",(String)map.get("mac"));
                json.put("PowerLevel",(int)map.get("battery"));
                json.put("UpdateTime",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ").format(new Date()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(json);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response=post(url,jsonArray.toString());
                    Log.d("url",jsonArray.toString());

                    md.dismiss();
                    if(response.isSuccessful()) {
                        if(activity!=null)
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new MaterialDialog.Builder(activity)
                                        .content(R.string.dialog_upload_successfully)
                                        .positiveText(R.string.dialog_ok)
                                        .show();
                            }
                        });
                        return;
                    }

                } catch (IOException e) {
                    md.dismiss();
                    e.printStackTrace();
                }
                if(activity!=null)
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GeneralUsage.ShowErrorDialog(activity);
                        }
                    });

            }
        }).start();

    }
    static OkHttpClient client = new OkHttpClient();
    static Response post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }
    private static boolean loop=false;
    private static long loopTime=1 * 200;
    private static RunLoopHandler runLoopHandler;
    private static class RunLoopHandler extends Handler {
        public SAILS sails;
        public SAILSMapView sailsMapview;
        boolean working=false;
        TextView tvBeacons,tvLowBatBeacons;
        @Override
        public void handleMessage(Message msg) {
            if(!loop)
                return;
            if(working)
                return;
            working=true;
            List<HashMap<String, Object>> maplist = sails.getBeaconPatrollingList();
            int beaconCount=0,lowBeaconCount=0;
            for(HashMap<String,Object> beacon:maplist) {
                Beacon b=beaconMap.get(beacon.get("mac"));
                if(b!=null) {
                    b.setGeneralMarker();
                    if(beacon.get("battery")!=null) {
                        b.battery = (int) beacon.get("battery");
                        if(b.battery<20) {
                            b.setLowBatMarker();
                            lowBeaconCount++;
                        }
                        beaconCount++;

                    }
                }
            }
            if(beaconCount>0) {
                tvBeacons.setText(Integer.toString(beaconCount)+"/"+Integer.toString(beaconMap.size()));
                tvLowBatBeacons.setText(Integer.toString(lowBeaconCount));

            }
            sailsMapview.redraw();
            working=false;
            sleep(loopTime);

        }
        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }
}
