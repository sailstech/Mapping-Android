package com.sailstech.mapping.parse;


import com.parse.ParseClassName;
import com.parse.ParseObject;
/**
 * Created by liberty on 2017/5/2.
 */

@ParseClassName(ParseClass.Photo)
public class NavigationPhotoTest extends ParseObject {

    public static final String URL = "url";
    public static final String BUILDINGID = "buildingId";
    public static final String SELFID = "selfId";
    public static final String COMMENT = "comment";
    public String getUrl(){
        return getString(URL);
    }
    public String getBuildingId() {
        return getString(BUILDINGID);
    }
    public int getSelfId(){
        return getInt(SELFID);
    }
    public String getComment(){
        return getString(COMMENT);
    }
}
