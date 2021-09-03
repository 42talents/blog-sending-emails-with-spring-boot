package com.fortytwotalent.emailsampleapp.email;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Locale;

public interface EmailService {

    void sendEmail(String to, String from, String subject, String content);

    void sendEmailWithAttachment(String to, String from, String subject, String content, String pathToAttachment) throws MessagingException;

    void sendHtmlEmailWithInlineImage(String to, String from, String subject, Locale locale, String inlineImage) throws MessagingException, IOException;
}
