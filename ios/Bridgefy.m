//
//  Bridgefy.m
//  AwesomeProject
//
//  Created by Danno on 6/15/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "Bridgefy.h"
#import <BFTransmitter/BFTransmitter.h>

#ifndef BRIDGEFY_E
#define BRIDGEFY_E
#define kMessageReceived @"onMessageReceived"
#define kMessageSent @"onMessageSent"
#define kMessageReceivedError @"onMessageReceivedException"
#define kMessageSentError @"onMessageFailed"
#define kBroadcastReceived @"onBroadcastMessageReceived"
#define kStarted @"onStarted"
#define kStartedError @"onStartError"
#define kStopped @"onStopped"
#define kDeviceConnected @"onDeviceConnected"
#define kDeviceDisconnected @"onDeviceLost"
#define kEventOccurred @"onEventOccurred"
#endif

@interface Bridgefy()<BFTransmitterDelegate> {
}

@property (nonatomic, retain) BFTransmitter * transmitter;
@property (nonatomic, retain) NSMutableDictionary * transitMessages;

@end

@implementation Bridgefy
RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents {
    return @[
             kMessageReceived,
             kMessageSent,
             kMessageReceivedError,
             kMessageSentError,
             kBroadcastReceived,
             kStarted,
             kStartedError,
             kStopped,
             kDeviceConnected,
             kDeviceDisconnected,
             kEventOccurred
             ];
}

RCT_REMAP_METHOD(init, startWithApiKey:(NSString *)apiKey errorCallBack:(RCTResponseSenderBlock)errorCallBack successCallback:(RCTResponseSenderBlock)successCallback) {
    if (self.transmitter != nil) {
        NSDictionary * dictionary = [self createClientDictionary];
        successCallback(@[dictionary]);
    }
    self.transmitter = [[BFTransmitter alloc] initWithApiKey:apiKey];
    
    if (self.transmitter != nil) {
        self.transmitter.delegate = self;
        NSDictionary * dictionary = [self createClientDictionary];
        successCallback(@[dictionary]);
        _transitMessages = [[NSMutableDictionary alloc] init];
    } else {
        self.transmitter.delegate = self;
        errorCallBack(@[@(60000), @"Bridgefy could not be initialized."]);
    }
    
}

RCT_EXPORT_METHOD(start) {
    if ( self.transmitter == nil ) {
        RCTLogError(@"Bridgefy was not initialized, the operation won't continue.");
        return;
    }
    [self.transmitter start];
}

RCT_EXPORT_METHOD(stop) {
    [self.transmitter stop];
    [self.bridge.eventDispatcher sendDeviceEventWithName:kStopped body:@{}];
}

RCT_REMAP_METHOD(sendMessage, sendMessage:(NSDictionary *) message) {
    BFSendingOption options = (BFSendingOptionEncrypted | BFSendingOptionFullTransmission);
    [self sendMessage:message WithOptions:options];
}

RCT_REMAP_METHOD(sendBroadcastMessage, sendBroadcastMessage:(NSDictionary *) message) {
    BFSendingOption options = (BFSendingOptionBroadcastReceiver | BFSendingOptionMeshTransmission);
    [self sendMessage:message WithOptions:options];
}

- (void)sendMessage:(NSDictionary *)message WithOptions: (BFSendingOption)options {
    
    if (![self transmitterCanWork]) {
        return;
    }
    
    if (message[@"content"] == nil) {
        RCTLogError(@"The field 'content' is missing, the message won't be sent: %@", [message description]);
        return;
    }
    
    if (message[@"receiver_id"] == nil && (options & BFSendingOptionBroadcastReceiver) == 0) {
        RCTLogError(@"The field 'receiver_id' is missing, the message won't be sent: %@", [message description]);
        return;
    }
    
    NSError * error = nil;
    
    NSString * packetID = [self.transmitter sendDictionary:message[@"content"]
                                                    toUser:message[@"receiver_id"]
                                                   options:options
                                                     error:&error];
    
    NSDictionary * createdMessage = [self createMessageDictionaryWithPayload:message[@"content"]
                                                                      sender:self.transmitter.currentUser
                                                                    receiver:message[@"receiver_id"]
                                                                        uuid:packetID];
    
    if (error == nil) {
        // Message began the sending process
        self.transitMessages[packetID] = createdMessage;
    } else {
        // Error sending the message
        NSDictionary * errorDict = @{
                                     @"code": @(error.code),
                                     @"description": error.localizedDescription,
                                     @"origin": createdMessage
                                     };
        [self.bridge.eventDispatcher sendDeviceEventWithName:kMessageSentError body:errorDict];
        
    }
    
}

#pragma mark - Utils

-(BOOL)transmitterCanWork {
    if ( self.transmitter == nil ) {
        RCTLogError(@"Bridgefy was not initialized, the operation won't continue.");
        return NO;
    }
    
    if (!self.transmitter.isStarted) {
        RCTLogError(@"Bridgefy was not started, the operation won't continue.");
        return NO;
    }
    
    return YES;
}

- (NSDictionary *)createClientDictionary {
    NSLog(@"Public %@", self.transmitter.localPublicKey);
    NSLog(@"userUUID %@", self.transmitter.currentUser);
    return @{
             @"api_key": @"",
             @"bundle_id": @"",
             @"public_key": self.transmitter.localPublicKey,
             @"secret_key": @"",
             @"userUuid": self.transmitter.currentUser,
             @"deviceProfile": @""
             };
}

- (NSDictionary *)createMessageDictionaryWithPayload:(NSDictionary *)payload
                                              sender:(NSString *)sender
                                            receiver:(NSString *) receiver
                                                uuid:(NSString *)uuid {
    NSString * msgReceiver = receiver != nil? receiver : @"";
    NSString * msgUUID = uuid != nil? uuid : @"";
    
    return @{
             @"receiverId": msgReceiver,
             @"senderId": sender,
             @"uuid": msgUUID,
             @"dateSent": [NSDate dateWithTimeIntervalSince1970:0],
             @"content": payload
             };
    
}

#pragma mark - BFTransmitterDelegate

- (void)transmitter:(BFTransmitter *)transmitter meshDidAddPacket:(NSString *)packetID {
    if (self.transitMessages[packetID] != nil) {
        [self.transitMessages removeObjectForKey:packetID];
    }
}

- (void)transmitter:(BFTransmitter *)transmitter didReachDestinationForPacket:( NSString *)packetID {
    
}

- (void)transmitter:(BFTransmitter *)transmitter meshDidStartProcessForPacket:( NSString *)packetID {
    if (self.transitMessages[packetID] != nil) {
        [self.transitMessages removeObjectForKey:packetID];
    }
}

- (void)transmitter:(BFTransmitter *)transmitter didSendDirectPacket:(NSString *)packetID {
    NSDictionary * message = self.transitMessages[packetID];
    if (message == nil) {
        return;
    }
    [self.bridge.eventDispatcher sendDeviceEventWithName:kMessageSent body:message];
    [self.transitMessages removeObjectForKey:packetID];
}

- (void)transmitter:(BFTransmitter *)transmitter didFailForPacket:(NSString *)packetID error:(NSError * _Nullable)error {
    NSDictionary * message = self.transitMessages[packetID];
    if (message == nil) {
        return;
    }
    NSDictionary * errorDict = @{
                                 @"code": @(error.code),
                                 @"description": error.localizedDescription,
                                 @"origin": message
                                 };
    [self.bridge.eventDispatcher sendDeviceEventWithName:kMessageSentError body:errorDict];
    [self.transitMessages removeObjectForKey:packetID];
}

- (void)transmitter:(BFTransmitter *)transmitter meshDidDiscardPackets:(NSArray<NSString *> *)packetIDs {
    //TODO: Implement
    
}

- (void)transmitter:(BFTransmitter *)transmitter meshDidRejectPacketBySize:(NSString *)packetID {
    //TODO: Implement
    
}

- (void)transmitter:(BFTransmitter *)transmitter
didReceiveDictionary:(NSDictionary<NSString *, id> * _Nullable) dictionary
           withData:(NSData * _Nullable)data
           fromUser:(NSString *)user
           packetID:(NSString *)packetID
          broadcast:(BOOL)broadcast
               mesh:(BOOL)mesh {
    NSDictionary * message;
    if (broadcast) {
        message = [self createMessageDictionaryWithPayload:dictionary
                                                    sender:user
                                                  receiver:nil
                                                      uuid:packetID];
        [self.bridge.eventDispatcher sendDeviceEventWithName:kBroadcastReceived body:message];
    } else {
        message = [self createMessageDictionaryWithPayload:dictionary
                                                    sender:user
                                                  receiver: transmitter.currentUser
                                                      uuid:packetID];
        [self.bridge.eventDispatcher sendDeviceEventWithName:kMessageReceived body:message];
    }
}

- (void)transmitter:(BFTransmitter *)transmitter didDetectConnectionWithUser:(NSString *)user {
    //TODO: Implement
    
}

- (void)transmitter:(BFTransmitter *)transmitter didDetectDisconnectionWithUser:(NSString *)user {
    NSDictionary * userDict = @{
                                @"userId": user
                                };
    [self.bridge.eventDispatcher sendDeviceEventWithName:kDeviceDisconnected body:userDict];
}

- (void)transmitter:(BFTransmitter *)transmitter didFailAtStartWithError:(NSError *)error {
    
    NSDictionary * errorDict = @{
                                 @"code": @(error.code),
                                 @"message": error.localizedDescription
                                 };
    [self.bridge.eventDispatcher sendDeviceEventWithName:kStartedError body:errorDict];
}

- (void)transmitter:(BFTransmitter *)transmitter didOccurEvent:(BFEvent)event description:(NSString *)description {
    if (event == BFEventStartFinished ) {
        [self.bridge.eventDispatcher sendDeviceEventWithName:kStarted body:@{}];
    } else {
        NSDictionary * eventDict = @{
                                     @"code": @(event),
                                     @"description": description
                                     };
        [self.bridge.eventDispatcher sendDeviceEventWithName:kEventOccurred body:eventDict];
    }
}

- (void)transmitter:(BFTransmitter *)transmitter didDetectSecureConnectionWithUser:(NSString *)user {
    NSDictionary * userDict = @{
                                @"userId": user
                                };
    [self.bridge.eventDispatcher sendDeviceEventWithName:kDeviceConnected body:userDict];
}

- (BOOL)transmitter:(BFTransmitter *)transmitter shouldConnectSecurelyWithUser:(NSString *)user {
    return YES;
}

- (void)transmitterNeedsInterfaceActivation:(BFTransmitter *)transmitter {
    //TODO: Implement
    
}

- (void)transmitterDidDetectAnotherInterfaceStarted:(BFTransmitter *)transmitter {
    //TODO: Implement
    
}

@end
