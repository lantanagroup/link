package com.lantanagroup.link;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

public class StreamUtils {
  public static <T> Collector<T, ?, List<List<T>>> paging(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Size must be greater than zero");
    }
    BiConsumer<List<List<T>>, T> accumulator = (pages, item) -> {
      if (pages.isEmpty() || pages.get(pages.size() - 1).size() == size) {
        pages.add(new ArrayList<>(size));
      }
      pages.get(pages.size() - 1).add(item);
    };
    BinaryOperator<List<List<T>>> combiner = (pages1, pages2) -> {
      throw new UnsupportedOperationException("Combining not supported");
    };
    return Collector.of(ArrayList::new, accumulator, combiner);
  }

  public static <T> T toOnlyElement(T element1, T element2) {
    throw new IllegalStateException("Expected at most one element but found multiple");
  }
}
