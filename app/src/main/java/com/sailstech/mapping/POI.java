package com.sailstech.mapping;

import android.graphics.drawable.Drawable;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.Marker;

/**
 * Created by richard on 2016/11/26.
 */
@ParseClassName("POI")
public class POI extends ParseObject {
    enum Type {
        GENERAL,
        HIGHLIGHT,
        CHECK
    }
    public boolean creatideaObject=false;
    public Marker marker=null;
    private Drawable general,highlight,check;
    public void setMarker(Drawable general,Drawable highlight,Drawable check) {
        this.general=general;
        this.highlight=highlight;
        this.check=check;
        setHighlight(isHighlight);

    }

    public void setMarker(Drawable general,Drawable highlight) {
        this.general=general;
        this.highlight=highlight;
        setHighlight(isHighlight);
    }
    private boolean isHighlight=false;
    public boolean local=false;
    public void setHighlight(boolean hl) {
        if(!containsKey("lat")||!containsKey("lon"))
            return;
        if(getDouble("lat")>90||getDouble("lat")<-90)
            return;
        Drawable icon;
        if(hl) {
            icon=highlight;
        } else {
            icon=general;
        }
        if(marker==null)
            marker = new Marker(new GeoPoint(getDouble("lat"),getDouble("lon")),icon);
        else
            marker.setDrawable(icon);
        isHighlight=hl;
    }
    public void setIconType(Type type) {
        Drawable icon=general;

        switch (type) {

            case GENERAL:
                icon=general;
                isHighlight=false;

                break;
            case HIGHLIGHT:
                icon=highlight;
                isHighlight=true;
                break;
            case CHECK:
                icon=check;
                isHighlight=false;
                break;
        }
        if(marker==null)
            marker = new Marker(new GeoPoint(getDouble("lat"),getDouble("lon")),icon);
        else
            marker.setDrawable(icon);
    }
    public GeoPoint getGeoPoint() {
        if(!containsKey("lat")||!containsKey("lon"))
            return null;
        return new GeoPoint(getDouble("lat"),getDouble("lon"));

    }
    public boolean isThisFloor(String floor) {
        if(containsKey("floor")) {
            if(getString("floor").equals(floor))
                return true;
        }
        return false;
    }

    public void setLocation(GeoPoint geoPoint, String floor) {
        put("lon",geoPoint.longitude);
        put("lat",geoPoint.latitude);
        put("floor",floor);
        if(marker!=null)
            marker.setGeoPoint(geoPoint);
    }
}
