package net.zerobuilder.examples.values.inheritance;

import org.junit.Test;

import java.math.BigInteger;

import static java.math.BigInteger.TEN;
import static net.zerobuilder.examples.values.inheritance.PlanetBuilders.planetUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PlanetTest {

  @Test
  public void buildPlanet() throws Exception {
    BigInteger mass = new BigInteger("597237000000000000000000");
    Planet planet = PlanetBuilders.planetBuilder()
        .mass(mass)
        .numberOfMoons(1)
        .habitable(true);
    planet = planetUpdater(planet).mass(mass.multiply(TEN)).done();
    assertThat(planet.getNumberOfMoons(), is(1));
    assertThat(planet.isHabitable(), is(true));
    assertThat(planet.getMass(), is(mass.multiply(TEN)));
  }

}