
package io.hypertrack;

import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.LifecycleEventListener;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.HyperTrackConstants;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.callbacks.HyperTrackEventCallback;
import com.hypertrack.lib.internal.transmitter.models.HyperTrackLocation;
import com.hypertrack.lib.internal.common.models.GeoJSONLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class RNHyperTrackModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private final StatusBroadcastReceiver mStatusBroadcastReceiver;

    public RNHyperTrackModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.mStatusBroadcastReceiver = new StatusBroadcastReceiver();

        // Set Callback to receive events & errors
        HyperTrack.setCallback(new HyperTrackEventCallback() {
            @Override
            public void onEvent(@NonNull final HyperTrackEvent event) {
                // handle event received here
            }

            @Override
            public void onError(@NonNull final ErrorResponse errorResponse) {
                // handle event received here
            }
        });
    }

    @Override
    public String getName() {
        return "RNHyperTrack";
    }

    @ReactMethod
    public void initialize(String publishableKey) {
        HyperTrack.initialize(getReactApplicationContext(), publishableKey);
    }

    @ReactMethod
    public void getPublishableKey(final Callback callback) {
        Context context = getReactApplicationContext();
        callback.invoke(HyperTrack.getPublishableKey(context));
    }

    @ReactMethod
    public void createUser(String userName, final Callback successCallback, final Callback errorCallback) {
        HyperTrack.createUser(userName, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                // Return User object in successCallback
                successCallback.invoke(response.getResponseObject());
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                errorCallback.invoke(errorResponse);
            }
        });
    }

    @ReactMethod
    public void setUserId(String userId) {
        HyperTrack.setUserId(userId);
    }

    @ReactMethod
    public void getUserId(final Callback callback) {
        callback.invoke(HyperTrack.getUserId());
    }

    @ReactMethod
    public void startTracking(final Callback successCallback, final Callback errorCallback) {
        HyperTrack.startTracking(new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                // Return User object in successCallback
                successCallback.invoke(response.getResponseObject());
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                errorCallback.invoke(errorResponse);
            }
        });
    }

    @ReactMethod
    public void isTracking(final Callback callback) {
        callback.invoke(HyperTrack.isTracking());
    }

    @ReactMethod
    public void completeAction(String actionId) {
        HyperTrack.completeAction(actionId);
    }

    @ReactMethod
    public void stopTracking(final Callback successCallback, final Callback errorCallback) {
        HyperTrack.stopTracking(new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse successResponse) {
                successCallback.invoke(response.getResponseObject());
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                errorCallback.invoke(errorResponse);
            }
        });
    }

    @Override
    public void onHostDestroy() {
        LocalBroadcastManager.getInstance(getReactApplicationContext()).unregisterReceiver(mStatusBroadcastReceiver);
    }

    @Override
    public void onHostPause() { }

    @Override
    public void onHostResume() { }

    private ArrayList<String> toArrayList(ReadableArray taskIDs) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; i < taskIDs.size(); i++) {
            switch (taskIDs.getType(i)) {
                case String:
                    arrayList.add(taskIDs.getString(i));
                    break;
                default:
                    throw new IllegalArgumentException("Task ID at index " + i + " should be String");
            }
        }
        return arrayList;
    }

    private void sendCurrentLocation(HyperTrackLocation hyperTrackLocation) {
        WritableMap params = Arguments.createMap();
        GeoJSONLocation location = hyperTrackLocation.getGeoJSONLocation();

        params.putDouble("latitude", location.getLatitude());
        params.putDouble("longitude", location.getLongitude());
        sendEvent("currentLocationDidChange", params);
    }

    private void sendEvent(String eventName, WritableMap params) {
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private class StatusBroadcastReceiver extends BroadcastReceiver {
        private StatusBroadcastReceiver() { }

        public void onReceive(Context paramContext, Intent paramIntent) {
             if (paramIntent.getAction().equals(HyperTrackConstants.HT_USER_CURRENT_LOCATION_INTENT)) {
                 HyperTrackLocation location = (HyperTrackLocation) paramIntent.getSerializableExtra(
                         HyperTrackConstants.HT_USER_CURRENT_LOCATION_KEY);

                 RNHyperTrackModule.this.sendCurrentLocation(location);
             }
        }
    }
}
