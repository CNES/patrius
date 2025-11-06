package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.*;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class test for {@link Stela3StepsSolarActivity}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class Stela3StepsSolarActivityTest {

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
     * Common date 1 for all tests
     */
    private static final double DATE_1 = 17500;

    /**
     * Common date 2 for all tests
     */
    private static final double DATE_2 = 17510;

    /**
     * Variable solar activity reader
     */
    private static ACSOLFormatReader reader;

    /**
     * Default 3 steps solar activity
     */
    private static Stela3StepsSolarActivity defaultStela3StepsSolarActivity;

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

        reader = new ACSOLFormatReader(".txt");
        reader.loadData(Files.newInputStream(Paths.get(
            RESOURCE_DIR + "stela_solar_activity")), "DUMMY");

        final List<String> pathList = new ArrayList<>();
        final String dir = RESOURCE_DIR + "Solar_Activity_Cycles" + File.separator;
        pathList.add(dir + "CF_STELA_Cycle_1_def");
        pathList.add(dir + "CF_STELA_Cycle_2_def");
        pathList.add(dir + "CF_STELA_Cycle_3_def");
        pathList.add(dir + "CF_STELA_Cycle_4_def");
        pathList.add(dir + "CF_STELA_Cycle_5_def");
        pathList.add(dir + "CF_STELA_Cycle_6_def");
        StelaPastCycleSolarActivityProperties.setPastCycleFilePaths(pathList);

        final List<Integer> cycles1 = new ArrayList<>();
        cycles1.add(2);
        cycles1.add(4);
        cycles1.add(1);
        cycles1.add(3);

        // Set the timescale to UTC
        TIME_SCALE = TimeScalesFactory.getUTC();

        final AbsoluteDate startDate = new AbsoluteDate(DATE_1 - 10, TIME_SCALE);
        final AbsoluteDate date1 = new AbsoluteDate(DATE_1, TIME_SCALE);
        final AbsoluteDate date2 = new AbsoluteDate(DATE_2, TIME_SCALE);
        defaultStela3StepsSolarActivity = new Stela3StepsSolarActivity(date1, date2,
            F_107_COEF, AP_COEF, cycles1, reader);
        defaultStela3StepsSolarActivity.buildSolarActivity(startDate);
    }

    /**
     * Compare the default constructor values with expected ones <br>
     *
     * Method tested : {@link Stela3StepsSolarActivity#Stela3StepsSolarActivity(SolarActivityDataProvider)}
     */
    @Test
    public void testDefaultConstructor() throws PatriusException, IOException, ParseException {
        final Stela3StepsSolarActivity solarActivity = new Stela3StepsSolarActivity(reader);
        final AbsoluteDate startDate = new AbsoluteDate(DATE_1 - 10, TIME_SCALE);
        solarActivity.buildSolarActivity(startDate);
        Assert.assertEquals(new AbsoluteDate(), solarActivity.getDate1());
        Assert.assertEquals(new AbsoluteDate(), solarActivity.getDate2());
        Assert.assertEquals(1, solarActivity.getF107Coef(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(1, solarActivity.getApCoef(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertTrue(solarActivity.getSolarActivityCycles().isEmpty());
    }

    /**
     * Method tested : {@link Stela3StepsSolarActivity#toString()}
     */
    @Test
    public void testToString() {
        final String CR = System.lineSeparator();
        final String expected =
            "[ Solar Activity ]" + CR + " Solar Activity Type : MIXED_3DATE_RANGES" + CR + " Date 1 : "
                    + DATE_1 + "JJCNES" + CR + " Date 2 : " + DATE_2 + "JJCNES" + CR + " F10.7 coefficient : "
                    + F_107_COEF + CR + " Ap coefficient : " + AP_COEF + CR + " Cycles : [2, 4, 1, 3]" + CR;

        Assert.assertEquals(expected, defaultStela3StepsSolarActivity.toString());
    }

    /**
     * Method tested : {@link Stela3StepsSolarActivity#copy()}
     */
    @Test
    public void copy() throws PatriusException, IOException, ParseException {
        // Copy the default solar activity
        final Stela3StepsSolarActivity copy = defaultStela3StepsSolarActivity.copy();

        Assert.assertNotEquals(defaultStela3StepsSolarActivity, copy);
        Assert.assertNotEquals(defaultStela3StepsSolarActivity.getEntireAPMap(), copy.getEntireAPMap());

        // Build the copied solar activity
        final AbsoluteDate startDate = new AbsoluteDate(DATE_1 - 10, TIME_SCALE);
        copy.buildSolarActivity(startDate);

        Assert.assertEquals(defaultStela3StepsSolarActivity.getEntireAPMap(), copy.getEntireAPMap());
    }

    /**
     * Method tested : {@link Stela3StepsSolarActivity#getSolarActivityCycles()}
     */
    @Test
    public void getSolarActivityCycles() {
        final Integer[] expected = new Integer[] { 2, 4, 1, 3 };
        Assert.assertArrayEquals(expected, defaultStela3StepsSolarActivity.getSolarActivityCycles().toArray());
    }

    /**
     * Methods tested : <br>
     * - {@link Stela3StepsSolarActivity#getDate1()} <br>
     * - {@link Stela3StepsSolarActivity#getDate2()}
     */
    @Test
    public void getDates() {
        Assert.assertEquals(new AbsoluteDate(DATE_1, TIME_SCALE), defaultStela3StepsSolarActivity.getDate1());
        Assert.assertEquals(new AbsoluteDate(DATE_2, TIME_SCALE), defaultStela3StepsSolarActivity.getDate2());
    }

    /**
     * Method tested : {@link Stela3StepsSolarActivity#getF107Coef()}
     */
    @Test
    public void getF107Coef() {
        Assert.assertEquals(F_107_COEF, defaultStela3StepsSolarActivity.getF107Coef(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : {@link Stela3StepsSolarActivity#getApCoef()}
     */
    @Test
    public void getApCoef() {
        Assert.assertEquals(AP_COEF, defaultStela3StepsSolarActivity.getApCoef(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : {@link Stela3StepsSolarActivity#getSolarActivity(AbsoluteDate)}
     */
    @Test
    public void buildSolarActivity() throws PatriusException {
        final AbsoluteDate startDate = new AbsoluteDate(DATE_1 - 10, TIME_SCALE);
        Assert.assertEquals(92.76666666666667, defaultStela3StepsSolarActivity.getSolarActivity(startDate)[0],
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Write a file <br>
     *
     * Method tested : {@link Stela3StepsSolarActivity#writeToFile(String)}
     */
    @Test
    public void writeToFile() {
        defaultStela3StepsSolarActivity.writeToFile(RESOURCE_DIR + "target" + File.separator +
                "Stela3StepsSolarActivityOutput");
        // Manual check
        try {
            defaultStela3StepsSolarActivity.writeToFile(RESOURCE_DIR + "wrong" + File.separator +
                    "Stela3StepsSolarActivityOutput");
            Assert.fail();
        } catch (PatriusRuntimeException e) {
            Assert.assertEquals("STELA_SOLAR_ACTIVITY_FILE_SAVE_ERROR", e.getMessage());
        }
    }
}