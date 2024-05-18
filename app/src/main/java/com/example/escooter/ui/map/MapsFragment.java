package com.example.escooter.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.escooter.R;
import com.example.escooter.network.HttpRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class MapsFragment extends Fragment {

    private GoogleMap googleMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Polyline currentPolyline;
    private LatLng currentLatLng;
    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap map) {
            googleMap = map;
            // 開啟我的位置功能
            updateLocation();
            // 設定多邊形
//            setPolygon();
            // 設定點選事件
            setOnMarkerClick();
            // 新增車輛位置
            setRentableEscooter();
        }
    };

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(0)
                .setMinUpdateIntervalMillis(0)
                .setPriority(LocationRequest.CONTENTS_FILE_DESCRIPTOR)
                .build();
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
    private void updateLocationOnMap(Location location) {
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    private void updateLocation() {
        //設定地圖上的位置控制項
        if (googleMap == null) {
            return;
        }
        try {
            //設定google map的顯示格式與樣式
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.style_json));
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //設定google map的我的位置與定位按鈕
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                //設定固定位置
                LatLng startLocation = new LatLng(23.693802,120.533492);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 18));
            } else {
                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e)  {
            Toast.makeText(requireContext(), "Exception: %s" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void setPolygon() {
        // 新增多邊形
        Polygon polygon = googleMap.addPolygon(new PolygonOptions().clickable(true).add(
                new LatLng(23.6948, 120.5313),
                new LatLng(23.6960, 120.5379),
                new LatLng(23.6893, 120.5383),
                new LatLng(23.6882, 120.5332)));
        polygon.setTag("alpha");
        polygon.setStrokeColor(0x5000ABA5);
        polygon.setStrokeWidth(8);
        polygon.setFillColor(0x2000ABA5);
    }
    private void setOnMarkerClick() {
        //設定標籤點擊事件
        googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                //彈窗出現
                //引導線
                if (currentPolyline != null) {
                    currentPolyline.remove();
                }
//                LatLng currentLocation01 = new LatLng(23.693802, 120.533492);
                LatLng markerLocation = marker.getPosition();
                currentPolyline = googleMap.addPolyline(new PolylineOptions().clickable(true).add(currentLatLng,markerLocation));
                currentPolyline.setColor(0xffD08343);
                currentPolyline.setWidth(12);

                Toast.makeText(requireContext(), "緯度：" + currentLatLng, Toast.LENGTH_SHORT).show();

                return true;
            }
        });
    }
    private void setRentableEscooter(){
        String apiUrl = "http://36.232.105.97:8080/api/getRentableEscooterList";
        JSONObject postData = new JSONObject();
        try {
            postData.put("longitude", "120.534454");
            postData.put("latitude", "23.689305");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        HttpRequest getRentableEscooterList= new HttpRequest(apiUrl);
        // 發送 HTTP POST 請求
        getRentableEscooterList.httpPost(postData, new HttpRequest.ResultCallback<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    //json檔案資料處理
                    JSONObject escooters = result.getJSONObject("escooters");
                    Iterator<String> keys = escooters.keys();
                    while (keys.hasNext()){
                        String key = keys.next();
                        JSONObject escooter = escooters.getJSONObject(key);
                        int escooterId = escooter.getInt("escooterId");
                        JSONObject gps = escooter.getJSONObject("gps");
                        double latitude = gps.getDouble("latitude");
                        double longitude = gps.getDouble("longitude");
                        //切換為ui線程，才能使用google map的更改
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LatLng Escooter = new LatLng(latitude, longitude);
                                googleMap.addMarker(new MarkerOptions().position(Escooter).title("E-scooter"+escooterId).icon(BitmapDescriptorFactory.fromResource(R.drawable.escooter)));
                            }
                        });
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    private void runOnUiThread(Runnable action) {
        //切換為ui線程
        new Handler(Looper.getMainLooper()).post(action);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        // 獲取 SupportMapFragment 的實例
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // 異步加載地圖
        assert mapFragment != null;
        mapFragment.getMapAsync(callback);

        // 初始化 FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // 建立位置回調
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationOnMap(location);
                }
            }
        };

        return view;

    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            startLocationUpdates();
        } else {
            Snackbar.make(requireView(), "Location permission denied", Snackbar.LENGTH_LONG).show();
        }
    });

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 在 Fragment 銷毀時停止監聽位置
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

}
