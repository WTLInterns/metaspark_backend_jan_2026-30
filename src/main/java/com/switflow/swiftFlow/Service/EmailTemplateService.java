package com.switflow.swiftFlow.Service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String generateLoginDetailsEmail(String fullName, String email, String password, String department) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Welcome to MetaSpark ERP - Your Login Credentials</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 0;\n" +
                "            background-color: #f4f7fa;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            padding: 30px 20px;\n" +
                "            text-align: center;\n" +
                "            border-radius: 10px 10px 0 0;\n" +
                "        }\n" +
                "        .logo {\n" +
                "            font-size: 28px;\n" +
                "            font-weight: bold;\n" +
                "            margin-bottom: 10px;\n" +
                "            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);\n" +
                "        }\n" +
                "        .tagline {\n" +
                "            font-size: 16px;\n" +
                "            opacity: 0.9;\n" +
                "        }\n" +
                "        .content {\n" +
                "            background-color: white;\n" +
                "            padding: 40px 30px;\n" +
                "            border-radius: 0 0 10px 10px;\n" +
                "            box-shadow: 0 4px 15px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .welcome {\n" +
                "            font-size: 24px;\n" +
                "            color: #2c3e50;\n" +
                "            margin-bottom: 20px;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        .message {\n" +
                "            font-size: 16px;\n" +
                "            color: #555;\n" +
                "            margin-bottom: 30px;\n" +
                "            line-height: 1.8;\n" +
                "        }\n" +
                "        .credentials {\n" +
                "            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);\n" +
                "            border-radius: 8px;\n" +
                "            padding: 25px;\n" +
                "            margin: 30px 0;\n" +
                "            border-left: 5px solid #667eea;\n" +
                "        }\n" +
                "        .credential-item {\n" +
                "            margin: 15px 0;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "        }\n" +
                "        .credential-label {\n" +
                "            font-weight: 600;\n" +
                "            color: #2c3e50;\n" +
                "            min-width: 100px;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .credential-value {\n" +
                "            background-color: white;\n" +
                "            padding: 12px 15px;\n" +
                "            border-radius: 5px;\n" +
                "            font-family: 'Courier New', monospace;\n" +
                "            font-size: 15px;\n" +
                "            color: #e74c3c;\n" +
                "            font-weight: bold;\n" +
                "            border: 1px solid #ddd;\n" +
                "            flex: 1;\n" +
                "            margin-left: 15px;\n" +
                "        }\n" +
                "        .department-badge {\n" +
                "            display: inline-block;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            padding: 8px 16px;\n" +
                "            border-radius: 20px;\n" +
                "            font-size: 14px;\n" +
                "            font-weight: 600;\n" +
                "            margin-top: 10px;\n" +
                "            text-transform: uppercase;\n" +
                "            letter-spacing: 1px;\n" +
                "        }\n" +
                "        .instructions {\n" +
                "            background-color: #e8f4fd;\n" +
                "            border: 1px solid #bee5eb;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            margin: 25px 0;\n" +
                "        }\n" +
                "        .instructions h4 {\n" +
                "            color: #0c5460;\n" +
                "            margin-top: 0;\n" +
                "            margin-bottom: 15px;\n" +
                "            font-size: 18px;\n" +
                "        }\n" +
                "        .instructions ol {\n" +
                "            margin: 0;\n" +
                "            padding-left: 20px;\n" +
                "        }\n" +
                "        .instructions li {\n" +
                "            margin: 10px 0;\n" +
                "            color: #0c5460;\n" +
                "        }\n" +
                "        .security-note {\n" +
                "            background-color: #fff3cd;\n" +
                "            border: 1px solid #ffeaa7;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            margin: 25px 0;\n" +
                "        }\n" +
                "        .security-note h4 {\n" +
                "            color: #856404;\n" +
                "            margin-top: 0;\n" +
                "            margin-bottom: 10px;\n" +
                "            font-size: 16px;\n" +
                "        }\n" +
                "        .security-note p {\n" +
                "            margin: 0;\n" +
                "            color: #856404;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            text-align: center;\n" +
                "            padding: 30px 20px;\n" +
                "            background-color: #2c3e50;\n" +
                "            color: white;\n" +
                "            border-radius: 10px;\n" +
                "            margin-top: 30px;\n" +
                "        }\n" +
                "        .footer p {\n" +
                "            margin: 5px 0;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .footer .company-name {\n" +
                "            font-size: 18px;\n" +
                "            font-weight: bold;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .footer .contact {\n" +
                "            font-size: 12px;\n" +
                "            opacity: 0.8;\n" +
                "        }\n" +
                "        .login-button {\n" +
                "            display: inline-block;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            color: white;\n" +
                "            padding: 15px 30px;\n" +
                "            text-decoration: none;\n" +
                "            border-radius: 25px;\n" +
                "            font-weight: 600;\n" +
                "            font-size: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            text-align: center;\n" +
                "            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "        .login-button:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <div class=\"logo\">🚀 MetaSpark ERP</div>\n" +
                "        <div class=\"tagline\">Enterprise Resource Management System</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"content\">\n" +
                "        <h1 class=\"welcome\">Welcome aboard, " + fullName + "! 👋</h1>\n" +
                "        \n" +
                "        <div class=\"message\">\n" +
                "            <p>We're excited to have you join our team at MetaSpark! Your account has been successfully created and you're all set to get started with our powerful ERP system.</p>\n" +
                "            <p>Below are your login credentials that will give you access to the system based on your role as <strong>" + department + "</strong>.</p>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"credentials\">\n" +
                "            <h3 style=\"color: #2c3e50; margin-top: 0; margin-bottom: 20px;\">🔐 Your Login Credentials</h3>\n" +
                "            \n" +
                "            <div class=\"credential-item\">\n" +
                "                <span class=\"credential-label\">📧 Email:</span>\n" +
                "                <span class=\"credential-value\">" + email + "</span>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"credential-item\">\n" +
                "                <span class=\"credential-label\">🔑 Password:</span>\n" +
                "                <span class=\"credential-value\">" + password + "</span>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"credential-item\">\n" +
                "                <span class=\"credential-label\">👤 Role:</span>\n" +
                "                <span class=\"department-badge\">" + department + "</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div style=\"text-align: center;\">\n" +
                "            <a href=\"http://localhost:3000/login\" class=\"login-button\">🚀 Login to MetaSpark ERP</a>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"instructions\">\n" +
                "            <h4>📋 Getting Started Instructions:</h4>\n" +
                "            <ol>\n" +
                "                <li>Visit our login portal using the button above or go to: <strong>http://localhost:3000/login</strong></li>\n" +
                "                <li>Enter your email address and the password provided above</li>\n" +
                "                <li>You'll be automatically redirected to your department dashboard based on your role</li>\n" +
                "                <li>Complete your profile by updating your personal information if needed</li>\n" +
                "                <li>Explore the features available to your role and start managing your tasks efficiently</li>\n" +
                "            </ol>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"security-note\">\n" +
                "            <h4>🔒 Security Notice:</h4>\n" +
                "            <p>For your security, please change your password after your first login. Keep your credentials confidential and never share them with anyone. If you suspect any unauthorized access, please contact our IT support immediately.</p>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"message\" style=\"margin-top: 30px;\">\n" +
                "            <p><strong>Need Help?</strong> Our support team is here to assist you. Feel free to reach out if you have any questions or encounter any issues during the login process.</p>\n" +
                "            <p>We look forward to working with you and helping you make the most of MetaSpark ERP!</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"footer\">\n" +
                "        <div class=\"company-name\">MetaSpark ERP Solutions</div>\n" +
                "        <p>📞 Support: support@metaspark.com | 🌐 www.metaspark.com</p>\n" +
                "        <p class=\"contact\">© 2026 MetaSpark. All rights reserved. | Enterprise Resource Management</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    public String generatePasswordResetEmail(String fullName, String resetToken) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>MetaSpark ERP - Password Reset Request</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 0;\n" +
                "            background-color: #f4f7fa;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);\n" +
                "            color: white;\n" +
                "            padding: 30px 20px;\n" +
                "            text-align: center;\n" +
                "            border-radius: 10px 10px 0 0;\n" +
                "        }\n" +
                "        .content {\n" +
                "            background-color: white;\n" +
                "            padding: 40px 30px;\n" +
                "            border-radius: 0 0 10px 10px;\n" +
                "            box-shadow: 0 4px 15px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .reset-button {\n" +
                "            display: inline-block;\n" +
                "            background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);\n" +
                "            color: white;\n" +
                "            padding: 15px 30px;\n" +
                "            text-decoration: none;\n" +
                "            border-radius: 25px;\n" +
                "            font-weight: 600;\n" +
                "            font-size: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <div class=\"logo\">🔐 MetaSpark ERP</div>\n" +
                "        <div class=\"tagline\">Password Reset Request</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"content\">\n" +
                "        <h1>Password Reset Request</h1>\n" +
                "        <p>Hi " + fullName + ",</p>\n" +
                "        <p>We received a request to reset your password. Click the button below to reset it:</p>\n" +
                "        <div style=\"text-align: center;\">\n" +
                "            <a href=\"http://localhost:3000/reset-password?token=" + resetToken + "\" class=\"reset-button\">Reset Password</a>\n" +
                "        </div>\n" +
                "        <p><strong>Note:</strong> This link will expire in 24 hours for security reasons.</p>\n" +
                "        <p>If you didn't request this password reset, please ignore this email or contact our support team.</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
