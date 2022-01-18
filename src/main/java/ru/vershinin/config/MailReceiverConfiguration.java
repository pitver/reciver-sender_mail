package ru.vershinin.config;

import ru.vershinin.service.ReceiveMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.messaging.Message;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * аннотация @EnableIntegration обозначает этот класс как конфигурацию Spring Integration.
 */
@Configuration
@EnableIntegration
public class MailReceiverConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MailReceiverConfiguration.class);

    private final ReceiveMailService receiveMailService;

    public MailReceiverConfiguration(ReceiveMailService receiveMailService) {
        this.receiveMailService = receiveMailService;
    }

    /**
     * Service Activator -это тип конечной точки для подключения любого объекта, управляемого Spring, к входному каналу, чтобы он мог играть роль службы.
     *
     * @param message -  по сути, контейнер ключ-значение, который можно использовать для передачи метаданных,
     *                как определено в классе org.springframework.integration.MessageHeaders.
     *                Полезная нагрузка сообщения, представляющая собой фактические данные, которые представляют ценность для передачи —
     *                в нашем случае "email" является полезной нагрузкой.
     */
    @ServiceActivator(inputChannel = "receiveEmailChannel")
    public void receive(Message<?> message) {
        receiveMailService.handleReceivedMail((MimeMessage) message.getPayload());
    }

    /**
     * Канал, который вызывает одного подписчика для каждого отправленного сообщения. Вызов будет происходить в потоке отправителя
     * Ключевой особенностью DirectChannelявляется то, что, хотя он работает по модели «публикация-подписка»,
     * он отправляет каждое полученное сообщение только одному из подписчиков в циклическом режиме.
     * Таким образом, это комбинация моделей публикации-подписки и двухточечной передачи
     *
     * @return - DirectChannel
     */
    @Bean("receiveEmailChannel")
    public DirectChannel defaultChannel() {
        DirectChannel directChannel = new DirectChannel();
        directChannel.setDatatypes(javax.mail.internet.MimeMessage.class);
        return directChannel;
    }

    /**
     * Входящий адаптер ,  используются для доставки сообщений из внешней системы.
     * Конфигурация входящего адаптера состоит из:
     * Аннотация @InboundChannelAdapter , помечающая конфигурацию bean-компонента как адаптер — мы настраиваем канал,
     * на который адаптер будет передавать свои сообщения , и poller , компонент, который помогает адаптеру опрашивать настроенную папку на указанный интервал
     * Стандартный класс конфигурации Java Spring, который возвращает FileReadingMessageSource,
     * реализацию класса Spring Integration, которая обрабатывает опрос .
     */
    @Bean()
    @InboundChannelAdapter(
            channel = "receiveEmailChannel",
            poller = @Poller(fixedDelay = "5000", taskExecutor = "asyncTaskExecutor")
    )
    public MailReceivingMessageSource mailMessageSource(MailReceiver mailReceiver) {
        MailReceivingMessageSource mailReceivingMessageSource = new MailReceivingMessageSource(mailReceiver);
        return mailReceivingMessageSource;
    }

    /**
     * Стандартный класс для описания свойств
     *
     * @return - Properties
     */
    @Bean
    public Properties properties() {
        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.put("mail.imap.socketFactory.fallback", false);
        javaMailProperties.put("mail.store.protocol", "imaps");
        javaMailProperties.put("mail.debug", true);

        return javaMailProperties;
    }


    /**
     * Серверный компонент для получения сообщений электронной почты с использованием JavaMail.
     * Требует transportURI, storeURI и monitoringStrategy должен быть установлен в дополнение к messageFactory и messageReceiver, требуемому базовым классом.
     *
     * @param storeUrl - адрес почтового ящика в формате username.com:password@host:port/NameFolder
     * @return - MailReceiver
     */
    @Bean
    public MailReceiver imapMailReceiver(@Value("imaps://${mail.imap.username}:${mail.imap.password}@${mail.imap.host}:${mail.imap.port}/INBOX") String storeUrl) {
        log.info("IMAP connection url: {}", storeUrl);

        ImapMailReceiver imapMailReceiver = new ImapMailReceiver(storeUrl);
        imapMailReceiver.setShouldMarkMessagesAsRead(true);
        imapMailReceiver.setShouldDeleteMessages(false);
        imapMailReceiver.setMaxFetchSize(10);

        imapMailReceiver.setJavaMailProperties(properties());

        return imapMailReceiver;
    }

    /**
     * Добавляем в текущую сессию Authenticator
     * PasswordAuthentication является держателем данных, который используется Authenticator. Это просто хранилище для имени пользователя и пароля
     *
     * @param username - username
     * @param password - password
     * @return - Session
     */
    @Bean
    public Session session(@Value("${mail.imap.username}") String username, @Value("${mail.imap.password}") String password) {

        return Session.getDefaultInstance(properties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }


}
