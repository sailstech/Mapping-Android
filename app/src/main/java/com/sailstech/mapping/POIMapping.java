package com.sailstech.mapping;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.Time;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.sails.engine.SAILSMapView;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.ListOverlay;
import com.sails.engine.overlay.SingleLine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by richard on 2016/11/26.
 */

public class POIMapping {
    static List<POI> poiList= Collections.synchronizedList(new ArrayList<POI>());
    static private ListOverlay poiOverlay=new ListOverlay();
    static private HashMap<String,Boolean> showLayers=new HashMap<>();
    static private List<HashMap<String,Object>> rssfingerprintList=new ArrayList<>();
    private static Paint rssFingerprintStroke;

    public static POI CheckPOIIsTouched(int x, int y,SAILSMapView sailsMapView) {
        x -= sailsMapView.getWidth()/2;
        y -= sailsMapView.getHeight()/2;
        String floor=sailsMapView.getCurrentBrowseFloorName();

        for(POI poi:poiList) {
            if(floor.equals(poi.getString("floor"))&&poi.marker!=null) {
                if(poi.marker.isInMarker(x,y))
                    return poi;
            }
        }
        return null;
    }
    public static HashMap<String,Boolean> getShowLayers() {
        return showLayers;
    }
    public static void setRssfingerprintList(List<HashMap<String,Object>> list) {
        showLayers.put("rss_fingerprint",true);
        rssfingerprintList=list;
        rssFingerprintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        rssFingerprintStroke.setStyle(Paint.Style.FILL);
        rssFingerprintStroke.setColor(Color.rgb(255, 0, 0));//);
        rssFingerprintStroke.setAlpha(100);
        rssFingerprintStroke.setStrokeWidth(10);
        rssFingerprintStroke.setStrokeJoin(Paint.Join.ROUND);

    }
    public static void DeletePOI(final Context context, final ParseObject project, final POI poi, final FinishCallback callback) {
        final MaterialDialog md= GeneralUsage.ShowProgressing(context,R.string.dialog_poi_deleting);
        poi.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                md.dismiss();
                if(e==null) {
                    poiList.remove(poi);
                    if(project.containsKey("poi_update_get_url")&&project.containsKey("creatidea_museum_id")
                            &&poi.containsKey("link")) {
                        final MaterialDialog md= GeneralUsage.ShowProgressing(context,R.string.dialog_poi_deleting);
                        SavePOIToCreatideaCloud(context,project, poi,true, new FinishCallback() {
                            @Override
                            public void onSuccess() {
                                md.dismiss();
                                callback.onSuccess();
                            }

                            @Override
                            public void onFail(String err) {
                                md.dismiss();
                                GeneralUsage.ShowErrorDialog(context,context.getString(R.string.upload_to_creatidea_cloud_error));
                                callback.onFail(err);
                            }
                        });

                    } else {
                        callback.onSuccess();
                    }
                } else {
                    GeneralUsage.ShowErrorDialog(context,e.getMessage());
                    callback.onFail(e.getMessage());

                }
            }
        });

    }

    public static void SavePOI(final Context context, final ParseObject project, final POI poi, final FinishCallback callback) {
        final MaterialDialog md= GeneralUsage.ShowProgressing(context,R.string.dialog_poi_saving);
        if(poi.creatideaObject) {
            if(project.containsKey("poi_update_get_url")&&project.containsKey("creatidea_museum_id")
                    &&poi.containsKey("link")) {
                SavePOIToCreatideaCloud(context,project, poi,false, new FinishCallback() {
                    @Override
                    public void onSuccess() {
                        md.dismiss();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFail(String err) {
                        md.dismiss();
                        GeneralUsage.ShowErrorDialog(context,context.getString(R.string.upload_to_creatidea_cloud_error));
                        callback.onFail(err);
                    }
                });

            } else {
                callback.onFail(context.getString(R.string.upload_to_creatidea_cloud_error));
            }

        } else {
            poi.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    poi.local=false;
                    md.dismiss();
                    if(e==null) {
                        if(!poiList.contains(poi))
                            poiList.add(poi);
                        callback.onSuccess();
                    } else {
                        GeneralUsage.ShowErrorDialog(context,e.getMessage());
                        callback.onFail(e.getMessage());

                    }
                }
            });
        }

    }
    public static void SaveCheckPoint(final Context context, ParseObject projectObject, final POI poi, final FinishCallback callback) {

        final MaterialDialog md= GeneralUsage.ShowProgressing(context,R.string.dialog_poi_saving);
        poi.put("layer","check_point");
        poi.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                poi.local=false;
                md.dismiss();
                if(e==null) {
                    if(!poiList.contains(poi))
                        poiList.add(poi);
                    callback.onSuccess();
                } else {
                    GeneralUsage.ShowErrorDialog(context,e.getMessage());
                    callback.onFail(e.getMessage());

                }
            }
        });

    }

    public static void SavePOIToCreatideaCloud(final Context context,ParseObject project,final POI poi,boolean delete, final FinishCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url=project.getString("poi_update_get_url");
        if(url==null) {
            callback.onSuccess();
            return;
        }
        url+="?museumId="+project.getString("creatidea_museum_id");
        url+="&id="+poi.get("link");
        if(delete)
            url+="&x=0";
        else
            url+="&x="+poi.get("lon");
        if(delete)
            url+="&y=0";
        else
            url+="&y="+poi.get("lat");
        url+="&floor="+poi.get("floor");
        RequestBody formBody = new FormBody.Builder().build();

        Request request = new Request.Builder()
                .url(url).post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Handler mainHandler = new Handler(context.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        callback.onFail(e.getMessage());
                    } // This is your code
                };
                mainHandler.post(myRunnable);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                Handler mainHandler = new Handler(context.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if(response.isSuccessful())
                            callback.onSuccess();
                        else
                            callback.onFail(response.body().toString());
                    } // This is your code
                };
                mainHandler.post(myRunnable);

            }
        });

    }
    public static void SetMapCenterAsPOILocation(final SAILSMapView mSailsMapView, POI poi) {
        GeoPoint geoPoint=mSailsMapView.getMapViewPosition().getCenter();
        poi.setLocation(geoPoint,mSailsMapView.getCurrentBrowseFloorName());
        ShowFloorPOIs(mSailsMapView,mSailsMapView.getCurrentBrowseFloorName());

    }

    public static POI AddNewPOI(ParseObject project,SAILSMapView sailsMapView, HashMap<String, Drawable> generalIconMap, HashMap<String, Drawable> highlightIconMap) {
        POI poi=new POI();
        ParseACL acl=new ParseACL();
        acl.setRoleWriteAccess("Administrator",true);
        acl.setRoleReadAccess("Administrator",true);
        acl.setRoleWriteAccess(project.getObjectId(),true);
        acl.setPublicReadAccess(true);
        poi.put("project",project);
        poi.put("lon",sailsMapView.getMapViewPosition().getCenter().longitude);
        poi.put("lat",sailsMapView.getMapViewPosition().getCenter().latitude);
        poi.put("floor",sailsMapView.getCurrentBrowseFloorName());
        poi.setACL(acl);
        poi.local=true;
        poi.put("type","poi");
        poi.put("layer","poi");

        IconMapping(poi,generalIconMap,highlightIconMap);
        poiList.add(poi);
        ShowFloorPOIs(sailsMapView,sailsMapView.getCurrentBrowseFloorName());
        return poi;
    }
    public static POI AddNewCheckPoint(ParseObject project,
                                       SAILSMapView sailsMapView,
                                       HashMap<String, Drawable> generalIconMap,
                                       HashMap<String, Drawable> highlightIconMap,
                                       HashMap<String, Drawable> unselectIconMap) {
        POI poi=new POI();
        ParseACL acl=new ParseACL();
        acl.setRoleWriteAccess("Administrator",true);
        acl.setRoleReadAccess("Administrator",true);
        acl.setRoleWriteAccess(project.getObjectId(),true);
        acl.setRoleReadAccess(project.getObjectId(),true);
        poi.put("project",project);
        poi.put("lon",sailsMapView.getMapViewPosition().getCenter().longitude);
        poi.put("lat",sailsMapView.getMapViewPosition().getCenter().latitude);
        poi.put("floor",sailsMapView.getCurrentBrowseFloorName());
        poi.setACL(acl);
        poi.local=true;
        poi.put("type","check_point");
        poi.put("layer","check_point");

        IconMapping(poi,generalIconMap,highlightIconMap,unselectIconMap);

        poiList.add(poi);
        ShowFloorPOIs(sailsMapView,sailsMapView.getCurrentBrowseFloorName());
        return poi;
    }
    public static void DeleteLocalPOI(POI poi) {
        if(poiList.contains(poi)) {
            poiList.remove(poi);
        }
        if(poiOverlay.getOverlayItems().contains(poi.marker))
            poiOverlay.getOverlayItems().remove(poi.marker);
    }

    public static POI getCheckPointById(String s) {
        POI poi=null;
        for(POI p:poiList) {
            if(!p.containsKey("type"))
                continue;
            if(p.getString("type").equals("check_point")&&s.equals(p.getString("id"))) {
                poi = p;
                break;
            }
        }
        return poi;
    }


    public interface FinishCallback {
        void onSuccess();
        void onFail(String err);
    }
    static public void StartPOIMappingProcedure(final Context context,
                                                ParseObject project,
                                                final HashMap<String,Drawable> general,
                                                final HashMap<String,Drawable> highlight,
                                                final HashMap<String,Drawable> check,
                                                final SAILSMapView sailsMapView,
                                                final String floor) {
        if(!sailsMapView.getOverlays().contains(poiOverlay))
            sailsMapView.getOverlays().add(poiOverlay);
        showLayers.put("rss_fingerprint",false);
        showLayers.put("check_point",true);
        showLayers.put("poi",true);
        final MaterialDialog md= GeneralUsage.ShowProgressing(context,R.string.dialog_load_pois);
        LoadPOIsInCloud(context,project, new FinishCallback() {
            @Override
            public void onSuccess() {
                md.dismiss();
                IconMapping(general,highlight,check);
                ShowFloorPOIs(sailsMapView,floor);
            }

            @Override
            public void onFail(String err) {
                md.dismiss();
                GeneralUsage.ShowErrorDialog(context,err);
            }
        });

    }
    static public void StopPOIMappingProcedure(final SAILSMapView sailsMapView) {

        if(sailsMapView.getOverlays().contains(poiOverlay))
            sailsMapView.getOverlays().remove(poiOverlay);
        sailsMapView.redraw();
    }
    static void LoadPOIsInCloud(final Context context,final ParseObject project, final FinishCallback callback) {
        poiList.clear();
        ParseQuery<POI> query = new ParseQuery<>("POI");
        ParseQuery<ParseObject> queryProject = new ParseQuery<>("MappingProject");
        queryProject.whereEqualTo("objectId",project.getObjectId());
        query.whereMatchesQuery("project",queryProject);
        query.setLimit(5000);
        query.findInBackground(new FindCallback<POI>() {
            @Override
            public void done(List<POI> objects, ParseException e) {
                if(e==null) {
                    if(objects!=null) {
                        poiList=objects;
                    }
                    callback.onSuccess();
                    return;
                }
                callback.onFail(e.getMessage());
            }
        });
    }

    private static void LoadPOIsInCreatideaCloud(final Context context,ParseObject project, final FinishCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .build();
        String url="https://ciculture.azurewebsites.net/WebApi/api/Item/ListCoordinate";
        url+="?museumId="+project.getString("creatidea_museum_id");
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Handler mainHandler = new Handler(context.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        callback.onFail(e.getMessage());
                    } // This is your code
                };
                mainHandler.post(myRunnable);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonData = response.body().string();
                JSONObject object = null;
                try {
                    object = new JSONObject(jsonData);
                    JSONArray jsonArray = object.getJSONArray("Data");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        if(obj.isNull("Id"))
                            continue;
                        if(obj.isNull("X"))
                            continue;
                        if(obj.isNull("Y"))
                            continue;
                        if(obj.isNull("Floor"))
                            continue;
                        POI poi=new POI();
                        poi.put("link",obj.getString("Id"));
                        poi.put("lon",obj.getDouble("X"));
                        poi.put("lat",obj.getDouble("Y"));
                        poi.put("floor",obj.getString("Floor"));
                        if(!obj.isNull("Name"))
                            poi.put("name",obj.getString("Name"));
                        poi.put("type","poi");
                        poi.put("layer","poi");
                        poi.creatideaObject=true;
                        poiList.add(poi);

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(context.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess();
                    } // This is your code
                };
                mainHandler.post(myRunnable);

            }
        });
    }

    static private void IconMapping(HashMap<String,Drawable> general,
                                    HashMap<String,Drawable> highlight,
                                    HashMap<String,Drawable> check) {
        Drawable generalDefaultIcon=general.get("default");
        Drawable highlightDefaultIcon=highlight.get("default");
        Drawable checkDefaultIcon=check.get("default");
        for(POI poi:poiList) {
            Drawable generalIcon,highlightIcon,checkIcon;
            generalIcon=generalDefaultIcon;
            highlightIcon=highlightDefaultIcon;
            checkIcon=checkDefaultIcon;

            if(poi.containsKey("type")) {
                if(general.get(poi.getString("type"))!=null) {
                    generalIcon=general.get(poi.getString("type"));
                }
                if(highlight.get(poi.getString("type"))!=null) {
                    highlightIcon=highlight.get(poi.getString("type"));
                }
                if(highlight.get(poi.getString("type"))!=null) {
                    checkIcon=check.get(poi.getString("type"));
                }
            }
            poi.setMarker(generalIcon,highlightIcon,checkIcon);
        }
    }
    static private void IconMapping(POI poi,
                                    HashMap<String,Drawable> general,
                                    HashMap<String,Drawable> highlight,
                                    HashMap<String,Drawable> check) {
        Drawable generalDefaultIcon=general.get("default");
        Drawable highlightDefaultIcon=highlight.get("default");
        Drawable unselectDefaultIcon=check.get("default");
        Drawable generalIcon,highlightIcon,unselectIcon;
        generalIcon=generalDefaultIcon;
        highlightIcon=highlightDefaultIcon;
        unselectIcon=unselectDefaultIcon;

        if(poi.containsKey("type")) {
            if(general.get(poi.getString("type"))!=null) {
                generalIcon=general.get(poi.getString("type"));
            }
            if(highlight.get(poi.getString("type"))!=null) {
                highlightIcon=highlight.get(poi.getString("type"));
            }
            if(highlight.get(poi.getString("type"))!=null) {
                unselectIcon=check.get(poi.getString("type"));
            }
        }
        poi.setMarker(generalIcon,highlightIcon,unselectIcon);
    }

    static private void IconMapping(POI poi,HashMap<String,Drawable> general, HashMap<String,Drawable> highlight) {
        IconMapping(poi,general,highlight,general);
//        Drawable generalDefaultIcon=general.get("default");
//        Drawable highlightDefaultIcon=highlight.get("default");
//        Drawable generalIcon,highlightIcon;
//        generalIcon=generalDefaultIcon;
//        highlightIcon=highlightDefaultIcon;
//
//        if(poi.containsKey("type")) {
//            if(general.get(poi.getString("type"))!=null) {
//                generalIcon=general.get(poi.getString("type"));
//            }
//            if(highlight.get(poi.getString("type"))!=null) {
//                highlightIcon=highlight.get(poi.getString("type"));
//            }
//        }
//        poi.setMarker(generalIcon,highlightIcon);
    }
    static private void RemoveSAILSMapViewPOILayers(SAILSMapView sailsMapView) {
        if(!sailsMapView.getOverlays().contains(poiOverlay))
            return;
        sailsMapView.getOverlays().remove(poiOverlay);
    }
    static public void ShowFloorPOIs(SAILSMapView sailsMapView,String floor) {
        poiOverlay.getOverlayItems().clear();
        if(showLayers.get("rss_fingerprint")) {
            for(HashMap<String,Object> rss:rssfingerprintList) {
                if(rss.get("floor")!=null&&floor.equals((String)rss.get("floor"))) {
                    GeoPoint start=new GeoPoint((Double)rss.get("start_lat"),(Double)rss.get("start_lon"));
                    GeoPoint end=new GeoPoint((Double)rss.get("end_lat"),(Double)rss.get("end_lon"));
                    SingleLine line=new SingleLine(start,end,rssFingerprintStroke);
                    poiOverlay.getOverlayItems().add(line);
                }
            }
        }
        for(POI poi: poiList) {
            if(!poi.containsKey("layer"))
                continue;
            if(showLayers.get(poi.getString("layer"))!=null&&showLayers.get(poi.getString("layer"))) {
                if(poi.isThisFloor(floor)&&poi.marker!=null) {
                    poiOverlay.getOverlayItems().add(poi.marker);
                }
            }
        }
        sailsMapView.redraw();

    }

}
