package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

// originally generated by RandomGenericsTest
final class Gen1<A, B extends A> {

  private final A aa0;
  private final B ab0;
  private final String someString;

  Gen1(A aa0, B ab0, String someString) {
    this.aa0 = aa0;
    this.ab0 = ab0;
    this.someString = someString;
  }

  @Updater
  @Builder
  <C, D extends C> Bar<A, B, C, D> bar(C bc0,
                                       C bc1,
                                       D bd0,
                                       String bd1,
                                       C bc2) {
    return new Bar<>(aa0, someString, ab0, bc0, bc1, bd0, bd1, bc2);
  }

  static final class Bar<A, B extends A, C, D extends C> {

    final A aa0;
    final String someString;
    final B ab0;
    final C bc0;
    final C bc1;
    final D bd0;
    final String bd1;
    final C bc2;

    Bar(A aa0, String someString, B ab0, C bc0, C bc1, D bd0, String bd1, C bc2) {
      this.aa0 = aa0;
      this.someString = someString;
      this.ab0 = ab0;
      this.bc0 = bc0;
      this.bc1 = bc1;
      this.bd0 = bd0;
      this.bd1 = bd1;
      this.bc2 = bc2;
    }
  }
}
