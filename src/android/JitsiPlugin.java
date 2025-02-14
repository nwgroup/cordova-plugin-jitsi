package com.cordova.plugin.jitsi;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;
import android.content.Context;

import java.net.MalformedURLException;
import java.net.URL;

import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import android.view.View;

import org.apache.cordova.CordovaWebView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import com.facebook.react.modules.core.PermissionListener;

public class JitsiPlugin extends CordovaPlugin
        implements JitsiMeetActivityInterface, JitsiPluginModel.OnJitsiPluginStateListener {

    private CallbackContext _callback;
    private static final String TAG = "cordova-plugin-jitsi";

    final static String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    public static final int TAKE_PIC_SEC = 0;
    public static final int REC_MIC_SEC = 1;

    private String _conferenceState = "";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // your init code here
        JitsiPluginModel.getInstance().setListener(this);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // CB-10120: The CAMERA permission does not need to be requested unless it is
        // declared
        // in AndroidManifest.xml. This plugin does not declare it, but others may and
        // so we must
        // check the package info to determine if the permission is present.
        _conferenceState = JitsiPluginModel.getInstance().getState();
        checkPermission();

        _callback = callbackContext;

        Log.e(TAG, "ANDRESF: PASA");

        if (action.equals("join")) {
            try {

                String serverUrl = args.getString(0);
                String roomId = args.getString(1);
                Boolean audioOnly = args.getBoolean(2);
                Log.e(TAG, "ANDRESF: PASA 68");
                String room = args.getString(3);
                this.join(serverUrl, roomId, audioOnly, room);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            return true;
        } else if (action.equals("destroy")) {
            this.destroy(callbackContext);
            return true;
        }
        return false;
    }

    private void checkPermission() {
        boolean takePicturePermission = PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);
        boolean micPermission = PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO);

        // CB-10120: The CAMERA permission does not need to be requested unless it is
        // declared
        // in AndroidManifest.xml. This plugin does not declare it, but others may and
        // so we must
        // check the package info to determine if the permission is present.
        Log.e(TAG, "tp : " + takePicturePermission);
        Log.e(TAG, "mp : " + micPermission);

        if (!takePicturePermission) {
            takePicturePermission = true;

            try {
                PackageManager packageManager = this.cordova.getActivity().getPackageManager();
                String[] permissionsInPackage = packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(),
                        PackageManager.GET_PERMISSIONS).requestedPermissions;

                if (permissionsInPackage != null) {
                    for (String permission : permissionsInPackage) {
                        if (permission.equals(Manifest.permission.CAMERA)) {
                            takePicturePermission = false;
                            break;
                        }
                    }
                }
                Log.e(TAG, "10 : ");
            } catch (NameNotFoundException e) {
                // We are requesting the info for our package, so this should
                // never be caught
                Log.e(TAG, e.getMessage());
            }
        }

        if (!takePicturePermission) {
            PermissionHelper.requestPermissions(this, TAKE_PIC_SEC,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private void join(final String serverUrl, final String roomId, final Boolean audioOnly, final String token) {
        Log.e(TAG, "join called! Server: " + serverUrl + ", room : " + roomId);
        Log.e(TAG, token);

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Context context = cordova.getActivity();
                URL serverUrlObject;
                try {
                    serverUrlObject = new URL(serverUrl);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Invalid server URL!");
                }

                JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                        .setRoom(serverUrlObject.getProtocol() + "://" + serverUrlObject.getHost() + "/" + roomId)
                        .setSubject(" ")
                        //.setToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJud19hcHBfdmlkZW8iLCJyb29tIjoidXNlcmNoYXQ1NThkaXJlY2Npb25uZXR3b29kc25ldHRlc3RpbmduZXR3b29kc25ldDExMjMxNDQiLCJleHAiOjE5MzcwOTEwNTgsInN1YiI6Im1lZXQuZ3J1cG9udy5jb20iLCJhdWQiOiJud19hcHBfc2VjcmV0IiwibW9kZXJhdG9yIjoidHJ1ZSIsIm9wZW5CcmlkZ2VDaGFubmVsIjoidHJ1ZSIsImNvbnRleHQiOnsidXNlciI6eyJhdmF0YXIiOiIiLCJuYW1lIjoiIiwiZW1haWwiOiIiLCJpZCI6IiJ9fX0.qWeB2ogpPz7z_i9vbuzEfoJ4Xi3Lrq-NDmghjeALRjg")
                        .setToken(token)
                        .setAudioOnly(audioOnly)
                        .setFeatureFlag("chat.enabled", false)
                        .setFeatureFlag("invite.enabled", false)
                        .setFeatureFlag("calendar.enabled", false)
                        .setWelcomePageEnabled(false).build();

                JitsiMeetPluginActivity.launchActivity(cordova.getActivity(), options);
            }
        });
    }

    private void destroy(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                JitsiMeetActivityDelegate.onHostDestroy(cordova.getActivity());
                cordova.getActivity().setContentView(getView());
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "DESTROYED");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    private View getView() {
        try {
            return (View) webView.getClass().getMethod("getView").invoke(webView);
        } catch (Exception e) {
            return (View) webView;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
        JitsiMeetActivityDelegate.requestPermissions(cordova.getActivity(), permissions, requestCode, listener);
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(String permissions) {
        return true;
    }

    @Override
    public int checkSelfPermission(String permission) {
        return 0;
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return 0;
    }

    @Override
    public void stateChanged() {
        _conferenceState = JitsiPluginModel.getInstance().getState();
        Log.d(TAG, "MainActivity says: Model state changed: " + _conferenceState);
        cordova.getActivity().setContentView(getView());
        String m = "";

        switch (_conferenceState) {
            case "onConferenceJoined":
                m = "CONFERENCE_JOINED";
                break;
            case "onConferenceWillJoin":
                m = "CONFERENCE_WILL_JOIN";
                break;
            case "onConferenceTerminated":
                m = "CONFERENCE_TERMINATED";
                break;
            case "onConferenceFinished":
                m = "CONFERENCE_FINISHED";
                break;
            case "onConferenceDestroyed":
                m = "CONFERENCE_DESTROYED";
                break;
        }

        if (m != "") {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, m);
            pluginResult.setKeepCallback(true);
            _callback.sendPluginResult(pluginResult);
        }
    }
}
