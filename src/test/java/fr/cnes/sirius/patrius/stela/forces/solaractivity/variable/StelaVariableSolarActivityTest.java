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
 * Class test for {@link StelaVariableSolarActivity}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableSolarActivityTest {

    /**
     * Resource directory
     */
    private static final String RESOURCE_DIR =
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "stela"
                + File.separator + "solaractivity" + File.separator;

    /**
     * Common date for all tests
     */
    public static final double CNESJD = 17500.0;

    /**
     * Timescale constant for test
     */
    private static TimeScale TIME_SCALE;

    /**
     * Variable solar activity reader
     */
    private static ACSOLFormatReader reader;

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

        reader = new ACSOLFormatReader(".txt");
        reader.loadData(Files.newInputStream(Paths.get(
            RESOURCE_DIR + "stela_solar_activity")), "DUMMY");

        defaultVariableSolarActivity = new StelaVariableSolarActivity(reader);
    }

    /**
     * Compare the default solar activity mean F10.7 value with the expected one <br>
     * 
     * Method tested : {@link StelaVariableSolarActivity#getMeanFlux(AbsoluteDate, double[])}
     */
    @Test
    public void testGetMeanFlux() throws PatriusException {
        final double[] weight = new double[10];
        Arrays.fill(weight, 1);
        weight[2] = 10;
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        final double meanFlux = defaultVariableSolarActivity.getMeanFlux(date, weight);
        Assert.assertEquals(111.30000000000001, meanFlux, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Compare the default solar activity instant F10.7 value with the expected one <br>
     * 
     * Method tested : {@link StelaVariableSolarActivity#getInstantFluxValue(AbsoluteDate)}
     */
    @Test
    public void testGetInstantFluxValue() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        double flux = defaultVariableSolarActivity.getInstantFluxValue(date);
        Assert.assertEquals(112.14999999999999, flux, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Compare the default solar activity AP value with the expected one <br>
     * 
     * Method tested : {@link StelaVariableSolarActivity#getAp(AbsoluteDate)}
     */
    @Test
    public void testGetAp() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        double apValue = defaultVariableSolarActivity.getAp(date);
        Assert.assertEquals(4.0, apValue, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Compare the default solar activity values with the expected ones <br>
     * 
     * Method tested : {@link StelaVariableSolarActivity#getSolarActivity(AbsoluteDate)}
     */
    @Test
    public void testGetSolarActivity() throws PatriusException {
        assertDefaultSolarActivity(defaultVariableSolarActivity);
    }

    /**
     * Check a solar activity with the default reader values calling the
     * {@link StelaVariableSolarActivity#getSolarActivity(AbsoluteDate)} method
     *
     * @param solarActivity the solar activity to check
     */
    private static void assertDefaultSolarActivity(StelaVariableSolarActivity solarActivity) throws PatriusException {
        // Initialization
        final AbsoluteDate date = new AbsoluteDate(17531.989507, TIME_SCALE);

        final double[] expected =
            { 101.799757, 96.207026, 4.135493, 2.416059, 2.000000, 2.000000, 3.167883, 14.271897, 9.311132 };
        final double threshold = 1E-05;

        // Test and comparison

        double[] solActArray;
        solActArray = solarActivity.getSolarActivity(date);

        Assert.assertEquals("SolarActivity[0]", expected[0], solActArray[0], threshold);
        Assert.assertEquals("SolarActivity[1]", expected[1], solActArray[1], threshold);
        Assert.assertEquals("SolarActivity[2]", expected[2], solActArray[2], threshold);
        Assert.assertEquals("SolarActivity[3]", expected[3], solActArray[3], threshold);
        Assert.assertEquals("SolarActivity[4]", expected[4], solActArray[4], threshold);
        Assert.assertEquals("SolarActivity[5]", expected[5], solActArray[5], threshold);
        Assert.assertEquals("SolarActivity[6]", expected[6], solActArray[6], threshold);
        Assert.assertEquals("SolarActivity[7]", expected[7], solActArray[7], threshold);
        Assert.assertEquals("SolarActivity[8]", expected[8], solActArray[8], threshold);
    }

    /**
     * Method tested : {@link StelaVariableSolarActivity#toString()}
     */
    @Test
    public void testToString() throws PatriusException {
        final String CR = System.lineSeparator();
        Assert.assertEquals("[ Solar Activity ]" + CR + "Solar Activity Type : VARIABLE" + CR,
            defaultVariableSolarActivity.toString());
    }

    /**
     * Method tested : {@link StelaVariableSolarActivity#copy()}
     */
    @Test
    public void testCopy() throws PatriusException, IOException, ParseException {
        final StelaVariableSolarActivity solarActivityCopy = defaultVariableSolarActivity.copy();
        Assert.assertNotEquals(defaultVariableSolarActivity, solarActivityCopy);
        assertDefaultSolarActivity(solarActivityCopy);
    }

    /**
     * Method tested : {@link StelaVariableSolarActivity#getEntireAPMap()}
     */
    @Test
    public void testGetEntireAPMap() {
        // Built expected
        final TreeMap<AbsoluteDate, Double[]> map =
            new TreeMap<>(reader.getApKpValues(reader.getMinDate(), reader.getMaxDate()));
        final TreeMap<AbsoluteDate, Double> expectedMap = new TreeMap<>();
        // Populate the AP map with the first element of the Double[] array
        for (final Map.Entry<AbsoluteDate, Double[]> entry : map.entrySet()) {
            // Check if the value array is not null and has at least one element
            if (entry.getValue() != null && entry.getValue().length > 0) {
                expectedMap.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        // Built actual
        final TreeMap<AbsoluteDate, Double> actualMap = new TreeMap<>(defaultVariableSolarActivity.getEntireAPMap());

        // Assert equals expected with actual
        Assert.assertEquals(expectedMap, actualMap);
    }
}
