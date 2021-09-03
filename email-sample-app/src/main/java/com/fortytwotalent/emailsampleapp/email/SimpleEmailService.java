package com.fortytwotalent.emailsampleapp.email;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

@Service
@RequiredArgsConstructor
class SimpleEmailService implements EmailService {

    private final JavaMailSender javaMailSender;
    private final ResourceLoader resourceLoader;
    private final TemplateEngine templateEngine;

    public void sendEmail(String to, String from, String subject, String content) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.setText(content);

        javaMailSender.send(mail);
    }

    public void sendEmailWithAttachment(String to, String from, String subject, String content, String pathToAttachment) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setFrom(from);
        helper.setSubject(subject);
        helper.setText(content);

        Resource file = resourceLoader.getResource(pathToAttachment);
        helper.addAttachment(file.getFilename(), file);

        javaMailSender.send(message);
    }

    public void sendHtmlEmailWithInlineImage(String to, String from, String subject, Locale locale, String inlineImage) throws MessagingException, IOException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setFrom(from);
        helper.setSubject(subject);

        Resource file = resourceLoader.getResource(inlineImage);

        final Context ctx = new Context(locale);
        ctx.setVariable("name", to);
        ctx.setVariable("subscriptionDate", new Date());
        ctx.setVariable("frameworks", Arrays.asList("Micronaut", "Quarkus", "Spring Boot"));
        ctx.setVariable("imageResourceName", file.getFilename()); // so that we can reference it from HTML

        // Create the HTML body using Thymeleaf
        String htmlContent = templateEngine.process("frameworks", ctx);
        helper.setText(htmlContent, true);

        // Add the inline image, referenced from the HTML code as "cid:${imageResourceName}"
        helper.addInline(file.getFilename(), file, Files.probeContentType(file.getFile().toPath()));

        javaMailSender.send(message);
    }
}



