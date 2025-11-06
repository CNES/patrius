package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import fr.cnes.sirius.patrius.Utils;
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
import java.text.ParseException;
import java.util.*;

/**
 * Class test for {@link StelaPastCyclesSolarActivity}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaPastCyclesSolarActivityTest {

    /**
     * Resource directory
     */
    private static final String RESOURCE_DIR =
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "stela"
                + File.separator + "solaractivity" + File.separator;

    /**
     * Common date for all tests
     */
    public static final int CNESJD = 17532;

    /**
     * Timescale constant for test
     */
    private static TimeScale TIME_SCALE;

    /**
     * Default past cycles solar activity used in a lot of tests
     */
    private static StelaPastCyclesSolarActivity defaultPastCyclesSolAct;

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

        final List<String> pathList = new ArrayList<>();
        final String dir = RESOURCE_DIR + "Solar_Activity_Cycles" + File.separator;
        pathList.add(dir + "CF_STELA_Cycle_1_def");
        pathList.add(dir + "CF_STELA_Cycle_2_def");
        pathList.add(dir + "CF_STELA_Cycle_3_def");
        pathList.add(dir + "CF_STELA_Cycle_4_def");
        pathList.add(dir + "CF_STELA_Cycle_5_def");
        pathList.add(dir + "CF_STELA_Cycle_6_def");
        StelaPastCycleSolarActivityProperties.setPastCycleFilePaths(pathList);

        // Set the timescale to UTC
        TIME_SCALE = TimeScalesFactory.getUTC();

        // Case without additional cycle
        StelaPastCyclesData stelaPastCyclesData = new StelaPastCyclesData(151, getCycles1(), false);
        defaultPastCyclesSolAct = new StelaPastCyclesSolarActivity(stelaPastCyclesData);
        final AbsoluteDate date = new AbsoluteDate(CNESJD, TimeScalesFactory.getUTC());
        defaultPastCyclesSolAct.buildSolarActivity(date);

    }

    /**
     * Get cycles1
     * 
     * @return cycles1
     */
    private static List<Integer> getCycles1() {
        // Case without additional cycle
        final List<Integer> cycles1 = new ArrayList<>();
        cycles1.add(2);
        cycles1.add(4);
        cycles1.add(1);
        cycles1.add(3);
        return cycles1;
    }

    /**
     * Compare the default solar activity values with the expected ones <br>
     *
     * Methods tested : <br>
     * - {@link StelaPastCyclesSolarActivity#buildSolarActivity(AbsoluteDate)} <br>
     * - {@link StelaPastCyclesSolarActivity#getSolarActivity(AbsoluteDate)}
     */
    @Test
    public void testPastCyclesFirstCycle() throws PatriusException, IOException, ParseException {

        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);

        Assert.assertEquals("Acsol day 1 no 1st cycle", 77.4, defaultPastCyclesSolAct.getSolarActivity(date)[0],
            Precision.DOUBLE_COMPARISON_EPSILON);

        // Case with additional cycle
        final List<Integer> cycles2 = new ArrayList<>();
        cycles2.add(2);
        cycles2.addAll(getCycles1());
        final StelaPastCyclesData data2 = new StelaPastCyclesData(151, cycles2, true);
        final StelaPastCyclesSolarActivity solarActivity2 = new StelaPastCyclesSolarActivity(data2);
        solarActivity2.buildSolarActivity(date);
        Assert.assertEquals("Acsol day 1 no 1st cycle", 77.4, solarActivity2.getSolarActivity(date)[0],
            Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Method tested : {@link StelaPastCyclesSolarActivity#toString()}
     */
    @Test
    public void testToString() {
        final String CR = System.lineSeparator();
        final String expected = "[ Solar Activity ]" + CR +
                " Solar Activity Type : RANDOM_CYCLES" + CR +
                " First day of first cycle : 151" + CR +
                " Cycles : [2, 4, 1, 3]" + CR;

        Assert.assertEquals(expected, defaultPastCyclesSolAct.toString());
    }

    /**
     * Method tested : {@link StelaPastCyclesSolarActivity#copy()}
     */
    @Test
    public void copy() throws PatriusException, IOException, ParseException {
        final StelaPastCyclesSolarActivity copy = defaultPastCyclesSolAct.copy();
        Assert.assertNotEquals(defaultPastCyclesSolAct, copy);

        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        try {
            // Try to get the copied solar activity without build. Must fail
            copy.getSolarActivity(date);
            Assert.fail();
        } catch (NullPointerException ignored) {
            // nothing to do
        }

        // Build the copied solar activity
        copy.buildSolarActivity(date);

        Assert.assertEquals("Acsol day 1 no 1st cycle", 77.4, copy.getSolarActivity(date)[0],
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : {@link StelaPastCyclesSolarActivity#getSolarActivityFirstDay()}
     */
    @Test
    public void getSolarActivityFirstDay() {
        Assert.assertEquals(151, defaultPastCyclesSolAct.getSolarActivityFirstDay());
    }

    /**
     * Compare the default cycle values with the expected ones <br>
     *
     * Method tested : {@link StelaPastCyclesSolarActivity#getSolarActivityCycles()}
     */
    @Test
    public void getSolarActivityCycles() {
        final List<Integer> actual = defaultPastCyclesSolAct.getSolarActivityCycles();
        Assert.assertEquals(2, (long) actual.get(0));
        Assert.assertEquals(4, (long) actual.get(1));
        Assert.assertEquals(1, (long) actual.get(2));
        Assert.assertEquals(3, (long) actual.get(3));
    }

    /**
     * Method tested : {@link StelaPastCyclesSolarActivity#isAdditionalCycle()}
     */
    @Test
    public void isAdditionalCycle() {
        Assert.assertFalse(defaultPastCyclesSolAct.isAdditionalCycle());
    }

    /**
     * Methods tested : <br>
     * - {@link StelaPastCyclesSolarActivity#emptyMaps()}<br>
     * - {@link StelaPastCyclesSolarActivity#getEntireAPMap()}
     */
    @Test
    public void emptyMaps() {
        defaultPastCyclesSolAct.emptyMaps();
        final NavigableMap<AbsoluteDate, Double> actualMap = defaultPastCyclesSolAct.getEntireAPMap();
        Assert.assertTrue(actualMap.isEmpty());

        final AbsoluteDate date = new AbsoluteDate(CNESJD, TIME_SCALE);
        defaultPastCyclesSolAct.buildSolarActivity(date);
    }

    /**
     * Reader test <br>
     *
     * Methods exception tested : <br>
     * - {@link StelaPastCyclesSolarActivityReader#loadData(InputStream, String)}<br>
     */
    @Test
    public void readerTest() throws IOException, ParseException {
        final List<String> pathList = new ArrayList<>();
        final String dir = RESOURCE_DIR + "target" + File.separator;
        pathList.add(dir + "Stela3StepsSolarActivityOutput");
        StelaPastCycleSolarActivityProperties.setPastCycleFilePaths(pathList);

        final StelaPastCyclesSolarActivityReader pastCyclesReader = new StelaPastCyclesSolarActivityReader();
        try {
            for (final InputStream file : StelaPastCycleSolarActivityProperties.getPastCycleFilePath()) {
                pastCyclesReader.loadData(file, "DUMMY");
            }
            Assert.fail();
        } catch (PatriusException e) {
            Assert.assertEquals("unexpected format error for file DUMMY with loader StelaPastCyclesSolarActivityReader",
                e.getMessage());
        }
    }
}
