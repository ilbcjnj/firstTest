package com.hesine.nmsg.thirdparty;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.hesine.nmsg.business.bean.Coord;
import com.hesine.nmsg.business.bean.LocationInfo;
import com.hesine.nmsg.business.bo.PostPNToken;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.common.MLog;

public class Location {

    public static Location mInstance = null;
    public LocationClient mLocationClient = null;
    public MyLocationListener mMyLocationListener = null;
    public static final String TAG = "Location";
    public static int retryMaxNum = 3;

    public static Location getInstance() {
        if (null == mInstance) {
            mInstance = new Location();
        }
        return mInstance;
    }

    public void registerLocationClient(Context context) {
        mLocationClient = new LocationClient(context);
        mMyLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mMyLocationListener);
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // Receive Location
            retryMaxNum--;

            if (retryMaxNum <= 0) {
                mLocationClient.stop();
            }

            int locType = location.getLocType();
            LocationInfo loc = new LocationInfo();
            MLog.error(TAG, "onReceiveLocation loc Type: " + locType);
            MLog.info(TAG, "onReceiveLocation Latitude:" + location.getLatitude() + " Longitude: "
                    + location.getLongitude() + " Province: " + location.getProvince() + " city: "
                    + location.getCity());

            if (locType == BDLocation.TypeGpsLocation) {
                if (mLocationClient.isStarted()) {
                    mLocationClient.stop();
                }
                Coord coord = new Coord();
                coord.setLatitude(String.valueOf(location.getLatitude()));
                coord.setLongitude(String.valueOf(location.getLongitude()));
                loc.setCoord(coord);
                loc.setType(LocationInfo.LOCATION_GPS);
                GlobalData.instance().getSystemInfo().setLocation(loc);
                postLocation();
            } else if (locType == BDLocation.TypeNetWorkLocation) {
                if (mLocationClient.isStarted()) {
                    mLocationClient.stop();
                }
                Coord coord = new Coord();
                coord.setLatitude(String.valueOf(location.getLatitude()));
                coord.setLongitude(String.valueOf(location.getLongitude()));
                loc.setCoord(coord);
                loc.setProvince(location.getProvince());
                loc.setCity(location.getCity());
                loc.setDistrict(location.getDistrict());
                loc.setType(LocationInfo.LOCATION_NETWORK);
                GlobalData.instance().getSystemInfo().setLocation(loc);
                postLocation();
            }
            Config.saveLocation(JSON.toJSONString(loc));
        }
    }

    public synchronized void requestLocation() {
        if (null == mLocationClient) {
            MLog.error(TAG, "mLocationClient is null");
            return;
        }
        if (null != mLocationClient && mLocationClient.isStarted()) {
            mLocationClient.stop();
        }

        MLog.info(TAG, "requestLocation");
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Battery_Saving);
        option.setCoorType("bd09ll");
        option.setProdName("com.hesine.nmsg");
        option.setIsNeedAddress(true);
        option.setScanSpan(2000);
        retryMaxNum = 3;

        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    public void postLocation() {
        PostPNToken postPNToken = new PostPNToken();
        postPNToken.start();
    }

    public void checkNeedRequestLocation() {
        LocationInfo loc = JSON.parseObject(Config.getLocation(), LocationInfo.class);
        if (null == loc || loc.getCoord() == null || loc.getCoord().getLatitude() == null
                || loc.getCoord().getLongitude() == null) {
            MLog.info(TAG, "requestLocation from checkNeedRequestLocation");
            Location.getInstance().requestLocation();
        }
    }

    public void checkUploadedLoc() {
        LocationInfo loc = JSON.parseObject(Config.getLocation(), LocationInfo.class);
        if (loc != null && loc.getCoord() != null && loc.getCoord().getLatitude() != null
                && loc.getCoord().getLongitude() != null) {
            GlobalData.instance().getSystemInfo().setLocation(null);
            Config.saveUploadLocFlag(true);
        }
    }
}