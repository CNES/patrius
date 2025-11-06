package fr.cnes.sirius.patrius.stela.forces.solaractivity.constant;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.IStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class test for {@link StelaConstantSolarActivity}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaConstantSolarActivityTest {

    /**
     * F10.7 value
     */
    public static final int F_107 = 140;

    /**
     * AP value
     */
    public static final int AP_COEF = 15;

    /**
     * Default constant solar activity
     */
    private static StelaConstantSolarActivity defaultSolarActivity;

    /**
     * Set up.
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        Utils.clear();

        // Next line clears data set by other tests, are override later
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

        defaultSolarActivity = new StelaConstantSolarActivity(F_107, AP_COEF);
    }

    /**
     * Compare the default solar activity with the expected values <br>
     *
     * Method tested : <br>
     * - {@link StelaConstantSolarActivity#getSolarActivity(AbsoluteDate)}
     */
    @Test
    public void getSolarActivity() throws PatriusException {
        final double[] expected = new double[] { F_107, F_107, AP_COEF, AP_COEF, AP_COEF, AP_COEF, AP_COEF, AP_COEF,
            AP_COEF };
        final double[] solActArray = defaultSolarActivity.getSolarActivity(null);

        Assert.assertEquals("SolarActivity[0]", expected[0], solActArray[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[1]", expected[1], solActArray[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[2]", expected[2], solActArray[2], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[3]", expected[3], solActArray[3], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[4]", expected[4], solActArray[4], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[5]", expected[5], solActArray[5], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[6]", expected[6], solActArray[6], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[7]", expected[7], solActArray[7], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[8]", expected[8], solActArray[8], Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link StelaConstantSolarActivity#getSolActType()}
     */
    @Test
    public void getSolActType() {
        Assert.assertEquals(StelaSolarActivityType.CONSTANT, defaultSolarActivity.getSolActType());
    }

    /**
     * Method tested : <br>
     * - {@link StelaConstantSolarActivity#copy()}
     */
    @Test
    public void copy() throws PatriusException, IOException, ParseException {
        final StelaConstantSolarActivity copy = defaultSolarActivity.copy();
        Assert.assertNotEquals(defaultSolarActivity, copy);
        Assert.assertEquals(defaultSolarActivity.getInstantFlux(null), copy.getInstantFlux(null),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(defaultSolarActivity.getAp(null), copy.getAp(null), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link IStelaSolarActivity#setConstantF107(double)}<br>
     * - {@link IStelaSolarActivity#setConstantAP(double)}
     */
    @Test
    public void interfaceTest(){
        try{
            defaultSolarActivity.setConstantF107(0);
            Assert.fail();
        } catch (PatriusException e){
            Assert.assertEquals("Stela solar activity : error set constant F10.7", e.getMessage());
        }

        try{
            defaultSolarActivity.setConstantAP(0);
            Assert.fail();
        } catch (PatriusException e){
            Assert.assertEquals("Stela solar activity : error set constant AP", e.getMessage());
        }
    }
}