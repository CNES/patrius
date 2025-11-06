package fr.cnes.sirius.patrius.stela.forces.solaractivity.constant;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.stela.forces.drag.AbstractStelaDragCoef;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaConstantDragCoef;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaVariableDragCoef;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.AbstractStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.stela.spaceobject.StelaSpaceObject;
import fr.cnes.sirius.patrius.time.*;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class test for {@link StelaLOSConstantSolarActivity}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaLOSConstantSolarActivityTest {

    /**
     * Timescale constant for test
     */
    private static TimeScale TIME_SCALE;

    /**
     * Default LOS constant solar activity
     */
    private static StelaLOSConstantSolarActivity defaultSolarActivity;

    /**
     * Set up.
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        Utils.clear();

        // Next line clears data set by other tests, are overriden later
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

        // Create the default LOS solar activity
        defaultSolarActivity = new StelaLOSConstantSolarActivity();
    }

    /**
     * Compare the default solar activity with the expected data <br>
     *
     * Method tested : {@link StelaLOSConstantSolarActivity#getSolarActivity(AbsoluteDate)}
     */
    @Test
    public void testGetLosDefaultSolarActivity() {

        final AbsoluteDate date = new AbsoluteDate(22114.6848611111, TIME_SCALE);
        final double solarActivity_F107 = Constants.STELA_LOS_F107;
        final double solarActivity_AP = Constants.STELA_LOS_AP;
        final double[] expected =
            { solarActivity_F107, solarActivity_F107, solarActivity_AP, solarActivity_AP, solarActivity_AP,
                solarActivity_AP, solarActivity_AP, solarActivity_AP, solarActivity_AP };

        final double[] solActArray = defaultSolarActivity.getSolarActivity(date);

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
     * Compare F10.7 values and AP values with the expected ones <br>
     *
     * Method tested : <br>
     * - {@link StelaLOSConstantSolarActivity#updateF107(StelaSpaceObject, SpacecraftState)}<br>
     * - {@link StelaLOSConstantSolarActivity#computeLosF107(StelaSpaceObject, SpacecraftState)}
     */
    @Test
    public void testGetLosSolarActivity() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(22114.6848611111, TIME_SCALE);

        // Do not use the default LOS solar activity to not update its F10.7 coef
        final StelaLOSConstantSolarActivity solarActivity = new StelaLOSConstantSolarActivity();

        final double mass = 1000.0;
        final double dragArea = 10.0;
        final double reflectCoef = 0.5;
        final double constantDragCoeff = 0.8;
        final AbstractStelaDragCoef dragCoefObj = new StelaConstantDragCoef(constantDragCoeff);
        final StelaSpaceObject spaceObject =
            new StelaSpaceObject("SAT", mass, dragArea, reflectCoef, reflectCoef, dragCoefObj);

        final double za = 1992556.11892505;
        final double zp = 1714734.1225000303;

        final double a = (zp + za + 2 * Constants.CNES_STELA_AE) / 2;
        final double e = (za - zp) / (zp + za + 2 * Constants.CNES_STELA_AE);

        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, 1.32179520112547,
            0.87521451254795, 0.36451247785412, 5.23784105547862, PositionAngle.MEAN, FramesFactory.getICRF(), date,
            Constants.CNES_STELA_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Update flux
        solarActivity.updateF107(spaceObject, state);

        final double solarActivity_AP = 15.0;
        final double solarActivity_F107 = 126.97763472585537;
        final double[] expected = { solarActivity_F107, solarActivity_F107, solarActivity_AP, solarActivity_AP,
            solarActivity_AP,
            solarActivity_AP, solarActivity_AP, solarActivity_AP, solarActivity_AP };
        double[] solActArray;
        solActArray = solarActivity.getSolarActivity(date);

        Assert.assertEquals("SolarActivity[0]", expected[0], solActArray[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[1]", expected[1], solActArray[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[2]", expected[2], solActArray[2], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[3]", expected[3], solActArray[3], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[4]", expected[4], solActArray[4], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[5]", expected[5], solActArray[5], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[6]", expected[6], solActArray[6], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[7]", expected[7], solActArray[7], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals("SolarActivity[8]", expected[8], solActArray[8], Precision.DOUBLE_COMPARISON_EPSILON);

        // Create space objects
        final StelaConstantDragCoef nullDragCoef = new StelaConstantDragCoef(0);
        final StelaSpaceObject spaceObjectWithNullDrag =
            new StelaSpaceObject("SAT", mass, dragArea, reflectCoef, reflectCoef, nullDragCoef);
        final StelaVariableDragCoef variableDragCoef =
            new StelaVariableDragCoef(new HashMap<>(), new GeodPosition(0, 0));
        final StelaSpaceObject spaceObjectWithVariableDrag =
            new StelaSpaceObject("SAT", mass, dragArea, reflectCoef, reflectCoef, variableDragCoef);

        // Nothing should change with a null drag coefficient
        solarActivity.updateF107(spaceObjectWithNullDrag, state);
        Assert.assertEquals(126.97763472585537, solarActivity.getInstantFluxValue(null),
            Precision.DOUBLE_COMPARISON_EPSILON);

        // New flux value with variable drag coefficient
        solarActivity.updateF107(spaceObjectWithVariableDrag, state);
        Assert.assertEquals(130.18440961587612, solarActivity.getInstantFluxValue(null),
            Precision.DOUBLE_COMPARISON_EPSILON);

        // Nothing should change with a null drag coefficient
        solarActivity.updateF107(spaceObjectWithNullDrag, state);
        Assert.assertEquals(130.18440961587612, solarActivity.getInstantFluxValue(null),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link StelaLOSConstantSolarActivity#getInstantFluxValue(AbsoluteDate)}
     */
    @Test
    public void getInstantFluxValue() {
        Assert.assertEquals(Constants.STELA_LOS_F107, defaultSolarActivity.getInstantFluxValue(null),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link StelaLOSConstantSolarActivity#getAp(AbsoluteDate)}
     */
    @Test
    public void getAp() {
        Assert.assertEquals(Constants.STELA_LOS_AP, defaultSolarActivity.getAp(null),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link StelaLOSConstantSolarActivity#toString()}
     */
    @Test
    public void testToString() {
        final String CR = System.lineSeparator();

        Assert.assertEquals(
            "[ Solar Activity ]" + CR + " Solar Activity Type : MEAN_CONSTANT" + CR
                    + " AP Constant Equivalent Solar Activity : " + (int) Constants.STELA_LOS_AP + CR
                    + " F10.7 Constant Equivalent Solar " + "Activity : " + (int) Constants.STELA_LOS_F107 + CR,
            defaultSolarActivity.toString());
    }

    /**
     * Method tested : <br>
     * - {@link StelaLOSConstantSolarActivity#copy()}
     */
    @Test
    public void copy() throws PatriusException {
        final StelaLOSConstantSolarActivity copy = defaultSolarActivity.copy();
        Assert.assertNotEquals(defaultSolarActivity, copy);
        Assert.assertEquals(defaultSolarActivity.getInstantFluxValue(null), copy.getInstantFluxValue(null),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(defaultSolarActivity.getAp(null), copy.getAp(null), Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Method tested : <br>
     * - {@link AbstractStelaSolarActivity#getSolActType()}
     */
    @Test
    public void abstractClassTest (){
        Assert.assertEquals(StelaSolarActivityType.MEAN_CONSTANT, defaultSolarActivity.getSolActType());
    }
}
