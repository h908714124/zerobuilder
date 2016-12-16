package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;
import net.zerobuilder.NotNullStep;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

final class GenericInstance<S> {

  @Builder
  @GoalName("entry")
  <K, V> Map.Entry<K, V> entry(@NotNullStep S suffix,
                               @NotNullStep K key,
                               @NotNullStep V value) {
    return new SimpleEntry(key, value + String.valueOf(suffix));
  }
}
