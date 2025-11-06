package fr.cnes.sirius.patrius.stela.forces.drag;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.Squaring;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Cook drag coef test class.
 * 
 * @author Thibaut BONIT, Mathis GUILLEMETTE
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaCookDragCoefTest {

    /**
     * Default cook drag coefficient
     */
    public static final StelaCookDragCoef defaultCookDragCoef =
        new StelaCookDragCoef(Constants.STELA_COOK_WALL_TEMPERATURE, Constants.STELA_COOK_ACCOMODATION);

    /**
     * Default dispersed cook drag coefficient
     */
    public static final StelaCookDispersedDragCoef defaultCookDispersedDragCoef =
        new StelaCookDispersedDragCoef();

    /**
     * Default drag coefficient input
     */
    public static final StelaDragCoefInput defaultInput =
        new StelaDragCoefInput(null, 8017.324023600708, 1076.4750037195702, 0.002206892361980519);

    /**
     * Expected data from STELA CookDragCoefTest#testGetDragCoeffCIRF() test.<br>
     * Data: [Temperature ; Molar mass ; Drag coeff].
     */
    private final double[][] expectedStelaData = {
        { 1076.4750037195702, 0.002206892361980519, 3.1969681048861602 },
        { 1087.0056093408039, 0.0026547010875013132, 3.1003150418964966 },
        { 1096.785387188862, 0.003057395992943582, 3.0268731559439774 },
        { 1105.8701004897491, 0.0033726020342380553, 2.9747994501743276 },
        { 1114.34005313365, 0.003612481418873975, 2.937306315271602 },
        { 1122.2926086664836, 0.0038888838812205792, 2.898090485779736 },
        { 1129.8332941533947, 0.004658469513661551, 2.807286290872449 },
        { 1137.0652976247798, 0.007080813812012109, 2.5857781603914702 },
        { 1144.0721691209985, 0.011153779427472723, 2.3198390218234906 },
        { 1150.8590071253539, 0.014129848155684036, 2.173758850679002 },
        { 1157.1014572663166, 0.01551847805056739, 2.114834055459461 },
        { 1161.3471701857754, 0.01636426535983179, 2.093740150570962 },
        { 1159.7199810690465, 0.017253521827639864, 2.089870784734113 },
        { 1146.2563040642679, 0.018294810584239155, 2.0858029645750187 },
        { 1118.9143666134787, 0.019319334538011494, 2.082056639624063 },
        { 1088.4962355135913, 0.020078310749764985, 2.079372310968789 },
        { 1075.8716720436182, 0.020380348396785923, 2.07835857694552 },
        { 1092.7653979903234, 0.02015254906278269, 2.079266337799913 },
        { 1129.1069057833556, 0.01945680812118006, 2.0818807529434102 },
        { 1164.4824139363063, 0.018469601267635014, 2.085643606014894 },
        { 1186.5558913396696, 0.017429642736114654, 2.089848423103343 },
        { 1195.158760824735, 0.016526885587102323, 2.0938964730712684 },
        { 1195.181279902713, 0.015731935489917286, 2.1072916972690523 },
        { 1190.5999907168875, 0.014623696616633559, 2.1535781103330214 },
        { 1183.2721530324707, 0.012253583950823469, 2.263852816852237 },
        { 1173.8341608518976, 0.008254843545711543, 2.5010997607521634 },
        { 1162.5224641443333, 0.005096502526711459, 2.764975931665015 },
        { 1149.5188648140913, 0.003902582840678762, 2.8993212698793482 },
        { 1135.042177976424, 0.003479824406481454, 2.957310332121027 },
        { 1119.3602003493168, 0.0031493900518800564, 3.008424463426363 },
        { 1102.7830917844408, 0.002748660031593645, 3.0769588112075885 },
        { 1085.6522209860188, 0.0022879920273867773, 3.1689719807362633 },
        { 1068.3270988864062, 0.0018444687146615428, 3.280697938506296 }
    };

    /**
     * Set up method.
     */
    @Before
    public void setUp() throws PatriusException {

        Utils.clear();

        // Next line clears data set by other tests, are override later
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
    }

    /**
     * Test method for {@link StelaCookDragCoef#getDragCoef(StelaDragCoefInput)} .
     */
    @Test
    public void testGetDragCoef() {
        // final coefficient
        final StelaCookDragCoef cookDragCoef =
            new StelaCookDragCoef(Constants.STELA_COOK_WALL_TEMPERATURE, Constants.STELA_COOK_ACCOMODATION);
        final double dragCoeff = cookDragCoef.getDragCoef(defaultInput);
        Assert.assertEquals(3.1969681048861602, dragCoeff, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test method for {@link StelaCookDragCoef#getDragCoef(StelaDragCoefInput)} .
     */
    @Test
    public void testCopy() {
        final StelaCookDragCoef copy = defaultCookDragCoef.copy();
        Assert.assertNotEquals(defaultCookDragCoef, copy);
        Assert.assertEquals(defaultCookDragCoef.getDragCoef(defaultInput), copy.getDragCoef(defaultInput),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test method for : <br>
     * {@link StelaCookDragCoef#toString()} <br>
     * {@link StelaCookDispersedDragCoef#toString()} <br>
     * {@link StelaCookDispersedDragCoef#getStatInformation()}.
     */
    @Test
    public void testToString() {
        final String CR = System.lineSeparator();

        Assert.assertEquals("Drag Coefficient Type : COOK", defaultCookDragCoef.toString());
        Assert.assertEquals("Drag Coefficient Type : COOK_DISPERSED" + CR + "Dispersion coef : 1.0" + CR,
            defaultCookDispersedDragCoef.toString());
        Assert.assertEquals("1", defaultCookDispersedDragCoef.getStatInformation());
    }

    /**
     * Test method for {@link StelaCookDispersedDragCoef#getDragCoef(StelaDragCoefInput)} .
     */
    @Test
    public void testGetDispersedDragCoef() {
        // final coefficient
        final double coef = 2.;
        final StelaCookDispersedDragCoef cookDragCoef = new StelaCookDispersedDragCoef(coef);
        final double dragCoeff = cookDragCoef.getDragCoef(defaultInput);
        Assert.assertEquals(3.1969681048861602 * coef, dragCoeff, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test method for {@link StelaCookDispersedDragCoef#getDragCoef(StelaDragCoefInput)} .
     */
    @Test
    public void testDispersedCopy() {
        final StelaCookDispersedDragCoef copy = defaultCookDispersedDragCoef.copy();
        Assert.assertNotEquals(defaultCookDispersedDragCoef, copy);
        Assert.assertEquals(defaultCookDispersedDragCoef.getDragCoef(defaultInput), copy.getDragCoef(defaultInput),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test method for {@link StelaCookDragCoef#getDragCoef(StelaDragCoefInput)} and
     * {@link StelaCookDispersedDragCoef#getDragCoef(StelaDragCoefInput)} in real case.
     * <p>
     * This test is adapted from STELA CookDragCoefTest#testGetDragCoeffCIRF() test using PATRIUS objects, trying to be
     * as closed as possible to STELA's context.<br>
     * For instance, in STELA's test, the BulletinNatureConverter.toOsculating method configuration (with
     * ShortPeriodsSettings) use a third body attraction for the moon and the sun as well as a J7 zonal attraction for
     * the
     * Earth. This test also described these forces with PATRIUS models.
     * </p>
     * <p>
     * The results are compared to the reference values obtains with the STELA test (expected values).<br>
     * STELA reference version : 3.7
     * </p>
     * <p>
     * Two sources of errors between STELA and PATRIUS have been identified explaining the numerical differences :
     * <ul>
     * <li>The "toOsculating" method: add the force models derivatives contributions, but the force models are not
     * strictly the same between the two libraries</li>
     * <li>The "atmosModel.getData" (or the main computation method in the atmosphere model) is also not strictly
     * identical (MSISE2000 vs MSIS00Adapter)</li>
     * </ul>
     * </p>
     */
    @Test
    public void testGetDragCoefCIRFComparedToSTELA() throws PatriusException, IOException, ParseException {

        // Thresholds used to compare the results to the expected values
        final double threshTemp = 1.2511e-3;
        final double threshMolarMass = 1.2840e-8;
        final double threshDragCoeff = 8.5598e-7;

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();
        final CelestialBodyFrame tirf = FramesFactory.getTIRF();

        // Sun and moon
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);
        final CelestialBody moon = new MeeusMoonStela(Constants.CNES_STELA_AE);

        // Initial state
        final double sma = 24350500.0;
        final double e = 0.72887620377405;
        final double i = FastMath.toRadians(12.0);
        final double raan = FastMath.toRadians(30.0);
        final double w = FastMath.toRadians(30.0);
        final double m = FastMath.toRadians(50.0);
        final PositionAngle type = PositionAngle.MEAN;

        // Orbit construction
        final AbsoluteDate date = new AbsoluteDate(22370, TimeScalesFactory.getUTC());
        final KeplerianOrbit keplerianOrbit =
            new KeplerianOrbit(sma, e, i, w, raan, m, type, cirf, date, Constants.CNES_STELA_MU);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(keplerianOrbit);

        // Atmospheric model construction
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140., 15.);
        final MSISE2000 atmosModel = new MSISE2000(new ClassicalMSISE2000SolarData(solarActivity),
            new OneAxisEllipsoid(Constants.CNES_STELA_AE, 1 / 298.25765, tirf), sun);

        // Preparation
        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double eccentricity = FastMath.sqrt(ex * ex + ey * ey);
        final double za = (a * (1 + eccentricity)) - Constants.STELA_LOS_EARTH_RADIUS;

        // Computation of true and eccentric anomaly bounds of the part of the orbit inside the atmosphere
        final double[] bounds = computeAnomalyBounds(a, eccentricity, za);
        final double ve = bounds[0]; // True anomaly (lower bound)
        final double vs = bounds[1]; // True anomaly (upper bound)

        // Computation of squaring points (in mean parameters)
        final Squaring squaring = new Squaring();
        final double[][] squaringPV = squaring.computeSquaringPoints(33, orbit, ve, vs);

        // Get the orbit nature converter
        final OrbitNatureConverter converter = getOrbitNatureConverter(sun, moon);

        // Create cook dispersed drag coef
        final double dispersedCoef = 2.5;
        final StelaCookDispersedDragCoef cookDispersedDragCoef =
            new StelaCookDispersedDragCoef(dispersedCoef,
                Constants.STELA_COOK_WALL_TEMPERATURE, Constants.STELA_COOK_ACCOMODATION);

        // *** TEST ****
        final int squaringPoints = 33;
        // Drag evaluation for each squaring point (osculating parameters)
        for (int x = 0; x < squaringPoints; x++) {
            final AbsoluteDate squaringDate = squaring.getSquaringJDCNES()[x];

            final StelaEquinoctialOrbit orbitMean = new StelaEquinoctialOrbit(squaringPV[x][0], squaringPV[x][2],
                squaringPV[x][3], squaringPV[x][4], squaringPV[x][5], squaringPV[x][1], cirf,
                squaringDate, Constants.CNES_STELA_MU);

            final StelaEquinoctialOrbit orbitOsc = converter.toOsculating(orbitMean);
            final Vector3D position = orbitOsc.getPVCoordinates(cirf).getPosition();

            // Compute atmospheric physical quantities
            final AtmosphereData atmosData = atmosModel.getData(squaringDate, position, cirf);

            // Temperature computation
            final double temperature = atmosData.getLocalTemperature();

            // Molar mass computation
            final double molarMass =
                atmosData.getMeanAtomicMass() * (Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS);

            // final coefficient
            final PVCoordinates pvTIF = orbitOsc.getPVCoordinates(tirf);
            final double vTIRFNorm = pvTIF.getVelocity().getNorm();

            final StelaDragCoefInput input =
                new StelaDragCoefInput(pvTIF.getPosition(), vTIRFNorm, temperature, molarMass);

            final double dragCoef = defaultCookDragCoef.getDragCoef(input);
            final double defaultDragDispersedCoef = defaultCookDispersedDragCoef.getDragCoef(input);
            final double dragDispersedCoef = cookDispersedDragCoef.getDragCoef(input);

            Assert.assertEquals("Temperature " + x, this.expectedStelaData[x][0], temperature, threshTemp);
            Assert.assertEquals("Molar mass " + x, this.expectedStelaData[x][1], molarMass, threshMolarMass);

            Assert.assertEquals("Drag coeff " + x, this.expectedStelaData[x][2], dragCoef, threshDragCoeff);
            Assert.assertEquals("Drag coeff " + x, this.expectedStelaData[x][2], defaultDragDispersedCoef,
                threshDragCoeff);
            Assert.assertEquals("Drag coeff " + x, this.expectedStelaData[x][2] * dispersedCoef, dragDispersedCoef,
                threshDragCoeff * dispersedCoef);
        }
    }

    /**
     * Private method to get the orbit nature converter
     *
     * @param sun sun
     * @param moon moon
     * @return the orbit nature converter
     */
    private static OrbitNatureConverter getOrbitNatureConverter(CelestialBody sun, CelestialBody moon)
        throws IOException, ParseException, PatriusException {
        // SRP switch not used in the following conversion
        // Add forces
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final StelaZonalAttraction j7Attraction = new StelaZonalAttraction(provider, 7, true, 2, 0, false);
        final StelaThirdBodyAttraction sunForce = new StelaThirdBodyAttraction(sun, 4, 2, 0);
        final StelaThirdBodyAttraction moonForce = new StelaThirdBodyAttraction(moon, 4, 2, 0);
        final List<StelaForceModel> forces = Arrays.asList(j7Attraction, sunForce, moonForce);

        return new OrbitNatureConverter(forces);
    }

    /**
     * Private method copied from actual code to enable tests on cook drag coefficient.
     * 
     * @param a a
     * @param e e
     * @param za za
     * @return anomaly bounds
     */
    private static double[] computeAnomalyBounds(final double a, final double e, final double za) {

        final double[] result = new double[4];

        final double ve; // True anomaly (lower bound)
        final double vs; // True anomaly (upper bound)

        final double zbarre = Constants.STELA_Z_LIMIT_ATMOS;
        final double Rbarre = zbarre + Constants.STELA_LOS_EARTH_RADIUS;

        // Get semi-major axis and eccentricity (type 8)

        if (za > zbarre) {
            // Temporary correction
            double alpha = (a * (1. - (e * e)) / Rbarre - 1.) / e;
            final double epsilon = 1E-12;
            if (alpha > 1. && alpha < 1. + epsilon) {
                alpha = 1.;
            }

            // A part of the orbit is below the upper atmospheric boundary
            vs = FastMath.acos(alpha);
            ve = -vs;
        } else {
            // The orbit is entirely below the upper atmospheric boundary
            ve = 0.;
            vs = 2. * FastMath.PI;
        }

        result[0] = ve;
        result[1] = vs;
        return result;
    }
}
