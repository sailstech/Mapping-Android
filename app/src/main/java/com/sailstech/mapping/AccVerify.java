package com.sailstech.mapping;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.sails.engine.SAILS;
import com.sails.engine.SAILSMapView;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.ListOverlay;
import com.sails.engine.overlay.SingleLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by richard on 2016/12/4.
 */

public class AccVerify {

    public static class CheckPoint {
        POI verifyCheckPoint;
        GeoPoint target,measure;
        Date timestamp;
        double error;

    }
    static List<POI> poiList =new ArrayList<>();
    static List<CheckPoint> checkPointList =new ArrayList<>();
    static CheckPoint currentCheckPoint;
    static private ListOverlay checkPointsOverlay =new ListOverlay();
    static private ListOverlay currentErrorOverlay =new ListOverlay();
    public static String Accuracy,Deviation,Samples;
    public static void ClearMeasurementResult(SAILSMapView sailsMapView) {
        checkPointList.clear();
        for(POI poi:poiList) {
            poi.setIconType(POI.Type.GENERAL);
        }
        CalculateAccuracy();
        ShowFloorPOIs(sailsMapView,sailsMapView.getCurrentBrowseFloorName());
    }
    public static void Sample(SAILSMapView sailsMapView) {
        if(currentCheckPoint==null)
            return;
        currentCheckPoint.verifyCheckPoint.setIconType(POI.Type.CHECK);
        CheckPoint cpSaved=null;
        for(CheckPoint cp:checkPointList) {
            if(cp.verifyCheckPoint==currentCheckPoint.verifyCheckPoint) {
                cpSaved=cp;
                break;
            }
        }
        if(cpSaved!=null) {
            cpSaved.timestamp=currentCheckPoint.timestamp;
            cpSaved.error=currentCheckPoint.error;
            cpSaved.target=currentCheckPoint.target;
            cpSaved.measure=currentCheckPoint.measure;

        } else {
            checkPointList.add(currentCheckPoint);
        }
        CalculateAccuracy();
        ShowFloorPOIs(sailsMapView,sailsMapView.getCurrentBrowseFloorName());
    }

    private static void CalculateAccuracy() {
        Samples=Integer.toString(checkPointList.size());
        double error=0;
        for(CheckPoint cp:checkPointList) {
            error+=cp.error;
        }
        error/=checkPointList.size();
        Accuracy=getFormatedMeter(error);
        double deviation=0;
        for(CheckPoint cp:checkPointList) {
            deviation+=(cp.error-error)*(cp.error-error);
        }
        deviation/=checkPointList.size();
        deviation=Math.pow(deviation,0.5);
        Deviation=getFormatedMeter(deviation);
    }

    public static void StopAccVerifyProcedure(SAILSMapView sailsMapView) {
        if(sailsMapView.getOverlays().contains(checkPointsOverlay))
            sailsMapView.getOverlays().remove(checkPointsOverlay);
        if(sailsMapView.getDynamicOverlays().contains(currentErrorOverlay))
            sailsMapView.getDynamicOverlays().remove(currentErrorOverlay);
        sailsMapView.redraw();

    }

    public static String CheckCheckPointIsTouched(int x, int y, SAILS sails, SAILSMapView sailsMapView) {
        x -= sailsMapView.getWidth()/2;
        y -= sailsMapView.getHeight()/2;
        String floor=sailsMapView.getCurrentBrowseFloorName();
        currentCheckPoint=null;
        for(POI cp: poiList) {
            if(floor.equals(cp.getString("floor"))&&cp.marker!=null) {
                if(cp.marker.isInMarker(x,y)) {
                    currentCheckPoint=new CheckPoint();
                    currentCheckPoint.verifyCheckPoint=cp;
                    break;
                }
            }
        }
        if(currentCheckPoint==null) {
            ClearCurrentCheckPoint(sailsMapView);
            return "";

        }
        if(!sails.isLocationFix()&&!sails.isUseGPS())
            return "";
        if(!sails.getFloor().equals(currentCheckPoint.verifyCheckPoint.get("floor")))
            return "";

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.FILL);
        stroke.setColor(Color.rgb(0, 255, 128));//);
        stroke.setAlpha(100);
        stroke.setStrokeWidth(10);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        SingleLine singleLine=new SingleLine(currentCheckPoint.verifyCheckPoint.getGeoPoint(),new GeoPoint(sails.getLatitude(),sails.getLongitude()),stroke);
        currentErrorOverlay.getOverlayItems().clear();
        currentErrorOverlay.getOverlayItems().add(singleLine);
        sailsMapView.invalidate();
        double distance=sails.getMapDistanceByLngLat(currentCheckPoint.verifyCheckPoint.getGeoPoint().longitude,
                currentCheckPoint.verifyCheckPoint.getGeoPoint().latitude,
                sails.getLongitude(),
                sails.getLatitude());
        currentCheckPoint.target=currentCheckPoint.verifyCheckPoint.getGeoPoint();
        currentCheckPoint.measure=new GeoPoint(sails.getLatitude(),sails.getLongitude());
        currentCheckPoint.timestamp=new Date();
        currentCheckPoint.error=distance;
        return getFormatedMeter(distance);

    }

    private static void ClearCurrentCheckPoint(SAILSMapView sailsMapView) {
        currentCheckPoint=null;
        currentErrorOverlay.getOverlayItems().clear();
        sailsMapView.redraw();

    }

    static String getFormatedMeter(double meter) {
        int d=(int)(meter*10.0);
        return Double.toString(d/10.0);
    }
    public interface FinishCallback {
        void onSuccess();
        void onFail(String err);
    }
    static void StartAccVerify(final Context context,
                               ParseObject project,
                               final HashMap<String,Drawable> general,
                               final HashMap<String,Drawable> highlight,
                               final HashMap<String,Drawable> check,
                               final SAILSMapView sailsMapView,
                               final String floor) {
        currentErrorOverlay.getOverlayItems().clear();
        checkPointList.clear();

        if(!sailsMapView.getOverlays().contains(checkPointsOverlay))
            sailsMapView.getOverlays().add(checkPointsOverlay);
        if(!sailsMapView.getDynamicOverlays().contains(currentErrorOverlay))
            sailsMapView.getDynamicOverlays().add(currentErrorOverlay);
        final MaterialDialog md= GeneralUsage.ShowProgressing(context,R.string.dialog_load_check_points);
        LoadCheckPointsInCloud(project, new FinishCallback() {
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
    static private void IconMapping(HashMap<String,Drawable> general,
                                    HashMap<String,Drawable> highlight,
                                    HashMap<String,Drawable> check) {
        Drawable generalDefaultIcon=general.get("default");
        Drawable highlightDefaultIcon=highlight.get("default");
        Drawable checkDefaultIcon=check.get("default");
        for(POI poi: poiList) {
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
    static void LoadCheckPointsInCloud(ParseObject project, final FinishCallback callback) {
        poiList.clear();
        ParseQuery<POI> query = new ParseQuery<>("POI");
        ParseQuery<ParseObject> queryProject = new ParseQuery<>("MappingProject");
        queryProject.whereEqualTo("objectId",project.getObjectId());
        query.whereMatchesQuery("project",queryProject);
        query.whereEqualTo("type","check_point");
        query.setLimit(5000);
        query.findInBackground(new FindCallback<POI>() {
            @Override
            public void done(List<POI> objects, ParseException e) {
                if(e==null) {
                    if(objects!=null) {
                        poiList =objects;
                    }
                    callback.onSuccess();
                    return;
                }
                callback.onFail(e.getMessage());
            }
        });
    }

    static public void ShowFloorPOIs(SAILSMapView sailsMapView,String floor) {
        checkPointsOverlay.getOverlayItems().clear();

        for(POI poi: poiList) {
            if(poi.isThisFloor(floor)&&poi.marker!=null) {
                checkPointsOverlay.getOverlayItems().add(poi.marker);
            }
        }
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.FILL);
        stroke.setColor(Color.rgb(255, 0, 0));//);
        stroke.setAlpha(100);
        stroke.setStrokeWidth(5);
        stroke.setStrokeJoin(Paint.Join.ROUND);

        for(CheckPoint cp:checkPointList) {
            POI poi= cp.verifyCheckPoint;
            if(poi.isThisFloor(floor)&&poi.marker!=null) {
                SingleLine singleLine=new SingleLine(cp.measure,cp.target,stroke);
                checkPointsOverlay.getOverlayItems().add(singleLine);
            }


        }
        sailsMapView.redraw();
    }
    static public File SaveToCSVFile(Context context, ParseObject project, String fileName) {
        String output="Name"+","+fileName+"\r\n";;
        output+="Samples"+","+Samples+"\r\n";
        output+="Std. Deviation"+","+Deviation+", m \r\n";
        output+="Avg. Accuracy"+","+Accuracy+", m \r\n";
        output+="Check Point ID,Mapping ID,Floor,Check Point Latitude,Check Point Longitude,Measurement Latitude,Measurement Longitude,Error(m)\r\n";
        for(CheckPoint cp:checkPointList) {
            output+=cp.verifyCheckPoint.getString("id")+",";
            output+=cp.verifyCheckPoint.getString("comment")+",";
            output+=cp.verifyCheckPoint.getString("floor")+",";
            output+=Double.toString(cp.target.latitude)+",";
            output+=Double.toString(cp.target.longitude)+",";
            output+=Double.toString(cp.measure.latitude)+",";
            output+=Double.toString(cp.measure.longitude)+",";
            output+=Double.toString(cp.error)+"\r\n";
        }
        File file = new File(context.getExternalCacheDir(), fileName+".csv");
        Writer outputStream;

        try {
            outputStream = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF-8"));

            outputStream.write(output);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
}
