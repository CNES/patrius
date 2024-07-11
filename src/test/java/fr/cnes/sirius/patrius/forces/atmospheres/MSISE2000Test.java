/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * @history Created 25/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2741:27/01/2021:[PATRIUS] Chaine de transformation de repere non optimale dans MSIS2000
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::FA:1196:15/11/2017:add getPressure() method
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.NRLMSISE00;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.frames.FactoryManagedFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980Entry;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980HistoryLoader;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.transformations.GTODProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link MSISE2000} class for the MSIS2000 Atmosphere model
 * 
 * @see NRLMSISE00
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: MSISE2000Test.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class MSISE2000Test {

    /**
     * Doubles comparison epsilon
     */
    private static final double EPS = 1e-14;
    /**
     * Adapter for MSIS2000
     */
    private MSISE2000 atmosModel;
    /**
     * date used for test
     */
    private AbsoluteDate date;
    /**
     * Frame used for test
     */
    private FactoryManagedFrame frame;
    /**
     * position vector
     */
    private Vector3D pos;
    /**
     * velocity vector
     */
    private Vector3D vel;
    /**
     * PVCoordinates
     */
    private PVCoordinates pv;
    /**
     * Turning MOD frame
     */
    private TMODFrame tf;
    /**
     * Earth
     */
    private OneAxisEllipsoid earth;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle MSIS2000 adapter tests
         * 
         * @featureDescription validate the adapter for the MSIS 2000 atmospheric model
         * 
         * @coveredRequirements DV-MOD_260
         */
        MSIS2000_ADAPTER
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MSISE2000Test.class.getSimpleName(), "MSISE2000 atmosphere");
    }

    /**
     * @throws PatriusException
     *         if couldnt load UTC-TAI history
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getDensity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description the test computes the density for a given date, position and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output a double representing the density at given point
     * 
     * @testPassCriteria if density is as expected
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @comments threshold is set to 10<sup>-14</sup> on a relative scale, machine precision. Reference was generated
     *           with validated SIRIUS version of the algorithm.
     */
    // @Test
    public void testAtmosphere() throws PatriusException {

        Utils.setDataRoot("atmosphere");
        TimeScalesFactory.getUTC();

        final SolarActivityDataProvider reader = SolarActivityDataFactory
            .getSolarActivityDataProvider();

        final ClassicalMSISE2000SolarData msis2000Data = new ClassicalMSISE2000SolarData(reader);
        // pv coordinates of test point
        final FactoryManagedFrame frame1 = FramesFactory.getMOD(false);
        final Vector3D pos1 = new Vector3D(new double[] { 8749870.287474481, -976409.8027621375, -1110696.1958653878 });
        final Vector3D vel1 = new Vector3D(new double[] { -2918.9280434522407, 7866.058181939492, 1758.4000069207825 });
        new PVCoordinates(pos1, vel1);

        // Thu Mar 31 22:16:55 GMT 2011
        final AbsoluteDate date1 = new AbsoluteDate(2011, 03, 31, 22, 16, 55.4778569, TimeScalesFactory.getUTC());

        // earth - stela values
        final Frame tf1 = new MSISE2000Test.TMODFrame(frame1);
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;
        final OneAxisEllipsoid earth1 = new OneAxisEllipsoid(ae, 1 / f, tf1);

        final CelestialBody sun = CelestialBodyFactory.getSun();
        final MSISE2000 atmosModel1 = new MSISE2000(msis2000Data, earth1, sun);

        final double density = atmosModel1.getDensity(date1, pos1, frame1);
        final double ref = 7.345799381787514E-17;

        Assert.assertEquals(0, MathLib.abs((density - ref) / MathLib.max(density, ref)), EPS);
    }

    /**
     * @throws PatriusException
     *         if couldnt load UTC-TAI history
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getDensity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description the test computes the density for a given date, position and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output a double representing the density at given point
     * 
     * @testPassCriteria if density is as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @comments reference is PATRIUS 4.2 since celestial bodies reference frame is now set to GCRF.
     */
    @Test
    public void testGetDensity() throws PatriusException {

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        Report.printMethodHeader("testGetDensity", "Density computation", "STELA", EPS, ComparisonType.RELATIVE);
        this.testSetup();
        final double density = this.atmosModel.getDensity(this.date, this.pos, this.frame);
        // Old STELA reference: 1.1905176552175417E-16;
        final double ref = 1.1905176215889153E-16;
        // test the exception when density is not properly computed:
        boolean rez = false;
        try {
            final Vector3D posZero = new Vector3D(0., 0., 0.);
            this.atmosModel.getDensity(this.date, posZero, this.frame);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        Assert.assertEquals(0, MathLib.abs((density - ref) / MathLib.max(density, ref)), EPS);

        Report.printToReport("Density", ref, density);
    }

    /**
     * @throws PatriusException
     *         if couldnt transform
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getVelocity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description the test computes the density for a given date, position and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output a Vector3D representing the velocity of atmosphere particles, expressed in the same frame as that of user
     *         given position
     * 
     * @testPassCriteria if velocity is as expected
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @comments threshold is set to 10<sup>-14</sup> on a relative scale
     */
    @Test
    public void testGetVelocity() throws PatriusException {
        this.testSetup();
        // reference velocity is the velocity in MOD Frame of (the static point in TMOD frame) that has pos as position
        // in MOD frame
        final Transform modToBody = this.frame.getTransformTo(this.earth.getBodyFrame(), this.date);
        final Vector3D posBody = modToBody.transformPVCoordinates(this.pv).getPosition();
        final PVCoordinates nPV = modToBody.getInverse().transformPVCoordinates(
            new PVCoordinates(posBody, Vector3D.ZERO));
        final Vector3D refVel = nPV.getVelocity();

        // computed velocity
        final Vector3D cmpVel = this.atmosModel.getVelocity(this.date, this.pos, this.frame);

        this.checkVectors(refVel, cmpVel, EPS);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getSpeedOfSound(date, position, frame)}
     * 
     * @description the test computes the speed of sound for given date, positions and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output speed of sound
     * 
     * @testPassCriteria if speed of sound is as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     */
    @Test
    public void testGetSpeedOfSound() throws PatriusException {

        this.testSetup();
        final Vector3D position = new Vector3D(6478000., 0., 0.);
        final double speedOfSound = this.atmosModel.getSpeedOfSound(this.date, position, this.frame);
        final double ref = 279.7687996868958;
        Assert.assertEquals(0, MathLib.abs((speedOfSound - ref) / MathLib.max(speedOfSound, ref)), EPS);

    }

    /**
     * Test that aims at showing the "cache" process of class {@link #MSISE2000} :
     * density is recomputed the method {@link MSISE2000#getDensity(AbsoluteDate, Vector3D, Frame)} is successively
     * called with different parameters.
     * 
     * @throws PatriusException
     *         if MSISE2000 altitude range is not in range 0 to 1000 km
     * @referenceVersion 3.2
     */
    @Test
    public void testRecomputed() throws PatriusException {

        this.testSetup();

        // Altitude
        final double alt1 = this.computeZ(20E3);
        final double alt2 = this.computeZ(40E3);

        // Geodetic point
        final GeodeticPoint gp1 = new GeodeticPoint(0, 0, alt1);
        final GeodeticPoint gp2 = new GeodeticPoint(0, 0, alt2);

        // Positions
        final Vector3D pos1 = this.earth.transform(gp1);
        final Vector3D pos2 = this.earth.transform(gp2);

        // GCRF
        final Frame gcrf = FramesFactory.getGCRF();

        // Check that the density depends on the frame used
        // so it is recomputed and leads to different results

        // When calling the following getDensity methods, the parameters in cache are :
        // (date, pos1, frame)
        final double density1_ITRF = this.atmosModel.getDensity(this.date, pos1, this.frame);

        // Recomputation occur here : parameters in cache are now :
        // (date1, pos1, gcrf), so results are different
        final double density1_GCRF = this.atmosModel.getDensity(this.date, pos1, gcrf);
        Assert.assertFalse(density1_GCRF == density1_ITRF);

        // Check also that values are recomputed if the position changes

        // (date1, pos2, frame) are now in cache
        final double density2 = this.atmosModel.getDensity(this.date, pos2, this.frame);

        // Values are updated so different from previous
        Assert.assertFalse(density1_ITRF == density2);

        // Finally, check that changing the dates leads to recomputation in GCRF
        // Same idea about parameters in cache
        final double density_otherDate = this.atmosModel.getDensity(this.date.shiftedBy(3600), pos1, gcrf);

        Assert.assertFalse(density1_GCRF == density_otherDate);

    }

    /**
     * Geodetic altitude from geopotential altitude
     * 
     * @param h
     *        Geopotential altitude
     * @return altitude
     */
    public double computeZ(final double h) {
        final double AE = 6356766;
        return AE * h / (AE - h);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getData(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description test computation of all available atmospheric data
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output {@link AtmosphereData}
     * 
     * @testPassCriteria sum of partial densities equals total density. Atmosphere data are as expected (reference:
     *                   scilab ms_msis2000 - threshold: 1E-13, limited due to PATRIUS 4.6 frame conversion
     *                   optimization)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testAtmosphereData() throws PatriusException {

        Utils.setDataRoot("MSIS2000-resources");

        // Initialization
        final SolarActivityDataProvider provider = new ConstantSolarActivity(140, 15);
        final MSISE2000InputParameters msis2000Data = new ClassicalMSISE2000SolarData(provider);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1 / 0.29825765000000E+03,
            FramesFactory.getGCRF());
        final CelestialBody sun = new MeeusSun();
        final MSISE2000 atmosModel = new MSISE2000(msis2000Data, earth, sun);
        final MSISE2000 atm2 = (MSISE2000) atmosModel.copy();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D pos = new Vector3D(new double[] { 8749870.287474481, -976409.8027621375, -1110696.1958653878 });

        // Computation
        final AtmosphereData data = atm2.getData(date, pos, FramesFactory.getGCRF());
        final double density = data.getDensity();
        final double localTemperature = data.getLocalTemperature();
        final double exosphericTemperature = data.getExosphericTemperature();
        final double densityHe = data.getDensityHe();
        final double densityO = data.getDensityO();
        final double densityN2 = data.getDensityN2();
        final double densityO2 = data.getDensityO2();
        final double densityAr = data.getDensityAr();
        final double densityH = data.getDensityH();
        final double densityN = data.getDensityN();
        final double densityAnomalousOxygen = data.getDensityAnomalousOxygen();
        final double meanAtomicMass = data.getMeanAtomicMass();

        // Check density = sum partial densities
        final double expected = densityHe + densityO + densityN2 + densityO2 + densityAr + densityH + densityN
                + densityAnomalousOxygen;
        Assert.assertEquals(0., (expected - density) / expected, 5E-16);

        // Non-regression
        Assert.assertEquals(8.64860338619198E-17, density, 1E-13);
        Assert.assertEquals(1217.1426613598985, localTemperature, 1E-13);
        Assert.assertEquals(1217.1426613599185, exosphericTemperature, 1E-13);
        Assert.assertEquals(0, (341717210107.43560791016E-28 - densityHe) / densityHe, 1E-13);
        Assert.assertEquals(0, (1899190.8634123941883445E-28 - densityO) / densityO, 1E-13);
        Assert.assertEquals(0, (0.0141708268323862962568E-28 - densityN2) / densityN2, 2E-13);
        Assert.assertEquals(0, (0.0000010500108651131238E-28 - densityO2) / densityO2, 2E-13);
        Assert.assertEquals(0, (1.183661569500849259E-41 - densityAr) / densityAr, 2E-13);
        Assert.assertEquals(0, (522482170858.56048583984E-28 - densityH) / densityH, 1E-13);
        Assert.assertEquals(0, (757899.08704671438317746E-28 - densityN) / densityN, 1E-13);
        Assert.assertEquals(0, (658300563.23720526695251E-28 - densityAnomalousOxygen) / densityAnomalousOxygen, 1E-13);
        Assert.assertEquals(0.0021967995676329355945 * 1000, meanAtomicMass, 1E-13);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSISE2000#getPressure(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description test computation of pressure
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output pressure
     * 
     * @testPassCriteria pressure is as expected (reference: Celestlab, threshold: 1E-6 due to the fact that Celestlab
     *                   uses simple precision)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testPressure() throws PatriusException {

        Utils.setDataRoot("regular-data");

        final FramesConfiguration configSvg = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Initialization
        final SolarActivityDataProvider provider = new ConstantSolarActivity(250, 25);
        final MSISE2000InputParameters msis2000Data = new ClassicalMSISE2000SolarData(provider);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1 / 0.29825765000000E+03,
            FramesFactory.getITRF());
        final CelestialBody sun = new MeeusSun();
        final MSISE2000 atmosModel = new MSISE2000(msis2000Data, earth, sun);
        final AbsoluteDate date = new AbsoluteDate("2008-01-01T16:25:00", TimeScalesFactory.getUTC());
        final Vector3D pos = earth.transform(new GeodeticPoint(0, 0, 600000));

        // Computation
        final double actual = atmosModel.getPressure(date, pos, FramesFactory.getITRF());
        // Perfect gas constant used in Celestlab is 8.314472
        final double expected = 1.478134189718436740E-06 * Constants.PERFECT_GAS_CONSTANT / 8.314472;

        // Check
        Assert.assertEquals(0, (expected - actual) / expected, 2E-6);

        FramesFactory.setConfiguration(configSvg);
    }

    /**
     * setup method
     * 
     * @throws PatriusException
     *         if data embedded in the library cannot be read
     */
    public void testSetup() throws PatriusException {

        Utils.setDataRoot("MSIS2000-resources");

        // Thu Mar 31 22:16:55 GMT 2011
        this.date = new AbsoluteDate(2011, 03, 31, 22, 16, 55.4778569, TimeScalesFactory.getUTC());

        final int mjd = (int) (this.date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / 86400);
        EOPHistoryFactory.clearEOP1980HistoryLoaders();
        EOPHistoryFactory.addEOP1980HistoryLoader(new EOP1980HistoryLoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) throws IOException,
                ParseException, PatriusException {
                throw new IOException();

            }

            @Override
            public void fillHistory(final EOP1980History history) throws PatriusException {

                history.addEntry(new EOP1980Entry(mjd - 3, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd - 2, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd - 1, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd + 1, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd + 2, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd + 3, 0, 0, 0, 0, 0, 0));
            }

        });

        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(Utils.getIERS2010Configuration());
        builder.setEOPHistory(new NoEOP2000History());
        FramesFactory.setConfiguration(builder.getConfiguration());

        // pv coordinates of test point
        this.frame = FramesFactory.getMOD(false);
        this.pos = new Vector3D(new double[] { 8749870.287474481, -976409.8027621375, -1110696.1958653878 });
        this.vel = new Vector3D(new double[] { -2918.9280434522407, 7866.058181939492, 1758.4000069207825 });
        this.pv = new PVCoordinates(this.pos, this.vel);

        // solar activity data
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140, 15);

        // earth - stela values
        this.tf = new TMODFrame(this.frame);
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;
        this.earth = new OneAxisEllipsoid(ae, 1 / f, this.tf);

        final CelestialBody sun = CelestialBodyFactory.getSun();
        // Atmospheric model a
        this.atmosModel = new MSISE2000(new ClassicalMSISE2000SolarData(solarActivity), this.earth, sun);

    }

    /**
     * @throws PatriusException
     *         MSISE2000 model creation
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final BodyShape earthBody = new OneAxisEllipsoid(Constants.CNES_STELA_AE,
            Constants.GRIM5C1_EARTH_FLATTENING,
            FramesFactory.getCIRF());
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL.act"));
        final MSISE2000InputParameters data = new ContinuousMSISE2000SolarData(
            SolarActivityDataFactory.getSolarActivityDataProvider());

        final MSISE2000 atm = new MSISE2000(data, earthBody, CelestialBodyFactory.getSun());
        Assert.assertTrue(data.equals(atm.getParameters()));
        Assert.assertTrue(earthBody.equals(atm.getEarthBody()));
        Assert.assertTrue(CelestialBodyFactory.getSun().equals(atm.getSunBody()));
    }

    /**
     * FA-2364: second call of msis result is wrong.
     * 
     * @testType UT
     * 
     * @description check that the second call of the msis model returns the expected value. The expected values have
     *              been obtained separately by calling only the second call.
     * 
     * @input the class parameters
     * 
     * @output pressure, temperature and density
     * 
     * @testPassCriteria the output value is as expected (reference: Debrisk values)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testSeveralCalls() throws PatriusException {
        // Adding Patrius Dataset
        Utils.setDataRoot("regular-dataPBASE");

        // De-activate EOP
        final FramesConfiguration config = FramesFactory.getConfiguration();
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(
            FramesConfigurationFactory.getIERS2003Configuration(false));
        builder.setEOPHistory(new NoEOP2000History());
        FramesFactory.setConfiguration(builder.getConfiguration());

        // First call (values are not checked)
        testAtmValues120km_01012012T120000();
        // Second call (values are checked)
        testAtmValues120km_01012000T120000_latlon();

        // Set back frames configuration
        FramesFactory.setConfiguration(config);
    }

    /**
     * First call.
     */
    private void testAtmValues120km_01012012T120000() throws PatriusException {

        // lon, lat and altitude
        final double lon = 0.0;
        final double lat = 0.0;
        final double altitude = 120e3;

        final Vector3D position = getPositionVector(lat, lon, altitude);
        final AbsoluteDate date = new AbsoluteDate("2012-01-01T12:00:00", TimeScalesFactory.getUTC());
        final FactoryManagedFrame frame = FramesFactory.getITRF();

        // Atmosphere object
        final double solarActivity[] = { 140.0, 48.0 };
        final MSISE2000 atmo = createAtmosphere(solarActivity);

        // Compute values
        atmo.getData(date, position, frame);
    }

    /**
     * Second call.
     */
    public void testAtmValues120km_01012000T120000_latlon() throws PatriusException {

        // lon, lat and altitude
        final double lon = MathLib.toRadians(230.0);
        final double lat = MathLib.toRadians(20.0);
        final double altitude = 120e3;

        final Vector3D position = getPositionVector(lat, lon, altitude);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final FactoryManagedFrame frame = FramesFactory.getITRF();

        // Atmosphere object
        final double solarActivity[] = { 140.0, 48.0 };
        final MSISE2000 atmo = createAtmosphere(solarActivity);
        final AtmosphereData atmData = atmo.getData(date, position, frame);

        // Obtained values
        final double temp = atmData.getLocalTemperature();
        final double pres = atmo.getPressure(date, position, frame);
        final double dens = atmData.getDensity();

        assertEquals(399.69913810257106, temp, 0);
        assertEquals(0.0019899540368167940, pres, 0);
        assertEquals(1.5242823809149666E-8, dens, 0);
    }

    /**
     * FA-2364: returned temperature is 0 below 72.5km of altitude.
     * 
     * @testType UT
     * 
     * @description Check the temperature below 72.5km is not 0.
     * 
     * @input the class parameters
     * 
     * @output temperature
     * 
     * @testPassCriteria the output value is > 0
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testTemperatureBelow72km() throws PatriusException {
        testSetup();

        // Altitude of 60km
        final Vector3D position = getPositionVector(0, 0, 60E3);
        final AbsoluteDate date = new AbsoluteDate("2012-01-01T12:00:00", TimeScalesFactory.getTAI());

        // Atmosphere object
        final double solarActivity[] = { 140.0, 48.0 };
        final MSISE2000 atmo = createAtmosphere(solarActivity);
        final AtmosphereData atmData = atmo.getData(date, position, FramesFactory.getGCRF());

        // Obtained values
        Assert.assertTrue(atmData.getLocalTemperature() > 0);
    }

    private MSISE2000 createAtmosphere(final double solarActivity[]) throws PatriusException {
        final SolarActivityDataProvider solarActivityDataProvider = new ConstantSolarActivity(solarActivity[0],
            solarActivity[1]);
        final MSISE2000InputParameters msise2000InputParameters = new ClassicalMSISE2000SolarData(
            solarActivityDataProvider);
        return new MSISE2000(msise2000InputParameters, getEllipsoid(), CelestialBodyFactory.getSun());
    }

    /**
     * Convert a geodetical point to cartesian components.
     * 
     * @param lat
     *        latitude.
     * @param lon
     *        longitude.
     * @param altitude
     *        altitude.
     * @return cartesian point.
     * 
     * @throws PatriusException
     */
    private Vector3D getPositionVector(final double lat, final double lon, final double altitude)
        throws PatriusException {
        final GeodeticPoint geoPoint = new GeodeticPoint(lat, lon, altitude);
        return getEllipsoid().transform(geoPoint);
    }

    private OneAxisEllipsoid getEllipsoid() throws PatriusException {
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(6378136.46, 1.0 / 298.257223563,
            FramesFactory.getITRF());
        return ellipsoid;
    }

    /**
     * MOD + Earth rotation Frame used for testing purposes only
     * 
     * @see GTODFrame
     * 
     * @author Rami Houdroge
     * 
     * @version $Id: MSISE2000Test.java 17911 2017-09-11 12:02:31Z bignon $
     * 
     * @since 1.2
     * 
     */
    private final class TMODFrame extends Frame {

        /** Serializable UID. */
        private static final long serialVersionUID = 6498012403263820054L;

        /**
         * Constructor from MODFrame
         * 
         * @param modFrame
         *        Mean of date frame, parent frame
         * @throws PatriusException
         *         if fails
         */
        private TMODFrame(final Frame modFrame) throws PatriusException {

            super(modFrame, new TMODTransformProvider(), "tmod");

        }

    }

    /**
     * {@link TransformProvider} for {@link TMODFrame}
     * 
     * @author tournebizej
     * 
     * @version $Id: MSISE2000Test.java 17911 2017-09-11 12:02:31Z bignon $
     * 
     * @since 1.2
     * 
     */
    private class TMODTransformProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 1919009513563694137L;

        @Override
        public Transform getTransform(final AbsoluteDate transformDate) throws PatriusException {
            return new Transform(transformDate, new Rotation(Vector3D.PLUS_K, GTODProvider.getGMST(transformDate,
                FramesConfigurationFactory.getStelaConfiguration())),
                new Vector3D(7.292115146706979e-5, Vector3D.PLUS_K));
        }

        /** {@inheritDoc} */
        @Override
        public Transform
            getTransform(final AbsoluteDate datee, final FramesConfiguration config)
                throws PatriusException {
            return this.getTransform(datee);
        }

        /**
         * 
         * @see fr.cnes.sirius.patrius.frames.transformations.TransformProvider#getTransform(fr.cnes.sirius.patrius.time.AbsoluteDate,
         *      boolean)
         */
        @Override
        public Transform
            getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives) throws PatriusException {
            return this.getTransform(date);
        }

        /**
         * 
         * @see fr.cnes.sirius.patrius.frames.transformations.TransformProvider#getTransform(fr.cnes.sirius.patrius.time.AbsoluteDate,
         *      fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration, boolean)
         */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives)
            throws PatriusException {
            return this.getTransform(date);
        }

    }

    /**
     * Check vectors are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param eps
     *        threshold
     */
    private void checkVectors(final Vector3D exp, final Vector3D act, final double eps) {
        Assert.assertEquals(exp.getX(), act.getX(), eps);
        Assert.assertEquals(exp.getY(), act.getY(), eps);
        Assert.assertEquals(exp.getZ(), act.getZ(), eps);
    }
}
