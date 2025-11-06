package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.*;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

/**
 * Class test for {@link StelaVariableDispersedSolarActivity}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableDispersedSolarActivityTest {

    /**
     * Resource directory
     */
    private static final String RESOURCE_DIR =
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "stela"
                + File.separator + "solaractivity" + File.separator;

    /**
     * AP coefficient constant for tests
     */
    private static final double AP_COEF = 2.2;

    /**
     * F10.7 coefficient constant for tests
     */
    private static final double F_107_COEF = 3.3;

    /**
     * Timescale constant for test
     */
    private static TimeScale TIME_SCALE;

    /**
     * Common date for all tests
     */
    public static final double CNESJD = 17531.989507;

    /**
     * Variable solar activity reader
     */
    private static final ACSOLFormatReader reader = new ACSOLFormatReader(".txt");

    /**
     * Default dispersed variable solar activity
     */
    private static StelaVariableDispersedSolarActivity defaultVariableDispersedSolarActivity;

    /**
     * Default variable solar activity
     */
    private static StelaVariableSolarActivity defaultVariableSolarActivity;

    /**
     * Set up.
     */
    @BeforeClass
    public static void setUp() throws IOException, PatriusException, ParseException {
        Utils.clear();

        // Next line clears data set by other tests,
        // are overriden later
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // UTC-TAI leap seconds:
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) {
                // nothing to do
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<>();
                for (int i = 1969; i < 2010; i++) {
                    // constant value:
                    map.put(new DateComponents(i, 11, 13), 35);
                }
                return map;
            }

            @Override
            public String getSupportedNames() {
                return "No name";
            }
        });

        // Set the timescale to UTC
        TIME_SCALE = TimeScalesFactory.getUTC();

        reader.loadData(Files.newInputStream(Paths.get(
            RESOURCE_DIR + "stela_solar_activity")), "DUMMY");

        defaultVariableDispersedSolarActivity =
            new StelaVariableDispersedSolarActivity(AP_COEF, F_107_COEF, reader);
        defaultVariableSolarActivity = new StelaVariableSolarActivity(reader);

    }

    /**
     * Compare the default solar activity F10.7 and AP values with the expected ones <br>
     *
     * Method tested : <br>
     * - {@link StelaVariableDispersedSolarActivity#getInstantFluxValue(AbsoluteDate)} <br>
     * - {@link StelaVariableDispersedSolarActivity#getAp(AbsoluteDate)} <br>
     */
    @Test
    public void getFluxAPValue() throws PatriusException {

        // Variable dispersed
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        final double dispersedFlux = defaultVariableDispersedSolarActivity.getInstantFluxValue(date);
        final double dispersedAP = defaultVariableDispersedSolarActivity.getAp(date);

        // Variable

        final double flux = defaultVariableSolarActivity.getInstantFluxValue(date);
        final double ap = defaultVariableSolarActivity.getSolarActivity(date)[2];

        Assert.assertEquals(flux * F_107_COEF, dispersedFlux, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(ap * AP_COEF, dispersedAP, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(343.35541801001517, dispersedFlux, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(9.098084600003496, dispersedAP, Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Compare the default solar activity values with the expected ones <br>
     *
     * Method tested : <br>
     * - {@link StelaVariableDispersedSolarActivity#getSolarActivity(AbsoluteDate)}
     */
    @Test
    public void getSolarActivity() throws PatriusException {
        // Assert defaultVariableDispersedSolarActivity
        assertDefaultSolarActivity(defaultVariableDispersedSolarActivity);

        // Build actual with default constructor
        final StelaVariableDispersedSolarActivity solarActivity = new StelaVariableDispersedSolarActivity(reader);
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        final double[] solarTab = solarActivity.getSolarActivity(date);
        final Double[] doubleTab = new Double[solarTab.length];
        int i = 0;
        for (double value : solarTab) {
            doubleTab[i] = value;
            i++;
        }
        // Built expected
        final Double[] expected = new Double[solarTab.length];
        Arrays.fill(expected, 0.);

        Assert.assertArrayEquals(expected, doubleTab);
    }

    /**
     * Check a solar activity with the default reader values calling the
     * {@link StelaVariableDispersedSolarActivity#getSolarActivity(AbsoluteDate)} method
     *
     * @param solarActivity the solar activity to check
     */
    private static void assertDefaultSolarActivity(StelaVariableDispersedSolarActivity solarActivity)
        throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);

        final double[] expected =
            { 101.799757, 96.207026, 4.135493, 2.416059, 2.000000, 2.000000, 3.167883, 14.271897, 9.311132 };
        final double threshold = 1E-05 * Math.max(AP_COEF, F_107_COEF);

        // Test and comparison
        final double[] solActArray = solarActivity.getSolarActivity(date);

        Assert.assertEquals("SolarActivity[0]", expected[0] * F_107_COEF, solActArray[0], threshold);
        Assert.assertEquals("SolarActivity[1]", expected[1] * F_107_COEF, solActArray[1], threshold);
        Assert.assertEquals("SolarActivity[2]", expected[2] * AP_COEF, solActArray[2], threshold);
        Assert.assertEquals("SolarActivity[3]", expected[3] * AP_COEF, solActArray[3], threshold);
        Assert.assertEquals("SolarActivity[4]", expected[4] * AP_COEF, solActArray[4], threshold);
        Assert.assertEquals("SolarActivity[5]", expected[5] * AP_COEF, solActArray[5], threshold);
        Assert.assertEquals("SolarActivity[6]", expected[6] * AP_COEF, solActArray[6], threshold);
        Assert.assertEquals("SolarActivity[7]", expected[7] * AP_COEF, solActArray[7], threshold);
        Assert.assertEquals("SolarActivity[8]", expected[8] * AP_COEF, solActArray[8], threshold);
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedSolarActivity#toString()}
     */
    @Test
    public void testToString() throws PatriusException {
        Assert.assertEquals("[ Solar Activity ]" + System.lineSeparator() + " Solar Activity Type : VARIABLE_DISPERSED"
                + System.lineSeparator() + " F10.7 coefficient : " + F_107_COEF + System.lineSeparator()
                + " Ap coefficient : " + AP_COEF,
            defaultVariableDispersedSolarActivity.toString());
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedSolarActivity#copy()}
     */
    @Test
    public void copy() throws PatriusException {
        final StelaVariableDispersedSolarActivity solarActivityCopy = defaultVariableDispersedSolarActivity.copy();
        Assert.assertNotEquals(defaultVariableDispersedSolarActivity, solarActivityCopy);
        assertDefaultSolarActivity(solarActivityCopy);
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedSolarActivity#getFluxCoef()} <br>
     * - {@link StelaVariableDispersedSolarActivity#getApCoef()} <br>
     */
    @Test
    public void getFluxApCoef() {
        Assert.assertEquals(AP_COEF, defaultVariableDispersedSolarActivity.getApCoef(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(F_107_COEF, defaultVariableDispersedSolarActivity.getFluxCoef(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedSolarActivity#getEntireAPMap()}
     */
    @Test
    public void getEntireAPMap() {
        // Build expected
        final TreeMap<AbsoluteDate, Double> expectedMap = new TreeMap<>(defaultVariableSolarActivity.getEntireAPMap());
        expectedMap.replaceAll((date, apValue) -> expectedMap.get(date) * AP_COEF);
        final Collection<Double> expected = expectedMap.values();

        // Built actual
        final TreeMap<AbsoluteDate, Double> actualMap = defaultVariableDispersedSolarActivity.getEntireAPMap();
        final Collection<Double> actual = actualMap.values();

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());

    }

}
