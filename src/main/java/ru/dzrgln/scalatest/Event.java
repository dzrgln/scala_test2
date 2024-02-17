package ru.dzrgln.scalatest;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {
}
