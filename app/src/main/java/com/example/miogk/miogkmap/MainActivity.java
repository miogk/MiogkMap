package com.example.miogk.miogkmap;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {
    private AMap aMap;
    private MapView mMapView;
    private EditText address;
    private Button parse;
    private boolean isFirstLoc = true;
    private GeocodeSearch search;
    private AMapLocationClient aMapLocationClient;
    private String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private AMapLocationListener al;
    private RouteSearch routeSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        //赋予权限
        grand();
        aMapLocationClient = new AMapLocationClient(this);
        //                    onLocationChangedListener.onLocationChanged(aMapLocation);
        al = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (isFirstLoc) {
                    locationChanged(aMapLocation);
//                    onLocationChangedListener.onLocationChanged(aMapLocation);
                    isFirstLoc = false;
                }
            }
        };
        aMapLocationClient.setLocationListener(al);
        aMapLocationClient.startLocation();
        address = (EditText) findViewById(R.id.address);
        parse = (Button) findViewById(R.id.parse);
        parse.setOnClickListener(this);
        routeSearch = new RouteSearch(this);
        routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
                DrivePath drivePath = driveRouteResult.getPaths().get(0);
                List<DriveStep> steps = drivePath.getSteps();
                for (DriveStep step : steps) {
                    List<LatLonPoint> points = step.getPolyline();
                    List<LatLng> latLngs = new ArrayList<LatLng>();
                    for (LatLonPoint point : points) {
                        latLngs.add(new LatLng(point.getLatitude(), point.getLongitude()));
                    }
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(latLngs).width(8);
                    aMap.addPolyline(polylineOptions);
                }
            }

            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

            }

            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

            }
        });
        search = new GeocodeSearch(this);
        //Geocode地理代码
        search.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
                aMap.clear();
                GeocodeAddress address = geocodeResult.getGeocodeAddressList().get(0);
                LatLonPoint point = address.getLatLonPoint();
                Double g = point.getLongitude();
                Double t = point.getLatitude();
                AMapLocation al = aMapLocationClient.getLastKnownLocation();
                float distance = AMapUtils.calculateArea(new LatLng(al.getLatitude(), al.getLongitude()), new LatLng(t, g));
                Toast.makeText(MainActivity.this, "距离： " + String.valueOf(distance), Toast.LENGTH_SHORT).show();
                //创建路线规划的起始点
                RouteSearch.FromAndTo ft = new RouteSearch.FromAndTo(new LatLonPoint(al.getLatitude(), al.getLongitude()),
                        point);
                //创建自驾车的查询条件
                RouteSearch.DriveRouteQuery driveRouteQuery = new RouteSearch.DriveRouteQuery(ft,
                        RouteSearch.DrivingDefault,
                        null, null, null);
                routeSearch.calculateDriveRouteAsyn(driveRouteQuery);
//                LatLng ll = new LatLng(t, g);
//                CameraUpdate cameraUpdate = CameraUpdateFactory.changeLatLng(ll);
//                aMap.moveCamera(cameraUpdate);
//                cameraUpdate = CameraUpdateFactory.zoomTo(16f);
//                aMap.moveCamera(cameraUpdate);
//                MarkerOptions options = new MarkerOptions();
//                options.position(ll);
//                aMap.clear();
//                aMap.addMarker(options);
//                CircleOptions circleOptions = new CircleOptions()
//                        .center(ll).radius(80).strokeWidth(1).strokeColor(0xff000000);
//                aMap.addCircle(circleOptions);
            }
        });
    }

    private void locationChanged(AMapLocation aMapLocation) {
        LatLng ll = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
        CameraUpdate cu = new CameraUpdateFactory().changeLatLng(ll);
        aMap.moveCamera(cu);
        cu = new CameraUpdateFactory().zoomTo(16f);
        aMap.moveCamera(cu);
        MarkerOptions mo = new MarkerOptions();
        mo.position(ll);
        aMap.clear();
        aMap.addMarker(mo);
    }

    private void grand() {
        if (EasyPermissions.hasPermissions(this, permissions)) {
            Toast.makeText(this, "已有权限", Toast.LENGTH_SHORT).show();
        } else {
            EasyPermissions.requestPermissions(this, "授权定位功能才能使用本软件...", 1, permissions);
        }
//        List<String> grand = new ArrayList<>();
//        if (Build.VERSION.SDK_INT >= 23) {
//            for (int i = 0; i < permissions.length; i++) {
//                if (ActivityCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
//                    grand.add(permissions[i]);
//                }
//            }
//            ActivityCompat.requestPermissions(this, grand.toArray(new String[grand.size()]), 1);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
//        switch (requestCode) {
//            case 1:
//                if (grantResults.length > 0) {
//                    for (int grantResult : grantResults) {
//                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
//                            Toast.makeText(this, "要同意所有权限才可以使用本软件", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//                Toast.makeText(this, "权限未申请", Toast.LENGTH_SHORT).show();
//                break;
//        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.parse:
                String ad = address.getText().toString().trim();
                if (TextUtils.isEmpty(ad)) {
                    Toast.makeText(this, "请输入有效地址", Toast.LENGTH_SHORT).show();
                } else {
                    GeocodeQuery query = new GeocodeQuery(ad, "上海");
                    search.getFromLocationNameAsyn(query);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        aMapLocationClient.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
        AMapLocation aMapLocation = aMapLocationClient.getLastKnownLocation();
        locationChanged(aMapLocation);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
    }
}