# blog-sending-email-with-spring-boot

[![Java CI with Maven](https://github.com/42talents/blog-sending-emails-with-spring-boot/actions/workflows/main.yml/badge.svg)](https://github.com/42talents/blog-sending-emails-with-spring-boot/actions/workflows/main.yml)

In this small tutorial, we walk through the possibilities of sending out emails with Spring Boot applications. We are leveraging only the `spring-boot-starter-mail`. We are not discussing the direct usage of [JavaMail](https://javaee.github.io/javamail/) or [Apache Commons Email](https://commons.apache.org/proper/commons-email/).

## Setup JavaMailSender with Spring Boot

Spring Boot configures us behind the scenes the `org.springframework.mail.javamail.JavaMailSender` abstraction. To define the properties for `JavaMailSender` we need to configure a few Spring Boot properties:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<login user to smtp server>
spring.mail.password=<login password to smtp server>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Some servers require authentication or/and a TLS connection. Basically, all properties are optional, but Spring Boot might assume some defaults like, e.g. the port. If you use GMail, you might need to follow [their instructions](https://support.google.com/mail/answer/7126229?hl=en#zippy=%2Cstep-change-smtp-other-settings-in-your-email-client).

## Sending Text Emails

Sending emails can be easily implemented in a service. We would use `org.springframework.mail.SimpleMailMessage` like in the code snippet below. The `SimpleMailMessage` allows you to send _text messages_ and control all other parameters of an Email.

```java
@Service
@RequiredArgsConstructor
class SimpleEmailService implements EmailService {

    private final JavaMailSender javaMailSender;

    public void sendEmail(String to, String from, String subject, String content) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.setText(content);

        javaMailSender.send(mail);
    }
}
```

## Sending Text Emails with Attachments

Sending emails with attachments needs more work since we need to load the file from somewhere, and Mimetypes have to be used.

```java
@Service
@RequiredArgsConstructor
class SimpleEmailService implements EmailService {

    private final ResourceLoader resourceLoader;
    private final JavaMailSender javaMailSender;

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
}
```

## Sending HTML Emails with Inline Images

These days we want to send also HTML messages. Therefore we need to set up a template engine like [Thymeleaf](https://www.thymeleaf.org). This would allow us to use SpEL expressions, flow control (iteration, conditionals, etc.), utility functions, i18n and natural templating. Let's add the `spring-boot-starter-thymeleaf` and tweak it with a few properties if needed. For the sake of the demo, we are changing the default templates path.

```properties
spring.thymeleaf.prefix=classpath:/templates-emails/
spring.thymeleaf.suffix=.html
```

Instead of sending text, we need to prepare the HTML output via the templating engine.

```java
@Service
@RequiredArgsConstructor
class SimpleEmailService implements EmailService {

    private final JavaMailSender javaMailSender;
    private final ResourceLoader resourceLoader;
    private final TemplateEngine templateEngine;

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
```

Thymeleaf extends HTML5 with a namespace that lets us do some of the magic. Try to find all the topics I mentioned above: i18n, conditionals and so on — more info in the official [Thymeleaf Documentation](https://www.thymeleaf.org/).

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
  <head>
    <title th:remove="all">Template for HTML email with inline image</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  </head>
  <body>
    <p th:text="#{greeting(${name})}">Hello, John Doe!</p>
    <p th:if="${name.length() > 10}">
      Wow! You've got a long name (more than 10 chars). This must be your email
      address!
    </p>
    <p>
      You have been successfully subscribed to the
      <b>Java Frameworks Newsletter</b> on
      <span th:text="${#dates.format(subscriptionDate)}">31-12-2021</span>
    </p>
    <p>Your favourite Java Frameworks are:</p>
    <ul th:remove="all-but-first">
      <li th:each="framework : ${frameworks}" th:text="${framework}">
        Spring Boot
      </li>
      <li>Quarkus</li>
      <li>Micronaut</li>
    </ul>
    <p>You can find <b>your inlined image</b> just below this text.</p>
    <p>
      <img src="SpringBoot.png" th:src="|cid:${imageResourceName}|" />
    </p>
    <p>
      Regards, <br />
      <em>Your Java Frameworks Newsletter Team</em>
    </p>
  </body>
</html>
```

## Checking the Emails Visually

To see how the email would look, you could use the Debugger and try to imagine by parsing the HTML in your mind or using a simple project like [MailHog](https://github.com/mailhog/MailHog), which is like GMail but without login for local testing.

![MailHog][1]

To set up MailHog, we can run it with a docker container. We can use the official container from DockerHub. Make sure you change your properties in your `application.properties` and access it afterwards via `http://localhost:8025/`.

```properties
spring.mail.host=localhost
spring.mail.port=1025
```

```bash
docker run --restart always --name mailhog -p 1025:1025 -p 8025:8025 -d mailhog/mailhog:latest
```

Unfortunately, the inline images are not shown properly! You might download the message to your Desktop and open it with another Email client. This way, it looks way better ;)

## Integration Testing with GreenMail

[GreenMail](https://greenmail-mail-test.github.io/greenmail/) extends your `@SpringBootTest` with a JUnit5 extension and runs an SMTP Server (or others) locally. Via API, you can access GreenMail and read the messages from the server, which did not leave your computer.

```java
@SpringBootTest
class EmailSampleAppApplicationTests {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);
}
```

GreenMail, by default, takes an offset by 3000 for all the ports. Therefore make sure your port property is adjusted to `spring.mail.port=3025`.

Your implemented integration test could now look like the following example. All other tests for the mentioned use cases you find in the [GitHub repository](https://github.com/42talents/blog-sending-emails-with-spring-boot/blob/master/email-sample-app/src/test/java/com/fortytwotalent/emailsampleapp/EmailSampleAppApplicationTests.java). There, there are some gems!

```java
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
```

The complete source code is available on our [GitHub Repository](https://github.com/42talents/blog-sending-emails-with-spring-boot).

If you are interested to learn more about Spring and Spring Boot, [get in touch and have a look at our training courses!](https://42talents.com/en/training/in-house/Spring-Core/)

[1]: MailHog.png
