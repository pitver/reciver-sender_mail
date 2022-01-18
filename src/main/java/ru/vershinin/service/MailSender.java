package ru.vershinin.service;

import org.apache.commons.mail.util.MimeMessageParser;

public interface MailSender {

    public void sendMail(MimeMessageParser mimeMessageParser) throws Exception;
}
