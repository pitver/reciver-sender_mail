package ru.vershinin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**Класс который реализовывает интерфейс, который должен быть реализован в @Configuration классами, аннотированными @EnableAsync,
 * которые хотят настроить Executorэкземпляр, используемый при обработке вызовов асинхронного метода, или
 * AsyncUncaughtExceptionHandler экземпляр, используемый для обработки исключения, выданного из асинхронного метода, с void возвращаемым типом.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    private final TaskExecutionProperties taskExecutionProperties;

    public AsyncConfiguration(TaskExecutionProperties taskExecutionProperties) {
        this.taskExecutionProperties = taskExecutionProperties;
    }

    /**
     * JavaBean, который обеспечивает абстракцию вокруг экземпляра java.util.concurrent.ThreadPoolExecutor и
     * предоставляет его как Spring org.springframework.core.task.TaskExecutor .
     * Кроме того, он легко настраивается с помощью свойств corePoolSize, maxPoolSize, queueCapacity, allowCoreThreadTimeOut и  keepAliveSeconds
     * @return - ThreadPoolTaskExecutor
     */
    @Override
    @Bean(name = "asyncTaskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //минимальное количество рабочих процессов, которое необходимо поддерживать без тайм - аута
        executor.setCorePoolSize(taskExecutionProperties.getPool().getCoreSize());
        //определяет максимальное количество потоков, которые могут быть когда-либо созданы
        executor.setMaxPoolSize(taskExecutionProperties.getPool().getMaxSize());
        //емкость очереди
        executor.setQueueCapacity(taskExecutionProperties.getPool().getQueueCapacity());
        //имя потока
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
