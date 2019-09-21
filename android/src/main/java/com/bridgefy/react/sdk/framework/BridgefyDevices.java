package com.bridgefy.react.sdk.framework;

import com.bridgefy.react.sdk.utils.BridgefyEvent;
import com.bridgefy.react.sdk.utils.Utils;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;

/**
 * @author kekoyde on 6/9/17.
 */

class BridgefyDevices extends StateListener {
    private ReactContext reactContext;

    public BridgefyDevices(ReactContext reactContext) {
        this.reactContext = reactContext;
    }

    @Override
    public void onStarted() {
        Utils.onEventOccurred(reactContext, BridgefyEvent.BFEventStartFinished.getValue(), "The Bridgefy was started.");
        Utils.sendEvent(reactContext,"onStarted", Arguments.createMap());
    }

    @Override
    public void onStartError(String message, int errorCode) {
        WritableMap writableMap = Arguments.createMap();
        writableMap.putMap("message", Arguments.createMap());
        writableMap.putString("description", message);
        writableMap.putInt("code", errorCode);
        Utils.sendEvent(reactContext, "onStartError", writableMap);
    }

    @Override
    public void onStopped() {
        Utils.sendEvent(reactContext,"onStopped", Arguments.createMap());
    }

    @Override
    public void onDeviceConnected(Device device, Session session) {
        WritableMap writableMap = Utils.getMapForDevice(device);
        writableMap.putString("publicKey", session.getPublicKey());
        Utils.sendEvent(reactContext,"onDeviceConnected", writableMap);
        Utils.onEventOccurred(reactContext, BridgefyEvent.BFEventNearbyPeerDetected.getValue(), "The Bridgefy was started.");
    }

    @Override
    public void onDeviceLost(Device device) {
        Utils.sendEvent(reactContext,"onDeviceLost", Utils.getMapForDevice(device));
    }
}
