package com.example.hungdt2.voice.agora;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public class AccessToken {

    public static class Privileges {
        public static final int kJoinChannel = 1;
        public static final int kPublishAudioStream = 2;
        public static final int kPublishVideoStream = 3;
        public static final int kPublishDataStream = 4;
    }

    private final String appId;
    private final String appCertificate;
    private int salt = 0;
    private int ts = 0;
    private final Map<Integer, Service> services = new HashMap<>();

    public AccessToken(String appId, String appCertificate) {
        this.appId = appId;
        this.appCertificate = appCertificate;
    }

    public void setSalt(int salt) { this.salt = salt; }
    public void setTs(int ts) { this.ts = ts; }

    public void addService(Service s) { services.put(s.getServiceType(), s); }

    public String build() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            // write salt (int32)
            dos.writeInt(salt);
            // write ts (int32)
            dos.writeInt(ts);
            // write number of services (int16)
            dos.writeShort(services.size());
            for (Service s : services.values()) {
                s.pack(dos);
            }
            dos.flush();
            byte[] content = baos.toByteArray();

            // signature = hmacsha256(appCertificate, content)
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appCertificate.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(content);

            ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
            finalBaos.write(signature);
            finalBaos.write(content);
            byte[] finalBytes = finalBaos.toByteArray();
            String encoded = Base64.getEncoder().encodeToString(finalBytes);
            return "007" + appId + encoded;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build token", e);
        }
    }

    public static abstract class Service {
        abstract int getServiceType();
        abstract void pack(DataOutputStream dos) throws java.io.IOException;
    }

    public static class ServiceRtc extends Service {
        private final String channel;
        private final String uid;
        private final Map<Integer, Integer> privileges = new HashMap<>();

        public ServiceRtc(String channel, String uid) {
            this.channel = channel == null ? "" : channel;
            this.uid = uid == null ? "" : uid;
        }

        public void addPrivilege(int privilege, int expireTs) { privileges.put(privilege, expireTs); }

        @Override
        int getServiceType() { return 1; }

        @Override
        void pack(DataOutputStream dos) throws java.io.IOException {
            // service type (uint16)
            dos.writeShort(getServiceType());
            // pack channel (string with length prefix)
            writeString(dos, channel);
            // pack uid (string with length prefix)
            writeString(dos, uid);
            // pack privilege map size (uint16)
            dos.writeShort(privileges.size());
            for (Map.Entry<Integer, Integer> e : privileges.entrySet()) {
                dos.writeShort(e.getKey()); // privilege id (uint16)
                dos.writeInt(e.getValue()); // expireTs (uint32)
            }
        }

        private void writeString(DataOutputStream dos, String s) throws java.io.IOException {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(b.length);
            dos.write(b);
        }
    }
}
