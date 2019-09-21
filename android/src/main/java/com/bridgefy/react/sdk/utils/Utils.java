package com.bridgefy.react.sdk.utils;

import android.support.annotation.Nullable;

import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.DeviceProfile;
import com.bridgefy.sdk.client.Message;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author kekoyde on 6/12/17.
 */

public class Utils {

    private static final Object object = new Object();

    private Utils(){}

    private static final String CONTENT = "content", RECEIVER_ID = "receiver_id", SENDER_ID = "sender_id";

    public static synchronized WritableMap getBridgefyClient(BridgefyClient bridgefyClient)
    {
        WritableMap map = new WritableNativeMap();
        map.putString("api_key", bridgefyClient.getApiKey());
        map.putString("bundleId",bridgefyClient.getBundleId());
        map.putString("public_key", bridgefyClient.getPublicKey());
        map.putString("secret_key", bridgefyClient.getSecretKey());
        map.putString("userUuid", bridgefyClient.getUserUuid());
        map.putMap("deviceProfile", getDeviceProfile(bridgefyClient));
        return map;
    }

    public static synchronized WritableMap getMapForMessage(Message message)
    {
        WritableMap mapMessage = new WritableNativeMap();
        mapMessage.putString("receiverId", message.getReceiverId());
        mapMessage.putString("senderId", message.getSenderId());
        mapMessage.putString("uuid", message.getUuid());
        mapMessage.putDouble("dateSent", message.getDateSent());
        if (message.getContent() != null) {
            mapMessage.putMap("content", toWritableMap(message.getContent()));
        }
        return mapMessage;
    }

    public static synchronized WritableMap getMapForDevice(Device device)
    {
        WritableMap mapDevice = new WritableNativeMap();
        mapDevice.putString("userId",device.getUserId());
        mapDevice.putString("deviceAddress",device.getDeviceAddress());
        mapDevice.putString("deviceName",device.getDeviceName());
        mapDevice.putString("sessionId",device.getSessionId());
        mapDevice.putString("deviceType",device.getAntennaType().toString());
        mapDevice.putDouble("crc",device.getCrc());
        mapDevice.putInt("retries", 3);
        return mapDevice;
    }

    private static synchronized WritableMap getDeviceProfile(BridgefyClient bridgefyClient)
    {
        DeviceProfile deviceProfile = bridgefyClient.getDeviceProfile();
        WritableMap mapDeviceProfile = new WritableNativeMap();
        mapDeviceProfile.putString("deviceEvaluation", deviceProfile.getDeviceEvaluation());
        mapDeviceProfile.putInt("rating", deviceProfile.getRating());
        return mapDeviceProfile;
    }

    public static synchronized void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params)
    {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    public static void onEventOccurred(ReactContext context, int code, String description)
    {
        synchronized (object) {
            WritableMap writableMap = Arguments.createMap();
            writableMap.putString("description", description);
            writableMap.putInt("code", code);
            sendEvent(context, "onEventOccurred", writableMap);
        }
    }

    public static synchronized Message getMessageFromMap(ReadableMap readableMap)
    {
        HashMap<String, Object> content = recursivelyDeconstructReadableMap(readableMap.getMap(CONTENT));
        return new Message(content,
                readableMap.hasKey(RECEIVER_ID)?readableMap.getString(RECEIVER_ID):"",
                readableMap.hasKey(SENDER_ID)?readableMap.getString(SENDER_ID):""
        );
    }

    private static HashMap<String, Object> recursivelyDeconstructReadableMap(ReadableMap readableMap)
    {
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        HashMap<String, Object> deconstructedMap = new HashMap<>();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType type = readableMap.getType(key);
            switch (type) {
                case Null:
                    deconstructedMap.put(key, null);
                    break;
                case Boolean:
                    deconstructedMap.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    deconstructedMap.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    deconstructedMap.put(key, readableMap.getString(key));
                    break;
                case Map:
                    deconstructedMap.put(key, recursivelyDeconstructReadableMap(readableMap.getMap(key)));
                    break;
                case Array:
                    deconstructedMap.put(key, recursivelyDeconstructReadableArray(readableMap.getArray(key)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
            }

        }
        return deconstructedMap;
    }

    private static List<Object> recursivelyDeconstructReadableArray(ReadableArray readableArray)
    {
        List<Object> deconstructedList = new ArrayList<>(readableArray.size());
        for (int i = 0; i < readableArray.size(); i++) {
            ReadableType indexType = readableArray.getType(i);
            switch(indexType) {
                case Null:
                    deconstructedList.add(i, null);
                    break;
                case Boolean:
                    deconstructedList.add(i, readableArray.getBoolean(i));
                    break;
                case Number:
                    deconstructedList.add(i, readableArray.getDouble(i));
                    break;
                case String:
                    deconstructedList.add(i, readableArray.getString(i));
                    break;
                case Map:
                    deconstructedList.add(i, recursivelyDeconstructReadableMap(readableArray.getMap(i)));
                    break;
                case Array:
                    deconstructedList.add(i, recursivelyDeconstructReadableArray(readableArray.getArray(i)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object at index " + i + ".");
            }
        }
        return deconstructedList;
    }

    private static WritableArray toWritableArray(Object[] array)
    {
        WritableArray writableArray = Arguments.createArray();

        for(int i = 0; i < array.length; i++) {
            Object value = array[i];

            if (value == null) {
                writableArray.pushNull();
            }
            if (value instanceof Boolean) {
                writableArray.pushBoolean((Boolean) value);
            }
            if (value instanceof Double) {
                writableArray.pushDouble((Double) value);
            }
            if (value instanceof Integer) {
                writableArray.pushInt((Integer) value);
            }
            if (value instanceof String) {
                writableArray.pushString((String) value);
            }
            if (value instanceof Map) {
                writableArray.pushMap(toWritableMap((Map<String, Object>) value));
            }

            if (value != null && value.getClass().isArray()) {
                writableArray.pushArray(toWritableArray((Object[]) value));
            }
        }

        return writableArray;
    }

    private static WritableMap toWritableMap(Map<String, Object> map) {
        WritableMap writableMap = Arguments.createMap();
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Object> pair = iterator.next();
            Object value = pair.getValue();

            if (value == null) {
                writableMap.putNull(pair.getKey());
            } else if (value instanceof Boolean) {
                writableMap.putBoolean(pair.getKey(), (Boolean) value);
            } else if (value instanceof Double) {
                writableMap.putDouble(pair.getKey(), (Double) value);
            } else if (value instanceof Integer) {
                writableMap.putInt(pair.getKey(), (Integer) value);
            } else if (value instanceof String) {
                writableMap.putString(pair.getKey(), (String) value);
            } else if (value instanceof Map) {
                writableMap.putMap(pair.getKey(), toWritableMap((Map<String, Object>) value));
            } else if (value.getClass() != null && value.getClass().isArray()) {
                writableMap.putArray(pair.getKey(), toWritableArray((Object[]) value));
            }

            iterator.remove();
        }

        return writableMap;
    }

}
