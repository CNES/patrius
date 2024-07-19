/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles
 * de troposphere
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit test class for the {@link ConstantMeteorologicalConditionsProvider} class.
 *
 * @author William POLYCARPE (TSN)
 */
public class ConstantMeteorologicalConditionsProviderTest {

    // Default values
    private static final double PRESSURE = 2.0;
    private static final double TEMPERATURE = 1.0;
    private static final double HUMIDITY = 3.0;

    private final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(
        PRESSURE, TEMPERATURE, HUMIDITY);

    final AbsoluteDate DEFAULT_DATE = new AbsoluteDate();

    @Test
    public void testDefaultConstructor() {

        final ConstantMeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            this.meteoConditions);

        final MeteorologicalConditions meteoConditionsOut = meteoConditionsProvider
            .getMeteorologicalConditions(this.DEFAULT_DATE);

        final double pressure = meteoConditionsOut.getPressure();
        final double temperature = meteoConditionsOut.getTemperature();
        final double humidity = meteoConditionsOut.getHumidity();

        Assert.assertEquals(PRESSURE, pressure, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(TEMPERATURE, temperature, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(HUMIDITY, humidity, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testConstructor() {

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(this.DEFAULT_DATE,
            this.DEFAULT_DATE.shiftedBy(1.0));
        final ConstantMeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            this.meteoConditions, interval);

        // Date in middle of interval
        final AbsoluteDate date = this.DEFAULT_DATE.shiftedBy(0.5);

        final MeteorologicalConditions meteoConditionsOut = meteoConditionsProvider
            .getMeteorologicalConditions(date);

        final double pressure = meteoConditionsOut.getPressure();
        final double temperature = meteoConditionsOut.getTemperature();
        final double humidity = meteoConditionsOut.getHumidity();

        Assert.assertEquals(PRESSURE, pressure, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(TEMPERATURE, temperature, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(HUMIDITY, humidity, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testConstructorException() {

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(this.DEFAULT_DATE,
            this.DEFAULT_DATE.shiftedBy(1.0));
        final ConstantMeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            this.meteoConditions, interval);

        // Date out of interval
        final AbsoluteDate date = this.DEFAULT_DATE.shiftedBy(-0.5);

        try {
            meteoConditionsProvider.getMeteorologicalConditions(date);
            Assert.fail();
        } catch (final IllegalStateException ise) {
            Assert.assertEquals(PatriusMessages.DATE_OUTSIDE_INTERVAL.getSourceString(),
                ise.getMessage());
        }
    }

    /**
     * @description Tests the equals and hashCode methods.
     *
     * @testedMethod {@link ConstantMeteorologicalConditionsProvider#equals(Object)}
     * @testedMethod {@link ConstantMeteorologicalConditionsProvider#hashCode()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testEqualsAndHashCode() {

        final MeteorologicalConditions meteoConditionsBis = new MeteorologicalConditions(10., 5., 15.);

        final AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(AbsoluteDate.J2000_EPOCH,
            AbsoluteDate.J2000_EPOCH.shiftedBy(10.));
        final AbsoluteDateInterval dateIntervalBis = new AbsoluteDateInterval(AbsoluteDate.J2000_EPOCH,
            AbsoluteDate.J2000_EPOCH.shiftedBy(20.));

        // New instance
        final ConstantMeteorologicalConditionsProvider instance = new ConstantMeteorologicalConditionsProvider(
            this.meteoConditions, dateInterval);

        // Check the hashCode consistency between calls
        final int hashCode = instance.hashCode();
        Assert.assertEquals(hashCode, instance.hashCode());

        // Compared object is null
        Assert.assertFalse(instance.equals(null));

        // Compared object is a different class
        Assert.assertFalse(instance.equals(new Object()));

        // Same instance
        Assert.assertEquals(instance, instance);

        // Same data, but different instances
        ConstantMeteorologicalConditionsProvider other = new ConstantMeteorologicalConditionsProvider(
            this.meteoConditions, dateInterval);

        Assert.assertEquals(other, instance);
        Assert.assertEquals(instance, other);
        Assert.assertEquals(other.hashCode(), instance.hashCode());

        // Different meteorological conditions
        other = new ConstantMeteorologicalConditionsProvider(meteoConditionsBis, dateInterval);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different interval
        other = new ConstantMeteorologicalConditionsProvider(this.meteoConditions, dateIntervalBis);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }
}
