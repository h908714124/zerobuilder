package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

@Builders
public class MapEntry {

  @Goal(name = "entry")
  static <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, value);
  }

  @Goal(name = "sentry")
  static <K extends String, V extends K> Map.Entry<K, V> sentry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, value);
  }
}
