package ru.dzrgln.scalatest;

import java.time.Duration;

public interface Handler {
    Duration timeout();

    void performOperation();
}
