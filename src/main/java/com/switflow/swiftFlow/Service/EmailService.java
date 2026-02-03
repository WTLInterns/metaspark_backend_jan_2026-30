package com.switflow.swiftFlow.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailTemplateService templateService;

    @Async
    public void sendLoginDetails(String toEmail, String fullName, String password, String department) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("worldtriplink30@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("🚀 Welcome to MetaSpark ERP - Your Login Credentials");
            
            String htmlContent = templateService.generateLoginDetailsEmail(fullName, toEmail, password, department);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Login details email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send login details email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("worldtriplink30@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("🔐 MetaSpark ERP - Password Reset Request");
            
            String htmlContent = templateService.generatePasswordResetEmail(fullName, resetToken);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Password reset email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("worldtriplink30@gmail.com");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            System.out.println("Simple email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send simple email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendUserCreationNotification(String adminEmail, String newUserFullName, String newUserEmail, String department) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("worldtriplink30@gmail.com");
            helper.setTo(adminEmail);
            helper.setSubject("✅ New User Created - MetaSpark ERP");
            
            String htmlContent = generateAdminNotificationEmail(newUserFullName, newUserEmail, department);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Admin notification email sent successfully to " + adminEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send admin notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateAdminNotificationEmail(String fullName, String email, String department) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>New User Created - MetaSpark ERP</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "            background-color: #f4f7fa;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(135deg, #27ae60 0%, #2ecc71 100%);\n" +
                "            color: white;\n" +
                "            padding: 20px;\n" +
                "            text-align: center;\n" +
                "            border-radius: 10px 10px 0 0;\n" +
                "        }\n" +
                "        .content {\n" +
                "            background-color: white;\n" +
                "            padding: 30px;\n" +
                "            border-radius: 0 0 10px 10px;\n" +
                "            box-shadow: 0 4px 15px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .user-info {\n" +
                "            background-color: #f8f9fa;\n" +
                "            border-left: 4px solid #27ae60;\n" +
                "            padding: 20px;\n" +
                "            margin: 20px 0;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .info-item {\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .label {\n" +
                "            font-weight: bold;\n" +
                "            color: #2c3e50;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <h2>✅ New User Successfully Created</h2>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "        <p>A new user has been successfully added to the MetaSpark ERP system:</p>\n" +
                "        <div class=\"user-info\">\n" +
                "            <div class=\"info-item\"><span class=\"label\">Name:</span> " + fullName + "</div>\n" +
                "            <div class=\"info-item\"><span class=\"label\">Email:</span> " + email + "</div>\n" +
                "            <div class=\"info-item\"><span class=\"label\">Department:</span> " + department + "</div>\n" +
                "        </div>\n" +
                "        <p>The user has been sent their login credentials via email and can now access the system.</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
