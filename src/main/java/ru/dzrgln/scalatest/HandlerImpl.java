package ru.dzrgln.scalatest;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HandlerImpl implements Handler {
    private final Client client;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public HandlerImpl(Client client) {
        this.client = client;
    }

    @Override
    public Duration timeout() {
        return null;
    }

    /*
    Примечания к решению:
    1. Сейчас вызов данного метода запускает набор бесконечных циклов, которые будут читать данные и кидать их в
    очередь executor serivice'a. Для остановки этого сервиса нужно будет где-то предусмотреть метод, вызывающий
    shotdown() у executor serivice'a для остановки приема новых задач.
    2. Для более корректного выбора конкретных реализаций executor serivice'ов необходимо было бы уточнить максимально
    возможное количество накапливаемых данных для чтения и порядок количества адресов для отправки.
     */
    @Override
    public void performOperation() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        var executorService = Executors.newFixedThreadPool(availableProcessors);
        for (int i = 0; i < availableProcessors; i++) {
            executorService.submit(this::getData);
        }
    }

    private void getData() {
        while (true) {
            var event = client.readData();
            if (Objects.nonNull(event)) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(Event event) {
        var recipients = event.recipients();
        var runnableList = recipients.stream()
                .map(address -> (Runnable) () -> sendData(address, event.payload()))
                .map(Executors::callable)
                .collect(Collectors.toList());
        try {
            executor.invokeAll(runnableList);
        } catch (InterruptedException e) {
            executor.shutdown();
            e.printStackTrace();
        }
    }

    private void sendData(Address address, Payload payload) {
        var res = Result.REJECTED;
        while (res == Result.REJECTED) {
            res = client.sendData(address, payload);
            if (res == Result.REJECTED) {
                try {
                    Thread.sleep(timeout().toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
