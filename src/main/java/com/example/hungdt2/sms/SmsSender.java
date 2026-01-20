package com.example.hungdt2.sms;

public interface SmsSender {
    void sendSms(String phone, String message);
}