package ru.vershinin.service;

import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;

@Service
public class ReceiveMailServiceImpl implements ReceiveMailService {

    private static final Logger log = LoggerFactory.getLogger(ReceiveMailServiceImpl.class);

    private static final String DOWNLOADED_MAIL_FOLDER = "архив";

    private final MailSenderImpl mailSenderImpl;


    public ReceiveMailServiceImpl(MailSenderImpl mailSenderImpl) {
        this.mailSenderImpl = mailSenderImpl;
    }


    /**
     * метод проходит по всем письмам в исходной папке и ищет не прочитанные.
     * Внимание! письма после прочтения необходимо перещать в другую папку, а текущей удалять,
     * так как метод проходит по всем сообщениям в исходной папке
     *
     * @param receivedMessage - MimeMessage
     */
    @Override
    public void handleReceivedMail(MimeMessage receivedMessage) {
        try {

            Folder folder = receivedMessage.getFolder();
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.getMessages();
            fetchMessagesInFolder(folder, messages);

            Arrays.stream(messages).filter(message -> {
                MimeMessage currentMessage = (MimeMessage) message;
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            }).forEach(this::extractMail);

            copyMailToDownloadedFolder(receivedMessage, folder);

            folder.close(true);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * FetchProfile для перечисления атрибутов сообщений, которые они хотят предварительно получить с сервера для диапазона сообщений.
     * <p>
     * Сообщения, полученные из папки, представляют собой легковесные объекты, которые обычно начинаются как пустые ссылки на фактические сообщения.
     * Такой объект Message заполняется «по запросу», когда для этого конкретного сообщения вызываются соответствующие методы get*().
     * Некоторые серверные протоколы доступа к сообщениям (например, IMAP) позволяют пакетно извлекать атрибуты сообщений для
     * диапазона сообщений в одном запросе. Клиенты, которые хотят использовать атрибуты сообщений для диапазона сообщений (например,
     * для отображения заголовков верхнего уровня в списке заголовков), могут захотеть использовать оптимизацию, предоставляемую такими серверами.
     * Позволяет FetchProfile клиенту указать это желание серверу.
     *
     * @throws MessagingException
     */
    private void fetchMessagesInFolder(Folder folder, Message[] messages) throws MessagingException {
        FetchProfile contentsProfile = new FetchProfile();
        contentsProfile.add(FetchProfile.Item.ENVELOPE);
        contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
        contentsProfile.add(FetchProfile.Item.FLAGS);
        contentsProfile.add(FetchProfile.Item.SIZE);
        folder.fetch(messages, contentsProfile);
    }

    /**
     * Метод копирует сообщение из исходной папки в назначенную
     *
     * @param mimeMessage  - сообщение веб-службы с вложениями MIME
     * @param folder-папка куда необходимо скопировать сообщение
     * @throws MessagingException
     */
    private void copyMailToDownloadedFolder(MimeMessage mimeMessage, Folder folder) throws MessagingException {
        Store store = folder.getStore();

        Folder downloadedMailFolder = store.getFolder(DOWNLOADED_MAIL_FOLDER);
        if (downloadedMailFolder.exists()) {
            downloadedMailFolder.open(Folder.READ_WRITE);
            downloadedMailFolder.appendMessages(new MimeMessage[]{mimeMessage});
            downloadedMailFolder.close();
        }
    }

    private void extractMail(Message message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message;
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(messageToExtract).parse();

            showMailContent(mimeMessageParser, message);

            mailSenderImpl.sendMail(mimeMessageParser);


            // To delete downloaded email
            messageToExtract.setFlag(Flags.Flag.DELETED, true);


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void showMailContent(MimeMessageParser mimeMessageParser, Message message) throws Exception {
        log.debug("From: {} to: {} | Subject: {}", mimeMessageParser.getFrom(), mimeMessageParser.getTo(), mimeMessageParser.getSubject());
        log.debug("Mail content: {}", mimeMessageParser.getPlainContent());
        log.debug("Mail text: {}", message.getContent());
        log.debug("Mail text1: {}", mimeMessageParser.getHtmlContent());
    }


}
