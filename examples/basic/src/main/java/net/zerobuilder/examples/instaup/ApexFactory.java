package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

@Builders
final class ApexFactory {
  private final String string;

  ApexFactory(String string) {
    this.string = string;
  }

  @Goal(builder = false, updater = true)
  Apex apex(String appendix) {
    return new Apex(string, appendix);
  }
}
