package com.bridgefy.react.sdk.framework;

import android.util.Log;

import com.bridgefy.react.sdk.utils.Utils;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.framework.exceptions.MessageException;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.google.gson.Gson;

/**
 * @author kekoyde on 6/9/17.
 */

class BridgefyMessages extends MessageListener {
    private ReactContext reactContext;

    public BridgefyMessages(ReactContext reactContext) {
        super();
        this.reactContext = reactContext;
    }

    @Override
    public void onMessageReceived(Message message) {
        Utils.sendEvent(reactContext,"onMessageReceived", Utils.getMapForMessage(message));
    }

    @Override
    public void onMessageReceivedException(String sender, MessageException e) {
        WritableMap writableMap = Arguments.createMap();
        writableMap.putString("sender", sender);
        writableMap.putMap("message", Arguments.createMap());
        writableMap.putString("description", "Failed to get the original message: [" + e.getMessage()+"]");
        writableMap.putInt("code", 100);
        Utils.sendEvent(reactContext,"onMessageReceivedException", writableMap);
    }

    @Override
    public void onMessageFailed(Message message, MessageException e) {
        WritableMap writableMap = Arguments.createMap();
        writableMap.putMap("message", Utils.getMapForMessage(message));
        writableMap.putString("description", e.getMessage());
        writableMap.putInt("code", 101);
        Utils.sendEvent(reactContext,"onMessageFailed", writableMap);
    }

    @Override
    public void onBroadcastMessageReceived(Message message) {
        Log.e("BridgefyMessages", "onBroadcastMessageReceived: " + new Gson().toJson(message));
        Utils.sendEvent(reactContext,"onBroadcastMessageReceived", Utils.getMapForMessage(message));
    }
}
