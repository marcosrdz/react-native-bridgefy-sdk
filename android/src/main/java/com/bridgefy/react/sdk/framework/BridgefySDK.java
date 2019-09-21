package com.bridgefy.react.sdk.framework;

import com.bridgefy.react.sdk.utils.BridgefyEvent;
import com.bridgefy.react.sdk.utils.Utils;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.RegistrationListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;

/**
 * @author kekoyde on 6/9/17.
 */

public class BridgefySDK extends RegistrationListener{

    private ReactContext reactContext;
    private Callback errorRegisterCallback;
    private Callback successRegisterCallback;

    public BridgefySDK(ReactContext reactContext){
        this.reactContext = reactContext;
    }

    public void sendMessage(Message message)
    {
        Bridgefy.sendMessage(message);
    }

    public void sendBroadcastMessage(Message message)
    {
        Bridgefy.sendBroadcastMessage(message);
    }

    public void initialize(String apiKey, Callback error, Callback success)
    {
        this.errorRegisterCallback = error;
        this.successRegisterCallback = success;
        Utils.onEventOccurred(reactContext, BridgefyEvent.BFEventStartWaiting.getValue(), "Waiting for online validation to start the transmitter.");
        Bridgefy.initialize(reactContext.getApplicationContext(), apiKey, this);
    }

    public void startSDK(){

        Bridgefy.start(
                new BridgefyMessages(reactContext),
                new BridgefyDevices(reactContext)
        );

    }

    @Override
    public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
        successRegisterCallback.invoke(Utils.getBridgefyClient(bridgefyClient));
    }

    @Override
    public void onRegistrationFailed(int errorCode, String message) {
        errorRegisterCallback.invoke(errorCode, message);
    }
}
