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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:567:04/04/2016:Link budget correction
 * VERSION::FA:652:28/09/2016:Link budget correction finalisation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.properties.RFAntennaProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitude;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.sensor.RFVisibilityDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980Entry;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980HistoryLoader;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.RFStationAntenna;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvHermite;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.PVCoordinatesPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit and validation tests for the RFLinkBudgetModel class.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFLinkBudgetTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle RF link budget model
         * 
         * @featureDescription RF link budget model validation
         * 
         * @coveredRequirements DV-VEHICULE_330, DV-VISI_50
         */
        RF_LINK_BUDGET_MODEL
    }

    /** Shift between TAI and TUC. */
    private static double dt;

    /** Mu. */
    private static double mu = 0.398600442000000E+15;

    /** Frame. */
    private static Frame frame = FramesFactory.getEME2000();

    /** Orbit ephemeris. */
    private static EphemerisPvHermite ephemeris;

    /** Attitude law. */
    private static TabulatedAttitude attitudeLaw;

    /** Satellite. */
    private static Assembly satellite;

    /** Station. */
    private static RFStationAntenna station;

    /** Property for Z+ antenna. */
    private static RFAntennaProperty propertyzPlus;

    /** Property for Z- antenna. */
    private static RFAntennaProperty propertyzMoins;

    /** Link budget for Z+ antenna. */
    private static RFLinkBudgetModel linkBudgetModelZplus;

    /** Link budget for Z- antenna. */
    private static RFLinkBudgetModel linkBudgetModelZMoins;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RF_LINK_BUDGET_MODEL}
     * 
     * @testedMethod {@link RFLinkBudgetModel#computeLinkBudget(AbsoluteDate)}
     * 
     * @description test the link budget computation (gain, ellipticity and link budget) with Z+ and Z- antennas
     * 
     * @input ephemeris, the RF link budget model parameters
     * 
     * @output gain, ellipticity and link budget
     * 
     * @testPassCriteria results are the expected ones (absolute threshold: 1E-3dB for link budget, 1E-14 for gain and
     *                   ellipticity, reference: CMSG).
     *                   Remaining differences come from:
     *                   - Orbit ephemeris interpolation (30m)
     *                   - Light speed (1E-6dB)
     *                   - Term not included in PATRIUS link budget computation (1E-3dB)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testLinkBudget() throws PatriusException {

        Report.printMethodHeader("testLinkBudget", "Link budget", "CMSG", 1E-3, ComparisonType.ABSOLUTE);

        // TU with Z+ antenna
        final AbsoluteDate datep = AbsoluteDate.FIFTIES_EPOCH_UTC.shiftedBy(22935.2485062961 * 86400 + dt);
        final PVCoordinates pvp = ephemeris.getPVCoordinates(datep, FramesFactory.getEME2000());
        final Orbit orbitp = new CartesianOrbit(pvp, FramesFactory.getEME2000(), datep, mu);
        final Attitude attitudep = attitudeLaw.getAttitude(orbitp);
        final SpacecraftState statep = new SpacecraftState(orbitp, attitudep);
        satellite.updateMainPartFrame(statep);

        // Check gain, ellipticity and link budget
        final double actualLBp = linkBudgetModelZplus.computeLinkBudget(datep);
        final double actualGainp = propertyzPlus.getGain(MathLib.toRadians(73.9243376404399),
            MathLib.toRadians(29.6004319784673));
        final double actualEllipticityp = MathLib
            .pow(10.,
                propertyzPlus.getEllipticity(MathLib.toRadians(73.9243376404399),
                    MathLib.toRadians(29.6004319784673))
                    / (10. * 2.));

        Assert.assertEquals(2.75949541662805, actualLBp, 0.001);
        Assert.assertEquals(-2.88590428801295, actualGainp, 1E-14);
        Assert.assertEquals(1.17913088781653, actualEllipticityp, 1E-14);

        Report.printToReport("Link budget (Z+)", 2.75949541662805, actualLBp);
        Report.printToReport("Gain (Z+)", -2.88590428801295, actualGainp);
        Report.printToReport("Ellipticity (Z+)", 1.17913088781653, actualEllipticityp);

        // TU with Z- antenna
        final AbsoluteDate datem = AbsoluteDate.FIFTIES_EPOCH_UTC.shiftedBy(22935.1153651279 * 86400 + dt);
        final PVCoordinates pvm = ephemeris.getPVCoordinates(datem, FramesFactory.getEME2000());
        final Orbit orbitm = new CartesianOrbit(pvm, FramesFactory.getEME2000(), datem, mu);
        final Attitude attitudem = attitudeLaw.getAttitude(orbitm);
        final SpacecraftState statem = new SpacecraftState(orbitm, attitudem);
        satellite.updateMainPartFrame(statem);

        // Check gain, ellipticity and link budget
        final double actualLBm = linkBudgetModelZMoins.computeLinkBudget(datem);
        final double actualGainm = propertyzMoins.getGain(MathLib.toRadians(33.8575400124479),
            MathLib.toRadians(356.344543212932));
        final double actualEllipticitym = MathLib.pow(
            10.,
            propertyzMoins.getEllipticity(MathLib.toRadians(33.8575400124479),
                MathLib.toRadians(356.344543212932))
                / (10. * 2.));

        Assert.assertEquals(4.82199081316511, actualLBm, 0.001);
        Assert.assertEquals(-0.702566180605239, actualGainm, 1E-14);
        Assert.assertEquals(1.25587380271753, actualEllipticitym, 1E-14);

        Report.printToReport("Link budget (Z-)", 4.82199081316511, actualLBm);
        Report.printToReport("Gain (Z-)", -0.702566180605239, actualGainm);
        Report.printToReport("Ellipticity (Z-)", 1.25587380271753, actualEllipticitym);
    }

    /**
     * @testType VT
     * 
     * @testedFeature {@link features#RF_LINK_BUDGET_MODEL}
     * 
     * @testedMethod {@link RFLinkBudgetModel#computeLinkBudget(AbsoluteDate)}
     * 
     * @description test the detection of RF visibility events
     * 
     * @input ephemeris, the RF link budget model parameters
     * 
     * @output RF visibility events date
     * 
     * @testPassCriteria events date are the expected one (absolute threshold: 1E-2s, reference: CMSG)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testRFVisibilityDetection() throws PatriusException, IOException {

        Report.printMethodHeader("testRFVisibilityDetection", "RF visibility events", "CMSG", 7E-2,
            ComparisonType.ABSOLUTE);

        // Event detection threshold in s
        final double eps = 200;

        // Expected events date
        final AbsoluteDate[] expected = {
            new AbsoluteDate("2012-10-17T02:46:16.564", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T02:48:50.132", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T02:48:52.756", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T02:48:56.162", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T02:49:11.260", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T02:50:03.190", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T04:19:22.391", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T04:32:53.905", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T05:58:15.075", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T06:03:32.413", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T06:03:35.649", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T06:11:48.250", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T16:26:59.596", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T16:28:12.013", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T16:28:47.535", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T16:36:40.458", TimeScalesFactory.getUTC()), // LOS

            // PATRIUS events only
            new AbsoluteDate("2012-10-17T16:37:45,598", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T16:38:26,546", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T16:39:18,376", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T16:39:31,668", TimeScalesFactory.getUTC()), // LOS

            new AbsoluteDate("2012-10-17T18:05:09.023", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T18:10:20.976", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T18:10:25.490", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T18:17:51.875", TimeScalesFactory.getUTC()), // LOS
            new AbsoluteDate("2012-10-17T19:43:45.357", TimeScalesFactory.getUTC()), // AOS
            new AbsoluteDate("2012-10-17T19:56:09.579", TimeScalesFactory.getUTC()), // LOS
        };

        // ============================ Propagation ============================

        // RF visibility detectors : Z+ and Z-
        final double linkBudgetThreshold = 5.1;
        final List<AbsoluteDate> actual = new ArrayList<AbsoluteDate>();
        final EventDetector detectorZPlus = new RFVisibilityDetector(linkBudgetModelZplus, linkBudgetThreshold, 2.,
            1E-6, Action.CONTINUE, Action.CONTINUE){
            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                actual.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        };
        final EventDetector detectorZMoins = new RFVisibilityDetector(linkBudgetModelZMoins, linkBudgetThreshold, 2.,
            1E-6, Action.CONTINUE, Action.CONTINUE){
            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                actual.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        };

        // Propagator
        final PVCoordinatesPropagator propagator = new PVCoordinatesPropagator(ephemeris, ephemeris.getMinDate(), mu,
            frame);
        propagator.addEventDetector(detectorZPlus);
        propagator.addEventDetector(detectorZMoins);
        propagator.setAttitudeProvider(attitudeLaw);

        // Propagation (on 86400s)
        propagator.propagate(ephemeris.getMinDate().shiftedBy(2 * 3600 + 40 * 60),
            ephemeris.getMinDate().shiftedBy(20 * 3600));

        // ============================ Check ============================

        // Check
        Assert.assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(0., expected[i].durationFrom(actual.get(i)), eps);
            final String aoslos = i % 2 == 0 ? "AOS" : "LOS";
            Report.printToReport("Event " + (i / 2 + 1) + " (" + aoslos + ")", expected[i].durationFrom(expected[0]),
                actual.get(i).durationFrom(expected[0]));
        }
    }

    /**
     * @testType VT
     * 
     * @testedFeature {@link features#RF_LINK_BUDGET_MODEL}
     * 
     * @testedMethod {@link RFLinkBudgetModel#computeLinkBudget(AbsoluteDate)}
     * 
     * @description test exception are properly thrown (if there is no RF property)
     * 
     * @input the RF link budget model parameters
     * 
     * @output exception
     * 
     * @testPassCriteria exception is properly thrown
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testException() throws PatriusException, IOException {

        try {
            final AssemblyBuilder builder = new AssemblyBuilder();
            final String mainBody = "mainBody";
            builder.addMainPart(mainBody);
            builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "mainFrame"));

            new RFLinkBudgetModel(station, builder.returnAssembly(), "mainBody");
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Setup for all unit tests in the class.
     * It provides a ground station and a satellite.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws NumberFormatException
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, NumberFormatException, IOException {

        Report.printClassHeader(RFLinkBudgetTest.class.getSimpleName(), "RF link budget");

        // =========================================== DATA =========================================== //

        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());

        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                                                                            throws IOException, ParseException,
                                                                            PatriusException {
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> entries = new TreeMap<DateComponents, Integer>();
                for (int i = 1920; i < 2200; i++) {
                    entries.put(new DateComponents(i, 1, 1), -35);
                }
                return entries;
            }

            @Override
            public String getSupportedNames() {
                return "";
            }
        });

        // EOP 1980 history
        EOPHistoryFactory.addEOP1980HistoryLoader(new EOP1980HistoryLoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                                                                            throws IOException, ParseException,
                                                                            PatriusException {
            }

            @Override
            public void fillHistory(final EOP1980History history) throws PatriusException {
                for (int i = 0; i < 1000; i++) {
                    history.addEntry(new EOP1980Entry(AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22934 * 86400), 0.36399,
                        0, 0, 0, 0, 0));
                    history.addEntry(new EOP1980Entry(AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22935 * 86400), 0.36399,
                        0, 0, 0, 0, 0));
                    history.addEntry(new EOP1980Entry(AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22936 * 86400), 0.36399,
                        0, 0, 0, 0, 0));
                    history.addEntry(new EOP1980Entry(AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22937 * 86400), 0.36399,
                        0, 0, 0, 0, 0));
                }
            }
        });

        dt = 34;

        // ============================ Orbit ephemeris (read file) ============================

        mu = 0.398600442000000E+15;
        frame = FramesFactory.getEME2000();
        final URL orbitURL = RFLinkBudgetTest.class.getClassLoader().getResource("RF/FC_EPHPV_NUM_J2000");
        final BufferedReader reader = new BufferedReader(new FileReader(orbitURL.getPath()));

        final List<SpacecraftState> spacecraftList = new ArrayList<SpacecraftState>();
        String line;
        while ((line = reader.readLine()) != null) {

            // Skip header
            if (line.startsWith("#") || line.startsWith(" ") || line.startsWith("<") || line.startsWith(">")
                || line.startsWith("w") || line.length() == 0) {
                continue;
            }

            // Parse line
            final String[] array = line.split(" ");

            // Store ephemeris
            final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_UTC.shiftedBy(Double.parseDouble(array[0])
                * Constants.JULIAN_DAY + dt);
            final Vector3D pos = new Vector3D(Double.parseDouble(array[1]), Double.parseDouble(array[2]),
                Double.parseDouble(array[3]));
            final Vector3D vel = new Vector3D(Double.parseDouble(array[4]), Double.parseDouble(array[5]),
                Double.parseDouble(array[6]));
            final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel), frame, date, mu);
            spacecraftList.add(new SpacecraftState(orbit));
        }
        reader.close();
        final SpacecraftState[] spacecraftArray = new SpacecraftState[spacecraftList.size()];
        for (int j = 0; j < spacecraftArray.length; j++) {
            spacecraftArray[j] = spacecraftList.get(j);
        }
        ephemeris = new EphemerisPvHermite(spacecraftArray, null, null);

        // ============================ Attitude ephemeris (read file) ============================

        final URL attitudeURL = RFLinkBudgetTest.class.getClassLoader().getResource("RF/FC_SIMU_CINE");
        final BufferedReader reader4 = new BufferedReader(new FileReader(attitudeURL.getPath()));

        final List<Attitude> attitudeList = new ArrayList<Attitude>();
        while ((line = reader4.readLine()) != null) {

            // Skip header
            if (line.startsWith("#") || line.startsWith(" ") || line.startsWith("<") || line.startsWith(">")
                || line.length() == 0) {
                continue;
            }

            // Parse line
            final String[] array = line.split(" ");

            // Store ephemeris
            final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_UTC.shiftedBy(Double.parseDouble(array[0])
                * Constants.JULIAN_DAY + Double.parseDouble(array[1]) + dt);
            final Rotation rotation = new Rotation(true, Double.parseDouble(array[19]), Double.parseDouble(array[20]),
                Double.parseDouble(array[21]), Double.parseDouble(array[22]));
            final Vector3D spin = Vector3D.ZERO;
            final Attitude attitude = new Attitude(date, frame, new AngularCoordinates(rotation, spin));
            attitudeList.add(attitude);
        }
        reader4.close();
        attitudeLaw = new TabulatedAttitude(attitudeList, -1);

        // ============================ Station ============================

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(0.637813600000000E+07, 1. / 0.298257800000000E+03,
            FramesFactory.getITRF());

        final GeodeticPoint point = new GeodeticPoint(0.758011794744558, 0.0261405281982074, 0.256000000000000E+03);
        final TopocentricFrame topo = new TopocentricFrame(earth, point, "station");

        final double meritFactor = 11.5;
        final double groundLoss = 2.5;
        final double ellipticityFactor = 3.;
        final double combinerLoss = 0.;
        final double[][] atmopshericLoss = {
            // { FastMath.toRadians(-0.5E+00), 0.000E+00 },
            { MathLib.toRadians(0.0E+00), 0.677E+01 },
            { MathLib.toRadians(0.5E+01), 0.109E+01 },
            { MathLib.toRadians(0.1E+02), 0.500E+00 },
            { MathLib.toRadians(0.2E+02), 0.230E+00 },
            { MathLib.toRadians(0.3E+02), 0.150E+00 },
            { MathLib.toRadians(0.4E+02), 0.120E+00 },
            { MathLib.toRadians(0.5E+02), 0.100E+00 },
            { MathLib.toRadians(0.6E+02), 0.800E-01 },
            { MathLib.toRadians(0.7E+02), 0.800E-01 },
            { MathLib.toRadians(0.9E+02), 0.700E-01 },
        };
        final double[][] pointingLoss = {
            { MathLib.toRadians(0.0E+00), 0. },
            { MathLib.toRadians(0.1E+02), 0. },
            { MathLib.toRadians(0.2E+02), 0. },
            { MathLib.toRadians(0.3E+02), 0. },
            { MathLib.toRadians(0.4E+02), 0. },
            { MathLib.toRadians(0.5E+02), 0. },
            { MathLib.toRadians(0.6E+02), 0. },
            { MathLib.toRadians(0.7E+02), 0. },
            { MathLib.toRadians(0.9E+02), 0. },
        };
        station = new RFStationAntenna(topo, meritFactor, groundLoss, ellipticityFactor, atmopshericLoss, pointingLoss,
            combinerLoss);

        // ============================ Satellite antenna (read file, 1deg sampling) ============================

        final double[] inPatternPolarAngle = new double[91];
        for (int i = 0; i < inPatternPolarAngle.length; i++) {
            inPatternPolarAngle[i] = i;
        }
        final double[] inPatternAzimuth = new double[361];
        for (int i = 0; i < inPatternAzimuth.length; i++) {
            inPatternAzimuth[i] = i;
        }
        final double[][] inGainPatternzPlus = new double[91][361];
        final double[][] inEllipticityFactorzPlus = new double[91][361];
        final double[][] inEllipticityFactorzPlusUnderSampled = new double[10][36];
        final double[][] inGainPatternzMoins = new double[91][361];
        final double[][] inEllipticityFactorzMoins = new double[91][361];
        final double[][] inEllipticityFactorzMoinsUnderSampled = new double[10][36];

        final URL ellipticLossURLzPlus = RFLinkBudgetTest.class.getClassLoader().getResource(
            "RF/FC_DIAG_PERTES_ELLIPTIQUES_TMTC_Z_PLUS");
        final URL gainURLzPlus = RFLinkBudgetTest.class.getClassLoader().getResource("RF/FC_DIAG_ANTENNE_TMTC_Z_PLUS");
        final URL ellipticLossURLzMoins = RFLinkBudgetTest.class.getClassLoader().getResource(
            "RF/FC_DIAG_PERTES_ELLIPTIQUES_TMTC_Z_MOINS");
        final URL gainURLzMoins = RFLinkBudgetTest.class.getClassLoader()
            .getResource("RF/FC_DIAG_ANTENNE_TMTC_Z_MOINS");
        final BufferedReader reader2 = new BufferedReader(new FileReader(ellipticLossURLzPlus.getPath()));
        final BufferedReader reader3 = new BufferedReader(new FileReader(gainURLzPlus.getPath()));
        final BufferedReader reader5 = new BufferedReader(new FileReader(ellipticLossURLzMoins.getPath()));
        final BufferedReader reader6 = new BufferedReader(new FileReader(gainURLzMoins.getPath()));
        // Skip header
        for (int i = 0; i < 9; i++) {
            reader2.readLine();
            reader3.readLine();
            reader5.readLine();
            reader6.readLine();
        }

        // Z+ // // // // // // // // // //
        // // // // // // // // // // // //

        while ((line = reader2.readLine()) != null) {
            // Parse line
            final String[] array = line.split(" ");

            // Store data
            final double azimuth = Double.parseDouble(array[0]);
            final double elevation = Double.parseDouble(array[1]);
            final int iAzimuth = (int) azimuth;
            final int iElevation = (int) elevation;
            inEllipticityFactorzPlus[iElevation][iAzimuth] = Double.parseDouble(array[2]);
            if (azimuth < 350) {
                inEllipticityFactorzPlusUnderSampled[(int) (elevation / 10.)][(int) (azimuth / 10.)] = Double
                    .parseDouble(array[2]);
            } else {
                inEllipticityFactorzPlusUnderSampled[(int) (elevation / 10.)][35] = Double.parseDouble(array[2]);
            }
        }
        while ((line = reader3.readLine()) != null) {
            // Parse line
            final String[] array = line.split(" ");

            // Store data
            final double azimuth = Double.parseDouble(array[0]);
            final double elevation = Double.parseDouble(array[1]);
            final int iAzimuth = (int) azimuth;
            final int iElevation = (int) elevation;
            inGainPatternzPlus[iElevation][iAzimuth] = Double.parseDouble(array[2]);
        }
        reader2.close();
        reader3.close();

        // Z- // // // // // // // // // //
        // // // // // // // // // // // //

        while ((line = reader5.readLine()) != null) {
            // Parse line
            final String[] array = line.split(" ");

            // Store data
            final double azimuth = Double.parseDouble(array[0]);
            final double elevation = Double.parseDouble(array[1]);
            final int iAzimuth = (int) azimuth;
            final int iElevation = (int) elevation;
            inEllipticityFactorzMoins[iElevation][iAzimuth] = Double.parseDouble(array[2]);
            if (azimuth < 350) {
                inEllipticityFactorzMoinsUnderSampled[(int) (elevation / 10.)][(int) (azimuth / 10.)] = Double
                    .parseDouble(array[2]);
            } else {
                inEllipticityFactorzMoinsUnderSampled[(int) (elevation / 10.)][35] = Double.parseDouble(array[2]);
            }
        }
        while ((line = reader6.readLine()) != null) {
            // Parse line
            final String[] array = line.split(" ");

            // Store data
            final double azimuth = Double.parseDouble(array[0]);
            final double elevation = Double.parseDouble(array[1]);
            final int iAzimuth = (int) azimuth;
            final int iElevation = (int) elevation;
            inGainPatternzMoins[iElevation][iAzimuth] = Double.parseDouble(array[2]);
        }
        reader5.close();
        reader6.close();

        // Fill gap
        final BiLinearIntervalsInterpolator interpolator = new BiLinearIntervalsInterpolator();
        final double[] xval = new double[] { 0., 10., 20., 30., 40., 50., 60., 70., 80., 90. };
        final double[] yval = new double[] { 0., 10., 20., 30., 40., 50., 60., 70., 80., 90., 100., 110., 120., 130.,
            140., 150., 160., 170., 180., 190., 200., 210., 220., 230., 240., 250., 260., 270., 280., 290., 300.,
            310., 320., 330., 340., 360. };
        final BiLinearIntervalsFunction funcZPlus = interpolator.interpolate(xval, yval,
            inEllipticityFactorzPlusUnderSampled);
        final BiLinearIntervalsFunction funcZMoins = interpolator.interpolate(xval, yval,
            inEllipticityFactorzMoinsUnderSampled);
        for (int i = 0; i < inEllipticityFactorzPlus.length; i++) {
            for (int j = 0; j < inEllipticityFactorzPlus[i].length; j++) {
                if (inEllipticityFactorzPlus[i][j] == 0) {
                    inEllipticityFactorzPlus[i][j] = funcZPlus.value(i, j);
                }
                if (inEllipticityFactorzMoins[i][j] == 0) {
                    inEllipticityFactorzMoins[i][j] = funcZMoins.value(i, j);
                }
            }
        }

        // Read data is sampled in degree
        for (int i = 0; i < inPatternPolarAngle.length; i++) {
            inPatternPolarAngle[i] = MathLib.toRadians(inPatternPolarAngle[i]);
        }

        for (int i = 0; i < inPatternAzimuth.length; i++) {
            inPatternAzimuth[i] = MathLib.toRadians(inPatternAzimuth[i]);
        }

        final double outputPower = 8.;
        final double technoLoss = 0.1;
        final double circuitLoss = 4.1;
        final double bitRate = 838861.;
        final double frequency = 2215920000.;
        propertyzPlus = new RFAntennaProperty(outputPower, inPatternPolarAngle, inPatternAzimuth,
            inGainPatternzPlus, inEllipticityFactorzPlus, technoLoss, circuitLoss, bitRate, frequency);
        propertyzMoins = new RFAntennaProperty(outputPower, inPatternPolarAngle, inPatternAzimuth,
            inGainPatternzMoins, inEllipticityFactorzMoins, technoLoss, circuitLoss, bitRate, frequency);

        // Assembly : satellite + antennaZ+ + antennaZ-
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main body :
        final String mainBody = "mainBody";
        builder.addMainPart(mainBody);
        final AbsoluteDate attitudeDate = ephemeris.getMinDate().shiftedBy(1);
        final Attitude attitude = attitudeLaw.getAttitude(ephemeris, attitudeDate, frame);
        final PVCoordinates pvCoordinates = ephemeris.getPVCoordinates(attitudeDate, frame);
        final Transform rotation = new Transform(attitudeDate, attitude.getOrientation());
        final Transform translation = new Transform(attitudeDate, pvCoordinates);
        final Transform transform = new Transform(attitudeDate, translation, rotation);
        final UpdatableFrame mainFrame = new UpdatableFrame(frame, transform, "mainFrame");
        builder.initMainPartFrame(mainFrame);

        // antenna Z+
        final String antennaZPlus = "antennaZplus";
        final Vector3D dirAntenneZplus = Vector3D.PLUS_K;
        builder.addPart(antennaZPlus, mainBody, Vector3D.ZERO, new Rotation(dirAntenneZplus, Vector3D.PLUS_K));
        builder.addProperty(propertyzPlus, antennaZPlus);

        // antenna Z-
        final String antennaZMoins = "antennaZMoins";
        final Vector3D dirAntenneZmoins = Vector3D.MINUS_K;
        builder.addPart(antennaZMoins, mainBody, Vector3D.ZERO,
            new Rotation(Vector3D.PLUS_K, FastMath.PI).applyTo(new Rotation(dirAntenneZmoins, Vector3D.PLUS_K)));
        builder.addProperty(propertyzMoins, antennaZMoins);

        satellite = builder.returnAssembly();

        // ============================ Link budget ============================

        linkBudgetModelZplus = new RFLinkBudgetModel(station, satellite, antennaZPlus);
        linkBudgetModelZMoins = new RFLinkBudgetModel(station, satellite, antennaZMoins);
    }
}
