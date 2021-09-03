package com.fortytwotalent.emailsampleapp;

import com.fortytwotalent.emailsampleapp.email.EmailService;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import javax.mail.BodyPart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("email")
class EmailSampleAppApplicationTests {

    private static final String FROM = "hello@42talents.com";
    private static final String TO = "patrick.baumgartner@42talents.com";
    private static final String SUBJECT = "Subject";
    private static final String CONTENT = "Content";
    private static final String FILE = "classpath:SpringBoot.png";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);
    @Autowired
    private EmailService emailService;

    @Test
    public void sendEmail() throws Exception {
        emailService.sendEmail(TO, FROM, SUBJECT, CONTENT);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages.length).isEqualTo(1);

        // Check Message
        MimeMessage receivedMessage = receivedMessages[0];
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(TO);
        assertThat(receivedMessage.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(receivedMessage.getSubject()).isEqualTo(SUBJECT);
        assertThat(receivedMessage.getContent().toString()).contains(CONTENT);
        // Or
        assertThat(CONTENT).isEqualTo(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));
    }

    @Test
    public void sendEmailWithAttachments() throws Exception {
        emailService.sendEmailWithAttachment(TO, FROM, SUBJECT, CONTENT, FILE);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages.length).isEqualTo(1);

        // Check Message
        MimeMessage receivedMessage = receivedMessages[0];
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(TO);
        assertThat(receivedMessage.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(receivedMessage.getSubject()).isEqualTo(SUBJECT);

        assertThat(((MimeMultipart) receivedMessage.getContent()).getCount()).isEqualTo(2);

        BodyPart body = ((MimeMultipart) receivedMessage.getContent()).getBodyPart(0);
        assertThat(body.getContentType()).contains("multipart/related");

        // Check Text Body
        BodyPart message = ((MimeMultipart) body.getContent()).getBodyPart(0);
        assertThat(message.getContentType()).contains("text/plain");
        assertThat(((MimeMultipart) body.getContent()).getBodyPart(0).getContent().toString()).contains(CONTENT);

        // Check Attached Image
        BodyPart attachment = ((MimeMultipart) receivedMessage.getContent()).getBodyPart(1);
        assertThat(attachment.getContentType()).contains("image/png; name=SpringBoot.png");
        assertThat(attachment.getDisposition()).contains(Part.ATTACHMENT);

        Resource resource = new DefaultResourceLoader().getResource(FILE);
        assertThat(attachment.getInputStream()).hasSameContentAs(resource.getInputStream());
    }


    @Test
    public void sendHtmlEmailWithAttachments() throws Exception {
        emailService.sendHtmlEmailWithInlineImage(TO, FROM, SUBJECT, Locale.ENGLISH, FILE);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages.length).isEqualTo(1);

        // Check Message
        MimeMessage receivedMessage = receivedMessages[0];
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(TO);
        assertThat(receivedMessage.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(receivedMessage.getSubject()).isEqualTo(SUBJECT);

        assertThat(((MimeMultipart) receivedMessage.getContent()).getCount()).isEqualTo(1);

        BodyPart body = ((MimeMultipart) receivedMessage.getContent()).getBodyPart(0);
        assertThat(body.getContentType()).contains("multipart/related");

        // Check Text Body
        BodyPart message = ((MimeMultipart) body.getContent()).getBodyPart(0);
        assertThat(message.getContentType()).contains("text/html");

        String htmlMessage = message.getContent().toString();
        assertThat(Jsoup.parse(htmlMessage).text()).contains("Hi, " + TO);

        // Check Inlined Image
        BodyPart attachment = ((MimeMultipart) body.getContent()).getBodyPart(1);
        assertThat(attachment.getContentType()).contains("image/png");
        assertThat(attachment.getDisposition()).contains(Part.INLINE);

        Resource resource = new DefaultResourceLoader().getResource(FILE);
        assertThat(attachment.getInputStream()).hasSameContentAs(resource.getInputStream());
    }

}
