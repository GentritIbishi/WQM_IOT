package com.gentritibishi.waterqualitymonitoringbackend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    // Method to send HTML email
    public void sendHtmlEmail(String toEmail, String subject, String body) throws MessagingException {
        // Create a MimeMessage to send HTML
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        // Set the email details
        helper.setFrom("gentritibishi@gmail.com");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, true);  // 'true' indicates this is an HTML email

        // Send the email
        mailSender.send(mimeMessage);
        System.out.println("HTML Mail Sent...");
    }

}
