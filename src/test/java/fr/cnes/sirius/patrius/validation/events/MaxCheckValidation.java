/**
 * 
 * Copyright 2011-2017 CNES
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.events;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.TempDirectory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This main aims at testing the Orekit events detection process when using a propagator for different values of
 * maxCheckInterval parameter.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: MaxCheckValidation.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class MaxCheckValidation {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Event detection maxCheck parametrisation
         * 
         * @featureDescription influence of maxCheck parametrisation when
         *                     detecting events.
         * 
         * @coveredRequirements DV-EVT_63, DV-PROPAG_60, DV-PROPAG_130
         */
        PARAMETRIZATION_MAXCHECK
    }

    /** Start date. */
    static AbsoluteDate iDate = new AbsoluteDate(2000, 1, 10,
        TimeScalesFactory.getTT());

    /** Number of trials. */
    private static int numberOfTrials = 5;

    /** Directory of the output files. */
    private static final String outputDirectory =
        TempDirectory.getTemporaryDirectory("pdb.misc.results", "eventDetectionValidation");

    /**
     * Private constructor.
     */
    private MaxCheckValidation() {
    }

    /**
     * <p>
     * Main method which launch the propagation of a generic orbit with numerical propagators (using Runge Kutta,
     * Dormand Prince 853 or Gragg Bulirsch Stoer integrator), Keplerian propagator and Eckstein Hechler propagator. The
     * propagator has been supplied with 10 mock events at 10 different dates. The purpose of this test is to analyse
     * the detection parametrization. This parametrization concerns the maximum interval length where events are
     * checked, the convergence (i.e. the precision of the detection process) and a maximum number of iteration to find
     * an event. We will try different maximum check interval lengths as well as different convergence values. The
     * generated files are stored in the temporary directory : 1 file per integrator.
     * </p>
     * 
     * @throws PatriusException
     *         Orekit exception
     * @throws IOException
     *         i/o exception
     * 
     * @since 1.0
     */
    public static void main(final String[] args)
                                                throws IOException, PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");

        /** Semi-major axis. */
        final double a = 7200000;

        /** Period. */
        final double period = FastMath.PI * 2 * MathLib.sqrt(MathLib.pow(a, 3) / Constants.EIGEN5C_EARTH_MU);

        /** End date. */
        final AbsoluteDate fDate = iDate.shiftedBy(3 * period);

        /** Inclination. */
        final double i = MathLib.toRadians(72);

        /** Eccentricity. */
        final double e = 0.001;

        // Add the package as containing directory for this test
        final String directory = outputDirectory + File.separator + "maxCheck";

        // Build a File object for this directory
        final File dir = new File(directory);
        dir.mkdirs();
        final String filePath = dir.getPath() + File.separator + "report_MaxCheck.csv";
        final File log = new File(filePath);
        final BufferedWriter writer = new BufferedWriter(new FileWriter(log));
        writer.write("Max check interval validation");
        writer.newLine();
        writer.newLine();
        writer.write("Delta T propagation:" + 3 * period);
        writer.newLine();
        writer.newLine();

        final Orbit orbit = new KeplerianOrbit(a, e, i, 0, 0,
            0, PositionAngle.MEAN, FramesFactory.getGCRF(), iDate,
            Constants.EGM96_EARTH_MU);

        // Propagator, DOP853
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.01, 2, 0.0001, 1);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        // No events detection
        final double t0 = System.currentTimeMillis();
        for (int y = 0; y < numberOfTrials; y++) {
            propagator.propagate(fDate);
        }
        final double duree_noevents = (System.currentTimeMillis() - t0) / numberOfTrials;
        propagator.resetInitialState(new SpacecraftState(orbit));
        writer.newLine();
        writer.write("Duree, no events" + ";" + duree_noevents);
        writer.newLine();
        writer.newLine();

        // Different tried max check interval lengths
        final double[] maxCheck = { 0.1, 0.2, 0.5, 1, 5, 15, 60, 100, 150, 300, 600, 1000 };
        final EventsLogger logger = new EventsLogger();
        double duree;

        for (final double element : maxCheck) {
            duree = calculate(propagator, fDate, orbit, element, logger);
            writer.newLine();
            writer.write("Max check interval" + ";" + "Duree");
            writer.newLine();
            writer.write(element + ";" + duree);
            writer.newLine();
            writer.newLine();
            writer.write("Date" + ";" + "Detected event" + ";" + "Entry/exit");
            writer.newLine();

            for (int k = 0; k < logger.getLoggedEvents().size(); k++) {
                final AbsoluteDate finalDate = logger.getLoggedEvents().get(k).getState().getDate();
                final double deltaT = finalDate.durationFrom(iDate);
                final String event = logger.getLoggedEvents().get(k).getEventName();
                final String state = logger.getLoggedEvents().get(k).getEventState();
                writer.write(deltaT + ";" + event + ";" + state);
                writer.newLine();
            }
            propagator.clearEventsDetectors();
            logger.clearLoggedEvents();
            propagator.resetInitialState(new SpacecraftState(orbit));
        }
        writer.close();

        System.out.println("done !");
    }

    /**
     * @description Run the simulation and write in several files
     *              the results relative to the detection event process.
     * 
     * @throws IOException
     *         IO exception
     * @throws PatriusException
     *         orekit exception
     * 
     * @since 1.0
     */
    private static double calculate(
                                    final NumericalPropagator propagator, final AbsoluteDate fDate, final Orbit orbit,
                                    final double maxCheck, final EventsLogger logger)
                                                                                     throws IOException,
                                                                                     PatriusException {

        // Events initialisation
        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        final CelestialBody sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);

        final JPLEphemeridesLoader loaderEarth = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH);

        final CelestialBody earth = loaderEarth.loadCelestialBody(CelestialBodyFactory.EARTH);

        // Penumbra detector that does not stop the propagation
        final EclipseDetector penumbraDetector =
            new EclipseDetector(sun, Constants.SUN_RADIUS, earth, Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                1, maxCheck, AbstractDetector.DEFAULT_THRESHOLD){

                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector, "Partial eclipse"));

        // Umbra detector that does not stop the propagation
        final EclipseDetector umbraDetector =
            new EclipseDetector(sun, Constants.SUN_RADIUS, earth, Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                0, maxCheck, AbstractDetector.DEFAULT_THRESHOLD){

                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
        propagator.addEventDetector(logger.monitorDetector(umbraDetector, "Total eclipse"));

        // Apogee/perigee detector that does not stop the propagation
        final ApsideDetector apsideDetector = new ApsideDetector(orbit, 2){

            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        propagator.addEventDetector(logger.monitorDetector(apsideDetector, "Apside crossing"));

        // Nodes detector that does not stop the propagation
        final NodeDetector nodeDetector =
            new NodeDetector(orbit, FramesFactory.getGCRF(), NodeDetector.ASCENDING_DESCENDING){

                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
        propagator.addEventDetector(logger.monitorDetector(nodeDetector, "Node crossing"));

        // Circular fov detector that does not stop the propagation
        final GeodeticPoint station1_coord = new GeodeticPoint(MathLib.toRadians(5),
            MathLib.toRadians(120), 0);
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid body = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frame);
        final TopocentricFrame station1 = new TopocentricFrame(body, station1_coord, "Station1");
        final Vector3D center1 = Vector3D.PLUS_I;
        final double aperture1 = MathLib.toRadians(35);

        final CircularFieldOfViewDetector fov1Detector =
            new CircularFieldOfViewDetector(station1, center1, aperture1, maxCheck){

                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
        propagator.addEventDetector(logger.monitorDetector(fov1Detector, "Visibility station 1"));

        // Circular fov detector that does not stop the propagation
        final GeodeticPoint station2_coord = new GeodeticPoint(MathLib.toRadians(85),
            MathLib.toRadians(30), 0);
        final TopocentricFrame station2 = new TopocentricFrame(body, station2_coord, "Station2");
        final Vector3D center2 = Vector3D.PLUS_I;
        final double aperture2 = MathLib.toRadians(25);

        final CircularFieldOfViewDetector fov2Detector =
            new CircularFieldOfViewDetector(station2, center2, aperture2, maxCheck){

                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
        propagator.addEventDetector(logger.monitorDetector(fov2Detector, "Visibility station 2"));

        // Dihedral fov detector that does not stop the propagation
        final GeodeticPoint station3_coord = new GeodeticPoint(MathLib.toRadians(-10),
            MathLib.toRadians(30), 0);
        final TopocentricFrame station3 = new TopocentricFrame(body, station3_coord, "Station3");
        final Vector3D center3 = Vector3D.PLUS_I;
        final double aperture3_1 = MathLib.toRadians(20);
        final double aperture3_2 = MathLib.toRadians(50);

        final DihedralFieldOfViewDetector fov3Detector =
            new DihedralFieldOfViewDetector(station3, center3, Vector3D.MINUS_J,
                aperture3_1, Vector3D.PLUS_K, aperture3_2, maxCheck){

                private static final long serialVersionUID = 1L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return Action.CONTINUE;
                }
            };
        propagator.addEventDetector(logger.monitorDetector(fov3Detector, "Visibility station 3"));

        // Integration execution time
        long start;
        long duree;

        start = System.currentTimeMillis();
        for (int y = 0; y < numberOfTrials; y++) {
            propagator.propagate(fDate);
        }
        duree = (System.currentTimeMillis() - start) / numberOfTrials;
        return duree;
    }

}
