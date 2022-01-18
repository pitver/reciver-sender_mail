package ru.vershinin.service;

import org.apache.commons.mail.util.MimeMessageParser;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailSenderImpl implements MailSender {
    private final JavaMailSender javaMailSender;

    public MailSenderImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendMail(MimeMessageParser mimeMessageParser) throws Exception {


        var mes = new SimpleMailMessage();
        mes.setTo("test@yandex.ru");
        mes.setSubject(mimeMessageParser.getSubject());
        mes.setText(mimeMessageParser.getPlainContent());

        mes.setFrom("text@yandex.ru");
        javaMailSender.send(mes);

    }
}
