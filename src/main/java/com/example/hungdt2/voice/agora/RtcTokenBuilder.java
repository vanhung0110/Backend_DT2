package com.example.hungdt2.voice.agora;

import java.security.SecureRandom;
import java.time.Instant;

public class RtcTokenBuilder {

    public enum Role {
        ROLE_PUBLISHER, ROLE_SUBSCRIBER
    }

    public static String buildTokenWithUid(String appId, String appCertificate, String channelName, String uid, Role role, int expireTs) {
        AccessToken token = new AccessToken(appId, appCertificate);
        AccessToken.ServiceRtc service = new AccessToken.ServiceRtc(channelName, uid);
        service.addPrivilege(AccessToken.Privileges.kJoinChannel, expireTs);
        if (role == Role.ROLE_PUBLISHER) {
            service.addPrivilege(AccessToken.Privileges.kPublishAudioStream, expireTs);
            service.addPrivilege(AccessToken.Privileges.kPublishVideoStream, expireTs);
            service.addPrivilege(AccessToken.Privileges.kPublishDataStream, expireTs);
        }
        token.addService(service);
        // set salt and ts
        token.setSalt(new SecureRandom().nextInt());
        token.setTs((int) Instant.now().getEpochSecond());
        return token.build();
    }

    // Convenience method with numeric uid
    public static String buildTokenWithUid(String appId, String appCertificate, String channelName, int uid, Role role, int expireTs) {
        return buildTokenWithUid(appId, appCertificate, channelName, String.valueOf(uid), role, expireTs);
    }
}
