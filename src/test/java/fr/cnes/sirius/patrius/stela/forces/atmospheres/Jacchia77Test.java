package fr.cnes.sirius.patrius.stela.forces.atmospheres;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.CheckUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.bodies.EarthRotation;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.IStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.variable.StelaVariableSolarActivity;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link Jacchia77} adapter class.
 * 
 * @author Thomas Rodrigues, Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * VERSION:4.16:OPENFD-390:25/04/2025:[STELA-PATRIUS] Modeles d'atmosphere additionnels
 * END-HISTORY
 * @since 4.16
 */
public class Jacchia77Test {

    /** Reference date from STELA test. */
    private static AbsoluteDate refDate;
    /** Reference date from STELA test. */
    private static AbsoluteDate dateT0m3Tau;
    /** Reference date from STELA test. */
    private static AbsoluteDate dateFMean;

    /**
     * SetUp.
     */
    @BeforeClass
    public static void setUpBeforeClass() {

        Utils.clear();

        // Next line clears data set by other tests
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Reference dates
        // We notice the reference date has to be shifted from 35s to get the same results as STELA ...
        refDate = new AbsoluteDate("2000-01-01T00:00:00.000", TimeScalesFactory.getTAI()).shiftedBy(35);
        dateT0m3Tau = new AbsoluteDate("1999-06-02T00:00:33.466", TimeScalesFactory.getTAI());
        dateFMean = new AbsoluteDate("2000-01-01T00:00:33.466", TimeScalesFactory.getTAI());
    }

    /**
     * Test methods :
     * <li>{@link Jacchia77#getData(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * <li>{@link Jacchia77#getDensity(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * <li>{@link Jacchia77#getTemperature(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * <li>{@link Jacchia77#getMeanMolarMass(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * <li>{@link Jacchia77#getVelocity(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * <li>{@link Jacchia77#getSpeedOfSound(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * <li>{@link Jacchia77#getSolarActivity()}</li>
     * <li>{@link Jacchia77#getEarthBody()}</li>
     * <li>{@link Jacchia77#getSun()}</li>
     * <li>{@link Jacchia77#copy()}</li>
     * <li>{@link Jacchia77#checkSolarActivityData(AbsoluteDate, AbsoluteDate)}</li>
     * 
     * @description The density, temperature and mean molar mass values are validated against reference values coming
     *              from STELA equivalent test.<br>
     *              STELA reference version : 3.7-SNAPSHOT<br>
     *              Some other basic methods are tested here.
     * 
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testComputeDensity() throws PatriusException {

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // *** CONFIGURATION ****

        final IStelaSolarActivity solarActivity = new FakeSolarActivity(162., 19.);
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        // Set up coordinates : latitude, longitude, altitude
        final double latitude = MathLib.PI / 3;
        final double longitude = MathLib.PI / 4 + EarthRotation.getERA(refDate);
        final double altitude = 300e3;
        final EllipsoidPoint point =
            earth.buildPoint(LLHCoordinatesSystem.ELLIPSODETIC, latitude, longitude, altitude, "");
        final Vector3D position = point.getPosition();

        // Actual values computes
        final JacchiaOutput atmosData = atmosModel.getData(refDate, position, cirf);
        final double actualDensity = atmosData.getDensity();
        final double actualTemp = atmosData.getTemperature();
        final double actualMolarMass = atmosData.getMeanMolarMass();

        // Use the same relative tolerance values and reference data used in STELA test
        final double[] relTol = { 5e-4, 6e-6, 1e-5 };
        final double[] refQuantities = { 1.705781426859555374E-11, 919.9065821244574863, 17.60579479541867443 };

        CheckUtils.checkEquality(refQuantities[0], actualDensity, 1e-14, relTol[0]);
        CheckUtils.checkEquality(refQuantities[1], actualTemp, 7e-3, relTol[1]);
        CheckUtils.checkEquality(refQuantities[2], actualMolarMass, 2e-4, relTol[2]);

        /*
         * Extra validation :
         * Re-use the test initialized models to check the basic getters and methods for coverage purposes
         */

        // Test the others atmosphere model getter methods to access the same data
        final double actualDensity2 = atmosModel.getDensity(refDate, position, cirf);
        final double actualTemp2 = atmosModel.getTemperature(refDate, position, cirf);
        final double actualMolarMass2 = atmosModel.getMeanMolarMass(refDate, position, cirf);

        Assert.assertEquals(actualDensity2, actualDensity, 0.);
        Assert.assertEquals(actualTemp2, actualTemp, 0.);
        Assert.assertEquals(actualMolarMass2, actualMolarMass, 0.);

        // Test the getVelocity method
        final Transform bodyToFrame = earth.getBodyFrame().getTransformTo(cirf, refDate);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, Vector3D.ZERO);
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        final Vector3D expectedVelocity = pvFrame.getVelocity();
        Assert.assertEquals(expectedVelocity, atmosModel.getVelocity(refDate, position, cirf));

        // Test the getSpeedOfSound method
        final double expectedSpeedOfSound = MathLib.sqrt(1.4 * 287.058 * actualTemp);
        Assert.assertEquals(expectedSpeedOfSound, atmosModel.getSpeedOfSound(refDate, position, cirf), 0.);

        // Test the basic getters
        Assert.assertEquals(solarActivity, atmosModel.getSolarActivity());
        Assert.assertEquals(earth, atmosModel.getEarthBody());
        Assert.assertEquals(sun, atmosModel.getSun());

        // Test the copy method
        final Jacchia77 copy = (Jacchia77) atmosModel.copy();
        Assert.assertEquals(solarActivity, copy.getSolarActivity());
        Assert.assertEquals(earth, copy.getEarthBody());
        Assert.assertEquals(sun, copy.getSun());

        // Test that from a fresh atmospheric model ("copy" hasn't any initialized cache), we can compute the density
        // first (without the temp/molar mass), then the temp & molar mass and the data are well computed (cache
        // mechanism works well)
        copy.getDensity(refDate, position, cirf);
        final double actualTemp3 = copy.getTemperature(refDate, position, cirf);
        final double actualMolarMass3 = copy.getMeanMolarMass(refDate, position, cirf);

        Assert.assertEquals(actualTemp3, actualTemp, 0.);
        Assert.assertEquals(actualMolarMass3, actualMolarMass, 0.);

        // Test the checkSolarActivityData method, for coverage only (nothing expected)
        atmosModel.checkSolarActivityData(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);

    }

    /**
     * Test methods :
     * <li>{@link Jacchia77#getData(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * 
     * @description This test has multiple purposes:
     *              <ul>
     *              <li>1) Verify that no anomalies are encountered on the altitude interval [0, 2500] km</li>
     *              <li>2) Generate data for comparison with MSLIB references if needed</li>
     *              <li>3) Check that the density is always positive</li>
     *              </ul>
     * 
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testComputeDensityMultiple() throws PatriusException {

        final int testSize = 2500;

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // *** CONFIGURATION ****
        final IStelaSolarActivity solarActivity = new StelaConstantSolarActivity(140., 15.);
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        double altitude = 0.;
        double actualDensity;
        
        // Compute the density for each altitude from 0 to "testSize * 100m"
        for (int i = 0; i < testSize; i++) {
            final Vector3D position = earth.buildPoint(new Vector3D(ae + altitude, 0., 0.), "").getPosition();
            actualDensity = atmosModel.getData(refDate, position, cirf).getDensity();
            altitude += 100.;
            
            // Check that the density is always positive
            Assert.assertTrue(actualDensity > 1e-17);
        }
    }

    /**
     * Test method for private method to compute F mean value in {@link Jacchia77}.
     *
     * @description Using a given solar activity, this test aims at validating the computation of the mean flux
     *              (weighted sum formula). The input solar activity form and reference value for the mean flux come
     *              from CNES.
     * 
     * @throws IllegalAccessException
     *         if this {@code Method} object is enforcing Java language access control and the underlying method is
     *         inaccessible
     * @throws IllegalArgumentException
     *         if the method is an instance method and the specified object argument is not an instance of the class or
     *         interface declaring the underlying method (or of a subclass or implementor thereof); if the number of
     *         actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if,
     *         after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter
     *         type by a method invocation conversion.
     * @throws InvocationTargetException
     *         if the underlying method throws an exception
     * @throws NoSuchMethodException
     *         if a matching method is not found
     * @throws SecurityException
     *         if the request is denied.
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testFMeanComputation()
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, PatriusException,
        NoSuchMethodException, SecurityException {

        final Method m = Jacchia77.class.getDeclaredMethod("computeMeanFlux", AbsoluteDate.class);
        m.setAccessible(true);

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // Solar activity
        final IStelaSolarActivity solarActivity = new FMeanSolarActivity(0., 0.);

        // atmospheric model
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        // Mean flux compute on 03/01/2001
        final AbsoluteDate tMeanFlux =
            new AbsoluteDate(new DateComponents(2001, 01, 03), TimeScalesFactory.getTAI()).shiftedBy(35);

        // Compute mean flux
        final double fMean = (Double) m.invoke(atmosModel, tMeanFlux);

        // Reference flux
        final double refMeanFlux = 141.64184;

        // Compare the result to the reference
        Assert.assertEquals(refMeanFlux, fMean, 1e-5);

        /*
         * Evaluate the "computeMeanFlux" method with a StelaVariableSolarActivity (coverage of this specific case)
         */

        final ACSOLFormatReader reader = new ACSOLFormatReader(".txt");
        final String RESOURCE_DIR = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "stela" + File.separator + "solaractivity" + File.separator;
        try {
            reader.loadData(Files.newInputStream(Paths.get(RESOURCE_DIR + "stela_solar_activity")), "DUMMY");
        } catch (IOException | ParseException | PatriusException e) {
            // Not expected
            Assert.fail();
        }

        final StelaVariableSolarActivity solarActivityVariable = new StelaVariableSolarActivity(reader);

        final Jacchia77 atmosModelBis = new Jacchia77(solarActivityVariable, earth, sun);
        final double fMeanBis = (Double) m.invoke(atmosModelBis, tMeanFlux);
        final double expectedFMeanBis = 168.78222010115203; // Non regression value
        Assert.assertEquals(expectedFMeanBis, fMeanBis, 0.);
    }

    /**
     * Test methods :
     * <li>{@link Jacchia77#getData(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * 
     * @description Test out of bound in case the height is < 0 km. This test ensures that the computed density,
     *              temperature and mean molar mass for altitude < 0 are exactly the same than the one computed at
     *              altitude = 0.
     * 
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testAltitudeOutOfBoundMin() throws PatriusException {

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // Solar activity & Jacchia77 atmospheric model
        final IStelaSolarActivity solarActivity = new StelaConstantSolarActivity(140., 15.);
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        // Altitude = 0
        final EllipsoidPoint pointAltMin = earth.buildPoint(new Vector3D(ae, 0., 0.), "");
        final Vector3D positionAltMin = pointAltMin.getPosition();

        final JacchiaOutput outputAltMin = atmosModel.getData(refDate, positionAltMin, cirf);
        final double densityAltMin = outputAltMin.getDensity();
        final double tempAltMin = outputAltMin.getTemperature();
        final double molarMassAltMin = outputAltMin.getMeanMolarMass();

        // Negative altitude
        final EllipsoidPoint pointAltNegative = earth.buildPoint(new Vector3D(ae - 10., 0., 0.), "");
        final Vector3D positionAltNegative = pointAltNegative.getPosition();

        final JacchiaOutput outputAltNegative = atmosModel.getData(refDate, positionAltNegative, cirf);
        final double densityAltNegative = outputAltNegative.getDensity();
        final double tempAltNegative = outputAltNegative.getTemperature();
        final double molarMassAltNegative = outputAltNegative.getMeanMolarMass();

        // Comparisons
        Assert.assertEquals(densityAltMin, densityAltNegative, 0.);
        Assert.assertEquals(tempAltMin, tempAltNegative, 0.);
        Assert.assertEquals(molarMassAltMin, molarMassAltNegative, 0.);
    }

    /**
     * Test methods :
     * <li>{@link Jacchia77#getData(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * 
     * @description Test out of bound in case the height is > 2500 km. This test ensures that the computed density is
     *              0, temperature and mean molar mass are exactly the same than the one computed at altitude = 2500km.
     * 
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testComputeDensityOutOfBoundMax() throws PatriusException {

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // Solar activity & Jacchia77 atmospheric model
        final IStelaSolarActivity solarActivity = new StelaConstantSolarActivity(140., 15.);
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        // Maximum altitude = 2500 km
        final double altitudeMax = 2500e3;
        final EllipsoidPoint pointAltMax = earth.buildPoint(new Vector3D(ae + altitudeMax, 0., 0.), "");
        final Vector3D positionAltMax = pointAltMax.getPosition();

        final JacchiaOutput outputAltMax = atmosModel.getData(refDate, positionAltMax, cirf);
        final double tempAltMax = outputAltMax.getTemperature();
        final double molarMassAltMax = outputAltMax.getMeanMolarMass();

        // Altitude > 2500 km (over maximum)
        final double altitudeOverMax = 3000e3; // Maximum altitude = 2500 km
        final EllipsoidPoint pointAltOverMax = earth.buildPoint(new Vector3D(ae + altitudeOverMax, 0., 0.), "");
        final Vector3D positionAltOverMax = pointAltOverMax.getPosition();

        final JacchiaOutput outputAltOverMax = atmosModel.getData(refDate, positionAltOverMax, cirf);
        final double densityAltOverMax = outputAltOverMax.getDensity();
        final double tempAltOverMax = outputAltOverMax.getTemperature();
        final double molarMassAltOverMax = outputAltOverMax.getMeanMolarMass();

        // Comparisons
        Assert.assertEquals(0., densityAltOverMax, 0.);
        Assert.assertEquals(tempAltMax, tempAltOverMax, 0.);
        Assert.assertEquals(molarMassAltMax, molarMassAltOverMax, 0.);

        // Test the special case (for coverage) when the altitude over maximum is used to compute only the density with
        // a new atmospheric model (no cache initialized) : should cover a quick escape branch
        final Jacchia77 copy = (Jacchia77) atmosModel.copy();
        final double densityAltOverMaxBis = copy.getDensity(refDate, positionAltOverMax, cirf);

        Assert.assertEquals(0., densityAltOverMaxBis, 0.);
    }

    /**
     * Test methods :
     * <li>{@link Jacchia77#getData(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * 
     * @description Test out of range in case exospheric temperature is < 200 K. This test ensure that an exception is
     *              raised for density, temperature and mean molar mass computation : indeed, the exception is raised
     *              first at density computation and because the same method is called for temperature and molar mass
     *              computation, it is only necessary the call the computeDensity() method.
     * 
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testTemperatureOutOfMinBound() throws PatriusException {

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // Solar activity & Jacchia77 atmospheric model
        final IStelaSolarActivity solarActivity = new StelaConstantSolarActivity(0., 1.);
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        // Set up coordinates
        final double altitude = 300e3;
        final EllipsoidPoint point = earth.buildPoint(new Vector3D(0., 0., ae + altitude), "point");
        final Vector3D position = point.getPosition();

        // An exception must be raised here : for density, temperature and also mean molar mass computation !
        // Test to build a measurement list with a null attribute (should fail)
        try {
            atmosModel.getData(refDate, position, cirf);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected (check the message is enriched as expected with the parameter information)
            Assert.assertTrue(e.getMessage().contains("8"));
        }
    }

    /**
     * Test methods :
     * <li>{@link Jacchia77#getData(AbsoluteDate, Vector3D, fr.cnes.sirius.patrius.frames.Frame)}</li>
     * 
     * @description Test out of range in case exospheric temperature is > 3000 K. This test ensure that an exception
     *              is raised for density, temperature and mean molar mass computation : indeed, the exception is raised
     *              first at density computation and because the same method is called for temperature and molar mass
     *              computation, it is only necessary the call the computeDensity() method.
     * 
     * @throws PatriusException
     *         if a Patrius error occurs
     */
    @Test
    public void testTemperatureOutOfMaxBound() throws PatriusException {

        final CelestialBodyFrame cirf = FramesFactory.getCIRF();

        // Sun
        final CelestialBody sun = new MeeusSun(MeeusSun.MODEL.STELA);

        // Solar activity & Jacchia77 atmospheric model
        final IStelaSolarActivity solarActivity = new StelaConstantSolarActivity(800., 410.);
        final double ae = Constants.CNES_STELA_AE;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / 298.25765, cirf);
        final Jacchia77 atmosModel = new Jacchia77(solarActivity, earth, sun);

        // Set up coordinates
        final double altitude = 300e3;
        final EllipsoidPoint point = earth.buildPoint(new Vector3D(0., 0., ae + altitude), "point");
        final Vector3D position = point.getPosition();

        // An exception must be raised here : for density, temperature and also mean molar mass computation !
        // Test to build a measurement list with a null attribute (should fail)
        try {
            atmosModel.getData(refDate, position, cirf);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected (check the message is enriched as expected with the parameter information)
            Assert.assertTrue(e.getMessage().contains("3,074"));
        }
    }

    // Utilitary class
    private class StelaConstantSolarActivity implements IStelaSolarActivity {

        /** Solar activity data. */
        protected final double[] solarActArray = { 0., 0., 0., 0., 0., 0., 0. };
        /** Instant flux. */
        protected final double instant;

        /**
         * Constructor for fake solar activity
         * 
         * @param f107
         *        instant flux
         * @param ap
         *        ap value
         */
        public StelaConstantSolarActivity(final double f107, final double ap) {
            this.instant = f107;
            this.solarActArray[0] = f107;
            this.solarActArray[2] = ap;
        }

        /** {@inheritDoc} */
        @Override
        public double getInstantFluxValue(final AbsoluteDate date) {
            return this.solarActArray[0];
        }

        /** {@inheritDoc} */
        @Override
        public double getAp(final AbsoluteDate date) {
            return this.solarActArray[2];
        }

        /** {@inheritDoc} */
        @Override
        public double[] getSolarActivity(final AbsoluteDate date) {
            return this.solarActArray;
        }

        /** {@inheritDoc} */
        @Override
        public StelaSolarActivityType getSolActType() {
            return StelaSolarActivityType.MEAN_CONSTANT;
        }

        /** {@inheritDoc} */
        @Override
        public IStelaSolarActivity copy() {
            return null;
        }
    }

    /**
     * Fake solar activity used for the validation test on quantities computation purpose.
     */
    private class FakeSolarActivity extends StelaConstantSolarActivity {

        /**
         * Constructor for fake solar activity.
         * 
         * @param f107
         *        instant flux
         * @param ap
         *        ap value
         */
        public FakeSolarActivity(final double f107, final double ap) {
            super(f107, ap);
        }

        /**
         * {@inheritDoc}
         * 
         * <p>
         * Fake implementation from STELA equivalent test.
         * </p>
         */
        @Override
        public double getInstantFluxValue(final AbsoluteDate date) {

            if (MathLib.abs(date.durationFrom(dateT0m3Tau)) < 1e-3) { // compare dates with 1ms tolerancy
                this.solarActArray[0] = ((140 * 125.84156512466222) - this.instant) / 1.2340980408667956E-4;
            } else if (MathLib.abs(date.durationFrom(dateFMean)) < 1e-3) {
                this.solarActArray[0] = this.instant;
            } else {
                this.solarActArray[0] = 0.;
            }
            return this.solarActArray[0];
        }

        /**
         * {@inheritDoc}
         * 
         * <p>
         * Fake implementation from STELA equivalent test.
         * </p>
         */
        @Override
        public double[] getSolarActivity(final AbsoluteDate date) {
            getInstantFluxValue(date); // Update solarActArray array
            return this.solarActArray;
        }
    }

    /**
     * Solar activity for mean flux computation test.
     */
    private class FMeanSolarActivity extends StelaConstantSolarActivity {

        /**
         * Constructor for solar activity for mean flux.
         * 
         * @param f107
         *        instant flux
         * @param ap
         *        ap value
         */
        public FMeanSolarActivity(final double f107, final double ap) {
            super(f107, ap);
        }

        /**
         * {@inheritDoc}
         * 
         * <p>
         * Fake implementation from STELA equivalent test.
         * </p>
         */
        @Override
        public double getInstantFluxValue(final AbsoluteDate date) {
            this.solarActArray[0] = 140. + 40. * MathLib.sin(date.durationFrom(refDate) / (Constants.JULIAN_DAY * 24.));
            return this.solarActArray[0];
        }

        /**
         * {@inheritDoc}
         * 
         * <p>
         * Fake implementation from STELA equivalent test.
         * </p>
         */
        @Override
        public double[] getSolarActivity(final AbsoluteDate date) {
            getInstantFluxValue(date); // Update solarActArray array
            return this.solarActArray;
        }
    };
}
