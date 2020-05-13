package com.ase.taklitgpskonum;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cursoradapter.widget.CursorAdapter;

import com.ase.taklitgpskonum.constant.DbConstantes;
import com.ase.taklitgpskonum.constant.GeralConstantes;
import com.ase.taklitgpskonum.db.DbFakeGpsHelper;
import com.ase.taklitgpskonum.db.TableHistorico;
import com.ase.taklitgpskonum.services.FakeService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleMap.OnMapClickListener, OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker marker;
    private Intent itService;
    private Menu menu;
    private LatLng latLng;
    private SharedPreferences prefService;
    private DbFakeGpsHelper dbFakeGpsHelper;
    private TableHistorico tHistorico;
    private CursorAdapter cursorAdapter;
    boolean isPermitted = false;
    private InterstitialAd interstitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((UILApplication) getApplication()).getTracker(UILApplication.TrackerName.APP_TRACKER);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();








        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            AdView mAdView = (AdView) findViewById(R.id.adView);



            // Initialize the Mobile Ads SDK
            MobileAds.initialize(this, getString(R.string.admob_banner_main_id));
            AdRequest adIRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adIRequest);
            // Prepare the Interstitial Ad Activity
            interstitial = new InterstitialAd(MainActivity.this);

            // Insert the Ad Unit ID
            interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));

            // Interstitial Ad load Request
            interstitial.loadAd(adIRequest);

            // Prepare an Interstitial Ad Listener
            interstitial.setAdListener(new AdListener()
            {
                public void onAdLoaded()
                {
                    // Call displayInterstitial() function when the Ad loads
                    displayInterstitial();
                }
            });



        }







        itService = new Intent(this, FakeService.class);


        checkRunTimePermission();


    }

    private void checkRunTimePermission() {
        String[] permissionArrays = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissionArrays, 11111);
        } else {
            intt();
            // if already permition granted
            // PUT YOUR ACTION (Like Open cemara etc..)
        }
    }

    private void intt() {


        if (setUpMapIfNeeded()) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            mMap.setOnMapClickListener(this);
        }

        prefService = this.getSharedPreferences(GeralConstantes.PREFS_SERVICE_NAME, Context.MODE_PRIVATE);
        latLng = new LatLng(
                Double.longBitsToDouble(prefService.getLong(GeralConstantes.PREFS_SERVICE_LAT_TAG, 0)),
                Double.longBitsToDouble(prefService.getLong(GeralConstantes.PREFS_SERVICE_LONG_TAG, 0))

        );

        int gPlayServiceStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean openActivityOnce = true;
        boolean openDialogOnce = true;
        if (requestCode == 11111) {
            for (int i = 0; i < grantResults.length; i++) {
                String permission = permissions[i];

                isPermitted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    boolean showRationale = shouldShowRequestPermissionRationale(permission);
                    if (!showRationale) {
                        //execute when 'never Ask Again' tick and permission dialog not show
                    } else {
                        if (openDialogOnce) {
                            alertView();
                        }
                    }
                }
            }

            if (isPermitted) {
                intt();
            }
        }
    }


    private void alertView() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

        dialog.setTitle("Permission Denied")
                .setInverseBackgroundForced(true)
                //.setIcon(R.drawable.ic_info_black_24dp)
                .setMessage("Without those permission the app is unable to save your profile. App needs to save profile image in your external storage and also need to get profile image from camera or external storage.Are you sure you want to deny this permission?")

                .setNegativeButton("I'M SURE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        dialoginterface.dismiss();
                    }
                })
                .setPositiveButton("RE-TRY", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        dialoginterface.dismiss();
                        checkRunTimePermission();

                    }
                }).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.historico_context_menu, menu);

        MenuItem menuItemRemover = menu.add(0, 0, 0, R.string.remover);

        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuItemRemover.getMenuInfo();

        menuItemRemover.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                dbFakeGpsHelper = new DbFakeGpsHelper(MainActivity.this);
                tHistorico = new TableHistorico(MainActivity.this);
                tHistorico.deleteHistorico(info.id);
                dbFakeGpsHelper.close();
                Cursor cursor = tHistorico.getHistorico();
                cursorAdapter.swapCursor(cursor);
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        if (FakeService.running) {
            menu.findItem(R.id.menuStart).setVisible(false);
            menu.findItem(R.id.menuPause).setVisible(true);
        } else {
            menu.findItem(R.id.menuStart).setVisible(true);
            menu.findItem(R.id.menuPause).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menuStart:
                if (isMockSettingsON()) {
                    if (marker != null) {

                        SharedPreferences.Editor editor = prefService.edit();
                        editor.putLong(GeralConstantes.PREFS_SERVICE_LAT_TAG, Double.doubleToLongBits(latLng.latitude));
                        editor.putLong(GeralConstantes.PREFS_SERVICE_LONG_TAG, Double.doubleToLongBits(latLng.longitude));
                        editor.commit();

                        List<Address> listSearch = searchLocation(latLng.latitude, latLng.longitude, 1);
                        if (listSearch != null && !listSearch.isEmpty()) {

                            addHistorico(listSearch);
                        }

                        startService(itService);
                        menu.findItem(R.id.menuStart).setVisible(false);
                        menu.findItem(R.id.menuPause).setVisible(true);

                        Toast.makeText(this, getString(R.string.toast_service_start), Toast.LENGTH_SHORT).show();

                        finish();
                    }
                } else {
                    openDevSettings();
                }
                return true;
            case R.id.menuPause:
                stopService(itService);
                menu.findItem(R.id.menuStart).setVisible(true);
                menu.findItem(R.id.menuPause).setVisible(false);
                Toast.makeText(this, getString(R.string.toast_service_pause), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menuSearch:

                final Dialog searchDialog = new Dialog(this);
                searchDialog.setContentView(R.layout.dialog_search);
                searchDialog.setTitle(getString(R.string.search));

                final EditText searchEdit = (EditText) searchDialog.findViewById(R.id.searchEdit);
                ImageButton searchButton = (ImageButton) searchDialog.findViewById(R.id.searchButton);

                searchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (searchEdit.getText() != null && !searchEdit.getText().toString().equals("")) {
                            try {
                                List<Address> listSearch = searchLocationName(searchEdit.getText().toString(), 1);
                                if (listSearch != null && !listSearch.isEmpty()) {
                                    latLng = new LatLng(listSearch.get(0).getLatitude(), listSearch.get(0).getLongitude());
                                    updateCameraZoom(latLng);
                                    searchDialog.dismiss();
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(getString(R.string.dialog_local_confirm_title))
                                            .setMessage(getString(R.string.dialog_local_confirm_msg))
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    addMark(latLng);
                                                }
                                            }).show();
                                    //Log.d("SEARCH", "Lista: " + listSearch);
                                } else {
                                    Toast.makeText(getBaseContext(), R.string.toast_address_notfound, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getBaseContext(), R.string.toast_address_notfound, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            searchEdit.setError(getString(R.string.toast_inform_search_error));
                        }
                    }
                });

                searchDialog.show();

                return true;
            case R.id.menuSobre:
                showSobre();
                return true;
            case R.id.menuHistorico:
                abrirHistoricoDialog();
                return true;
        }

        return false;
    }

    private void abrirHistoricoDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(R.string.historico);
        dialog.setContentView(R.layout.dialog_historico);

        ListView listView = (ListView) dialog.findViewById(R.id.listHistorico);
        TextView semRegistroTextView = (TextView) dialog.findViewById(R.id.semRegistroTextView);

        DbFakeGpsHelper db = new DbFakeGpsHelper(this);
        TableHistorico tHistorico = new TableHistorico(this);

        Cursor cursor = tHistorico.getHistorico();

        if (cursor == null || cursor.getCount() <= 0) {
            semRegistroTextView.setVisibility(View.VISIBLE);
        } else {
            cursorAdapter = new CursorAdapter(this, cursor, 0) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
                }

                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    TextView enderecoTextView = (TextView) view.findViewById(android.R.id.text1);
                    TextView latlongTextView = (TextView) view.findViewById(android.R.id.text2);

                    String textLatLong = "Lat: " + cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_COORD_X_HISTORICO)) + " | " +
                            "Long: " + cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_COORD_Y_HISTORICO));

                    enderecoTextView.setText(cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_ENDERECO_HISTORICO)));
                    latlongTextView.setText(textLatLong);
                }
            };

            listView.setAdapter(cursorAdapter);

            registerForContextMenu(listView);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (menu.findItem(R.id.menuStart).isVisible()) {
                        TextView latlongTextView = (TextView) view.findViewById(android.R.id.text2);
                        String[] latLongText = latlongTextView.getText().toString().split(" ");

                        latLng = new LatLng(Double.parseDouble(latLongText[1]), Double.parseDouble(latLongText[4]));
                        updateCameraZoom(latLng);
                        addMark(latLng);
                    } else {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.alert_servico_rodando_title))
                                .setMessage(getString(R.string.alert_servico_rodando))
                                .setPositiveButton(getString(R.string.ok), null).show();
                    }
                    dialog.dismiss();
                }
            });

        }

        dialog.show();
    }

    private void addHistorico(List<Address> a) {
        dbFakeGpsHelper = new DbFakeGpsHelper(this);
        tHistorico = new TableHistorico(this);

        String endereco = a.get(0).getAddressLine(0) + ", " + a.get(0).getAddressLine(2) + ", " + a.get(0).getAddressLine(1) + ", " + a.get(0).getAddressLine(3);
        endereco = endereco.replace("null", "unknown");
        tHistorico.insertDados(endereco, a.get(0).getLatitude(), a.get(0).getLongitude());
        dbFakeGpsHelper.close();
    }

    private void showSobre() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            Calendar calendar = Calendar.getInstance();
            int ano = calendar.get(Calendar.YEAR);

            if (versionName != null) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setMessage(String.format(getString(R.string.menu_sobre), getString(R.string.app_name), versionName, ano))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(R.string.ok, null).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void openDevSettings() {
        int dMessage;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dMessage = R.string.alert_mocksetting_and6_msg;
        } else {
            dMessage = R.string.alert_mocksetting_msg;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(dMessage)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            Intent intent = new Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                            startActivity(intent);
                        } else {

                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                            //Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                            startActivity(intent);
                        }

                    }
                }).show();
    }

    private boolean isMockSettingsON() {
        boolean isMockLocation = false;
        try {
            //if marshmallow
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID) == AppOpsManager.MODE_ALLOWED);
            } else {
                // in marshmallow this will always return true
                isMockLocation = !android.provider.Settings.Secure.getString(getContentResolver(), "mock_location").equals("0");
            }
        } catch (Exception e) {
            return isMockLocation;
        }
        return isMockLocation;
    }

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(this);

            if (mMap != null) {
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.setIndoorEnabled(true);
                mMap.setTrafficEnabled(true);
            }
            return mMap != null;
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap=map;

        LatLng lat = new LatLng(0, 0);
        map.addMarker(new MarkerOptions().position(lat).title("Marker"));
        CameraPosition position = new CameraPosition.Builder()
                .target(lat)
                .zoom(6)
                .tilt(8f)
                .build();
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
        mMap.animateCamera(update);

//        map.getUiSettings().setZoomControlsEnabled(true);
//        map.getUiSettings().setCompassEnabled(true);
//        map.getUiSettings().setMyLocationButtonEnabled(true);
//        map.getUiSettings().setMapToolbarEnabled(true);
//        map.getUiSettings().setZoomGesturesEnabled(true);
//        map.getUiSettings().setScrollGesturesEnabled(true);
//        map.getUiSettings().setTiltGesturesEnabled(true);
//        map.getUiSettings().setRotateGesturesEnabled(true);
    }


    private List searchLocationName(String endereco, int max) {
        Geocoder gc = new Geocoder(this, Locale.getDefault());
        try {
            return gc.getFromLocationName(endereco, max);
        } catch (IOException e) {
            return null;
        }
    }

    private List searchLocation(double latitude, double longitude, int max) {
        Geocoder gc = new Geocoder(this, Locale.getDefault());
        try {
            return gc.getFromLocation(latitude, longitude, max);
        } catch (IOException e) {
            return null;
        }
    }

    private void addMark(LatLng latLng) {
        if (marker != null) {
            marker.remove();
        }
        this.latLng = latLng;
        marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Lat: " + latLng.latitude + " | " + "Long: " + latLng.longitude));
    }

    private void updateCamera(LatLng latLng) {
        CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(update);
    }

    private void updateCameraZoom(LatLng latLng) {
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(19)
                .tilt(13f)
                .build();
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
        mMap.animateCamera(update);
    }


    @Override
    public void onMapClick(LatLng latLng) {

        if (menu.findItem(R.id.menuStart).isVisible()) {
            addMark(latLng);
            updateCamera(latLng);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.alert_servico_rodando_title))
                    .setMessage(getString(R.string.alert_servico_rodando))
                    .setPositiveButton(getString(R.string.ok), null).show();
        }

    }


    public void displayInterstitial()
    {
        // If Interstitial Ads are loaded then show else show nothing.
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }

}
