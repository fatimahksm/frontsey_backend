package com.dbwb.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender. BR-NOT-002: all platform email
 * notification categories are mandatory in the MVP - there is no per-category
 * opt-out, so this service has no "should I send this" branching logic.
 * The concrete SMTP provider (TBD-006) is swapped via application.yml only.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            // Email delivery must never fail the business transaction that triggered it
            // (e.g. a payment success). Failures are logged for support follow-up instead.
            log.error("Failed to send email to {} with subject '{}'", to, subject, ex);
        }
    }
}
