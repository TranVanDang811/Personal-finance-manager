package com.pfm.personalfinancemanager.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String fullName, String resetToken);
}

