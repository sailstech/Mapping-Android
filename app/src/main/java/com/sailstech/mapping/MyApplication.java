package com.sailstech.mapping;

import android.app.Application;

import com.bilibili.boxing.BoxingCrop;
import com.bilibili.boxing.BoxingMediaLoader;
import com.bilibili.boxing.loader.IBoxingMediaLoader;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseObject;
import com.sailstech.mapping.impl.BoxingFrescoLoader;
import com.sailstech.mapping.impl.BoxingUcrop;
import com.sailstech.mapping.parse.NavigationPhotoTest;

/**
 * Created by richard on 2016/11/25.
 */

public class MyApplication extends Application {
    public static MixpanelAPI Mixpanel;
    @Override
    public void onCreate() {
        super.onCreate();
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(getBaseContext())
                .setDownsampleEnabled(true)
                .build();
//        Fresco.initialize(this,config);
        Parse.initialize(new Parse.Configuration.Builder(getBaseContext())
                .applicationId(CloudRef.PARSE_APPLICATION_ID)
                .server(CloudRef.PARSE_SERVER_URL)
                .enableLocalDataStore()
                .build());
        ParseObject.registerSubclass(POI.class);
        ParseObject.registerSubclass(NavigationPhotoTest.class);
        Mixpanel=MixpanelAPI.getInstance(this,CloudRef.MIXPANEL_ID);
        initBoxing();
    }

    private void initBoxing() {
        IBoxingMediaLoader loader = new BoxingFrescoLoader(this);
        BoxingMediaLoader.getInstance().init(loader);
        BoxingCrop.getInstance().init(new BoxingUcrop());
    }
}
