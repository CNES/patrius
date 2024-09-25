/**
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
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
/*
 */
package fr.cnes.sirius.patrius.signalpropagation;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Unit test class for the {@link MeteorologicalConditions} class.
 *
 * @author bonitt
 */
public class MeteorologicalConditionsTest {

    private static final double DEFAULT_PRESSURE = 5.0;
    private static final double DEFAULT_TEMPERATURE = 1.0;
    private static final double DEFAULT_HUMIDITY = 6.0;
    private static final double ONE = 1.0;
    private static final double TWO = 2.0;

    /** Validity threshold. */
    private static final double epsilon = 1e-10;

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link MeteorologicalConditions#MeteorologicalConditions(double, double, double)}
     * @testedMethod {@link MeteorologicalConditions#getPressure()}
     * @testedMethod {@link MeteorologicalConditions#getTemperature()}
     * @testedMethod {@link MeteorologicalConditions#getHumidity()}
     * @testedMethod {@link MeteorologicalConditions#P0}
     * @testedMethod {@link MeteorologicalConditions#T0}
     * @testedMethod {@link MeteorologicalConditions#RH0}
     * @testedMethod {@link MeteorologicalConditions#H0}
     * @testedMethod {@link MeteorologicalConditions#ABSOLUTE_ZERO}
     * @testedMethod {@link MeteorologicalConditions#STANDARD}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the
     *                   expected data.
     */
    @Test
    public void testConstructor() {

        final MeteorologicalConditions meteorologicalConditions =
            new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY);

        Assert.assertEquals(DEFAULT_PRESSURE, meteorologicalConditions.getPressure(), 0.);
        Assert.assertEquals(DEFAULT_TEMPERATURE, meteorologicalConditions.getTemperature(), 0.);
        Assert.assertEquals(DEFAULT_HUMIDITY, meteorologicalConditions.getHumidity(), 0.);

        // Evaluate the static default values by non regression
        Assert.assertEquals(101325., MeteorologicalConditions.P0, 0.);
        Assert.assertEquals(18., MeteorologicalConditions.T0, 0.);
        Assert.assertEquals(50., MeteorologicalConditions.RH0, 0.);
        Assert.assertEquals(0., MeteorologicalConditions.H0, 0.);
        Assert.assertEquals(273.15, MeteorologicalConditions.ABSOLUTE_ZERO, 0.);
        Assert.assertEquals(new MeteorologicalConditions(MeteorologicalConditions.P0, MeteorologicalConditions.T0
                + MeteorologicalConditions.ABSOLUTE_ZERO, MeteorologicalConditions.RH0),
            MeteorologicalConditions.STANDARD);
    }

    /**
     * @description Tests the equals and hashCode methods.
     *
     * @testedMethod {@link MeteorologicalConditions#equals(Object)}
     * @testedMethod {@link MeteorologicalConditions#hashCode()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testEqualsAndHashCode() {

        // New instance
        final MeteorologicalConditions instance = new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE,
            DEFAULT_HUMIDITY);

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
        MeteorologicalConditions other = new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE,
            DEFAULT_HUMIDITY);

        Assert.assertEquals(other, instance);
        Assert.assertEquals(instance, other);
        Assert.assertEquals(other.hashCode(), instance.hashCode());

        // Different pressure
        other = new MeteorologicalConditions(10., DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different temperature
        other = new MeteorologicalConditions(DEFAULT_PRESSURE, 10., DEFAULT_HUMIDITY);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different humidity
        other = new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE, 10.);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    /**
     * @description Evaluate the meteorological conditions container serialization / deserialization
     *              process.
     *
     * @testPassCriteria The meteorological conditions container can be serialized with all its
     *                   parameters and deserialized.
     */
    @Test
    public void testSerialization() {

        final MeteorologicalConditions meteorologicalConditions =
            new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY);
        final MeteorologicalConditions deserializedMeteorologicalConditions = TestUtils
            .serializeAndRecover(meteorologicalConditions);
        Assert.assertEquals(meteorologicalConditions, deserializedMeteorologicalConditions);
    }

    /**
     * @description Check the String representation method behavior.
     *
     * @testedMethod {@link MeteorologicalConditions#toString()}
     *
     * @testPassCriteria The container String representation contains the expected information.
     */
    @Test
    public void testToString() {
        final MeteorologicalConditions meteorologicalConditions =
            new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY);
        final String expectedText = "MeteorologicalConditions{5.0[Pa]; 1.0[K]; 6.0[%]}\n";
        Assert.assertEquals(expectedText, meteorologicalConditions.toString());
    }

    /**
     * @description Try to set a pressure (in Pa) with a negative value (shouldn't be allowed).
     *
     * @testedMethod {@link MeteorologicalConditions#MeteorologicalConditions(double, double, double)}
     *
     * @testPassCriteria The {@link NotPositiveException} exception is returned as expected.
     */
    @Test
    public void testNotPositiveExceptionPressure() {
        try {
            new MeteorologicalConditions(-1.0, DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Try to set a temperature (in K) with a negative value (shouldn't be allowed).
     *
     * @testedMethod {@link MeteorologicalConditions#MeteorologicalConditions(double, double, double)}
     *
     * @testPassCriteria The {@link NotStrictlyPositiveException} exception is returned as expected.
     */
    @Test
    public void testNotStrictlyPositiveExceptionTemperature() {
        try {
            new MeteorologicalConditions(DEFAULT_PRESSURE, -2., DEFAULT_HUMIDITY);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Try to set the humidity out of the [0 ; 100] range (shouldn't be allowed).
     *
     * @testedMethod {@link MeteorologicalConditions#MeteorologicalConditions(double, double, double)}
     *
     * @testPassCriteria The {@link OutOfRangeException} exception is returned as expected.
     */
    @Test
    public void testOutOfRangeException() {
        try {
            new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE, -20.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new MeteorologicalConditions(DEFAULT_PRESSURE, DEFAULT_TEMPERATURE, 120.);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Pure non-regression test: there is not associated reference.
     * 
     * @testedMethod {@link MeteorologicalConditions#computeStandardValues(double)}
     */
    @Test
    public void testComputeStandardValues() {

        MeteorologicalConditions standardValues = MeteorologicalConditions.computeStandardValues(0.);
        Assert.assertEquals(101325.0, standardValues.getPressure(), epsilon);
        Assert.assertEquals(291.15, standardValues.getTemperature(), epsilon);
        Assert.assertEquals(50.0, standardValues.getHumidity(), epsilon);

        standardValues = MeteorologicalConditions.computeStandardValues(10.);
        Assert.assertEquals(101205.40748359638, standardValues.getPressure(), epsilon);
        Assert.assertEquals(291.085, standardValues.getTemperature(), epsilon);
        Assert.assertEquals(49.68122054344217, standardValues.getHumidity(), epsilon);

        standardValues = MeteorologicalConditions.computeStandardValues(100.);
        Assert.assertEquals(100134.20224900974, standardValues.getPressure(), epsilon);
        Assert.assertEquals(290.5, standardValues.getTemperature(), epsilon);
        Assert.assertEquals(46.902126024056234, standardValues.getHumidity(), epsilon);

        standardValues = MeteorologicalConditions.computeStandardValues(1000.);
        Assert.assertEquals(89917.56989589504, standardValues.getPressure(), epsilon);
        Assert.assertEquals(284.65, standardValues.getTemperature(), epsilon);
        Assert.assertEquals(26.375169160084233, standardValues.getHumidity(), epsilon);

        standardValues = MeteorologicalConditions.computeStandardValues(10000.);
        Assert.assertEquals(26569.790617728395, standardValues.getPressure(), epsilon);
        Assert.assertEquals(226.14999999999998, standardValues.getTemperature(), epsilon);
        Assert.assertEquals(8.341084062329147E-2, standardValues.getHumidity(), epsilon);
    }

    @Test
    public void testMean() {

        final MeteorologicalConditions meteo1 = new MeteorologicalConditions(DEFAULT_PRESSURE,
            DEFAULT_TEMPERATURE, DEFAULT_HUMIDITY);
        final MeteorologicalConditions meteo2 = new MeteorologicalConditions(DEFAULT_PRESSURE
                + MeteorologicalConditionsTest.ONE,
            DEFAULT_TEMPERATURE
                    + MeteorologicalConditionsTest.ONE, DEFAULT_HUMIDITY + MeteorologicalConditionsTest.ONE);
        final MeteorologicalConditions meteo3 = new MeteorologicalConditions(DEFAULT_PRESSURE
                + MeteorologicalConditionsTest.TWO,
            DEFAULT_TEMPERATURE
                    + MeteorologicalConditionsTest.TWO, DEFAULT_HUMIDITY + MeteorologicalConditionsTest.TWO);

        final ArrayList<MeteorologicalConditions> meteoConditionsList = new ArrayList<>(3);
        meteoConditionsList.add(meteo1);
        meteoConditionsList.add(meteo2);
        meteoConditionsList.add(meteo3);

        final MeteorologicalConditions averageConditions = MeteorologicalConditions
            .mean(meteoConditionsList);

        final double averagePressure = averageConditions.getPressure();
        final double averageTemperature = averageConditions.getTemperature();
        final double averageHumidity = averageConditions.getHumidity();

        Assert.assertEquals(DEFAULT_PRESSURE + MeteorologicalConditionsTest.ONE, averagePressure,
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(DEFAULT_TEMPERATURE + MeteorologicalConditionsTest.ONE, averageTemperature,
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(DEFAULT_HUMIDITY + MeteorologicalConditionsTest.ONE, averageHumidity,
            Precision.DOUBLE_COMPARISON_EPSILON);
    }
}
