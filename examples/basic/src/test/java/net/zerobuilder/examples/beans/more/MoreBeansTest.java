package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.MoreBeans.Access;
import net.zerobuilder.examples.beans.more.MoreBeans.AeroExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.BioExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.Ignorify;
import net.zerobuilder.examples.beans.more.MoreBeans.IterableExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.OverloadedExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.RawExperiment;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.examples.beans.more.MoreBeans_AccessBuilders.accessBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_AccessBuilders.accessToBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_AeroExperimentBuilders.aeroExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_AeroExperimentBuilders.aeroExperimentToBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_BioExperimentBuilders.bioExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_BioExperimentBuilders.bioExperimentToBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_IgnorifyBuilders.ignorifyBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_IgnorifyBuilders.ignorifyToBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_IterableExperimentBuilders.iterableExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_IterableExperimentBuilders.iterableExperimentToBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_OverloadedExperimentBuilders.overloadedExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_OverloadedExperimentBuilders.overloadedExperimentToBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_RawExperimentBuilders.rawExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_RawExperimentBuilders.rawExperimentToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MoreBeansTest {

  @Test
  public void atmosphericTest() {
    AeroExperiment experiment1 = aeroExperimentBuilder()
        .altitude(10)
        .yield(20);
    AeroExperiment experiment2 = aeroExperimentToBuilder(experiment1)
        .yield(100)
        .build();
    assertThat(experiment1.getAltitude(), is(10));
    assertThat(experiment1.getYield(), is(20));
    assertThat(experiment2.getAltitude(), is(10));
    assertThat(experiment2.getYield(), is(100));
  }

  @Test
  public void overloadedTest() {
    OverloadedExperiment experiment1 = overloadedExperimentBuilder()
        .yield(10);
    OverloadedExperiment experiment2 = overloadedExperimentToBuilder(experiment1)
        .yield(20)
        .build();
    assertThat(experiment1.getYield(), is(10));
    assertThat(experiment2.getYield(), is(20));
  }

  @Test
  public void biologicalTest() {
    BioExperiment experiment1 = bioExperimentBuilder()
        .candidates(singletonList(asList("Goku", "Frieza")));
    BioExperiment experiment2 = bioExperimentToBuilder(experiment1)
        .candidates(singletonList(asList("Gohan")))
        .build();
    assertThat(experiment1.getCandidates(), is(singletonList(asList("Goku", "Frieza"))));
    assertThat(experiment2.getCandidates(), is(singletonList(asList("Gohan"))));
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void objectTest() {
    RawExperiment experiment1 = rawExperimentBuilder()
        .things(asList(1, "one"));
    RawExperiment experiment2 = rawExperimentToBuilder(experiment1)
        .things(asList(2))
        .build();
    List expectedList1 = new ArrayList();
    expectedList1.add(1);
    expectedList1.add("one");
    List expectedList2 = new ArrayList();
    expectedList2.add(2);
    assertThat(experiment1.getThings(), is(expectedList1));
    assertThat(experiment2.getThings(), is(expectedList2));
  }

  @Test
  public void iterableTest() {
    IterableExperiment experiment1 = iterableExperimentBuilder()
        .things(asList(asList("1")));
    IterableExperiment experiment2 = iterableExperimentToBuilder(experiment1)
        .things(asList(asList("2")))
        .build();
    List<Iterable<String>> expectedList1 = new ArrayList();
    expectedList1.add(asList("1"));
    List<Iterable<String>> expectedList2 = new ArrayList();
    expectedList2.add(asList("2"));
    assertThat(experiment1.getThings(), is(expectedList1));
    assertThat(experiment2.getThings(), is(expectedList2));
  }

  @Test
  public void ignoreTest() {
    Ignorify nothing = ignorifyBuilder().emptyThings();
    Ignorify something = ignorifyToBuilder(nothing)
        .things(singletonList(singletonList("a")))
        .build();
    Ignorify nothing2 = ignorifyToBuilder(nothing)
        .emptyThings()
        .build();
    assertThat(nothing.getThings().size(), is(0));
    assertThat(something.getThings().size(), is(1));
    assertThat(something.getThings().get(0),
        is((Iterable<String>) singletonList("a")));
    assertThat(nothing2.getThings().size(), is(0));
  }

  @Test
  public void accessTest() throws NoSuchMethodException {
    Access foo = accessBuilder()
        .foo("foo");
    Access bar = accessToBuilder(foo)
        .foo("bar")
        .build();
    Method builderMethod = MoreBeans_AccessBuilders.class.getDeclaredMethod("accessBuilder");
    Method toBuilderMethod = MoreBeans_AccessBuilders.class.getDeclaredMethod("accessToBuilder", Access.class);
    assertFalse(Modifier.isPublic(builderMethod.getModifiers()));
    assertTrue(Modifier.isPublic(toBuilderMethod.getModifiers()));
    assertThat(foo.getFoo(), is("foo"));
    assertThat(bar.getFoo(), is("bar"));
  }
}