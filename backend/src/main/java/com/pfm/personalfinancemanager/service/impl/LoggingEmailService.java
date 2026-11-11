package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        // Production deployment can wire JavaMailSender / SMTP provider here without changing callers.
        log.info("Sending password reset email to {} ({}). Reset token generated (valid for 15 minutes): {}",
                toEmail, fullName, resetToken);
    }
}

