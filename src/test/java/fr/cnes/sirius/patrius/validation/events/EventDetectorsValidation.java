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
 * @history 12/03/2012
 * 
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3144:10/05/2022:[PATRIUS] Classe TempDirectory en double 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::FA:902:13/12/2016:corrected anomaly on local time computation
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.events;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AnomalyDetector;
import fr.cnes.sirius.patrius.propagation.events.BetaAngleDetector;
import fr.cnes.sirius.patrius.propagation.events.DistanceDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.ExtremaDistanceDetector;
import fr.cnes.sirius.patrius.propagation.events.ExtremaLatitudeDetector;
import fr.cnes.sirius.patrius.propagation.events.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.propagation.events.ThreeBodiesAngleDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.TemporaryDirectory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This main aims at testing the g-function of the new event detectors.
 * </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id $
 * 
 * @since 1.1
 * 
 */
public class EventDetectorsValidation {

    /** Temporary directory that keeps in memory the non regression results. */
    private static final String TEMPORARY_DIRECTORY =
        TemporaryDirectory.getTemporaryDirectory("pdb.misc.results", "eventDetectorsValidation");

    /** Validation directory that contains the reference files. */
    private static final String OREKIT_DIRECTORY = "regular-dataCNES-2003";
    /** Second satellite orbit. */
    private static Orbit otherSat;
    /** Earth shape.. */
    private static OneAxisEllipsoid earthBody;
    /** Main satellite orbit. */
    private static Orbit sat;
    /** Earth ephemerides. */
    private static PVCoordinatesProvider earth;
    /** Sun ephemerides. */
    private static PVCoordinatesProvider sun;

    /**
     * Creates in the temporary directory the g file and its reference file for each detector in order to validate those
     * functions g.
     * 
     * @param args
     * @throws IOException
     * @throws PatriusException
     * @throws ParseException
     */
    public static void main(final String[] args) throws IOException, PatriusException, ParseException {

        Utils.setDataRoot(OREKIT_DIRECTORY);

        final FramesConfiguration defConf = FramesFactory.getConfiguration();
        final FramesConfigurationBuilder fb = new FramesConfigurationBuilder(defConf);
        fb.setEOPHistory(new NoEOP2000History());

        FramesFactory.setConfiguration(fb.getConfiguration());

        // Bodies creation : sun and earth
        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loaderSun);

        final JPLEphemeridesLoader loaderEarth = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH, loaderEarth);

        sun = CelestialBodyFactory.getSun();

        earth = CelestialBodyFactory.getEarth();

        // Secondary satellite creation
        otherSat = new KeplerianOrbit(7150000, 0.001, MathLib.toRadians(98), 0, 0, 0,
            PositionAngle.ECCENTRIC, FramesFactory.getGCRF(), new AbsoluteDate(2012, 3, 8, 9, 50, 0.0,
                TimeScalesFactory.getTAI()), Constants.EGM96_EARTH_MU);

        // Earth shape creation
        earthBody = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING,
            FramesFactory.getITRF());

        // Main satellite creation
        final AbsoluteDate date = new AbsoluteDate(2012, 3, 8, 9, 50, 0.0, TimeScalesFactory.getTAI());
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final Frame refFrame = FramesFactory.getGCRF();
        sat = new KeplerianOrbit(7200000, 0.001, MathLib.toRadians(30), 0, 0, 0,
            PositionAngle.ECCENTRIC, refFrame, date, mu);

        // Propagator creation
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8, 1e-9 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        final NumericalPropagator propagator = new NumericalPropagator(dop853);

        final File outputDirectory = new File(TEMPORARY_DIRECTORY);
        deleteDirectory(outputDirectory);
        outputDirectory.mkdirs();

        // Anomaly detector
        final double period = MathUtils.TWO_PI * MathLib.sqrt(MathLib.pow(sat.getA(), 3) / sat.getMu());

        createFile(EventDetectorProvider.ANOMALY, propagator, 3 * period, 60, sat, outputDirectory);

        // Beta detector
        createFile(EventDetectorProvider.BETA, propagator, 365 * 24 * 3600., 24 * 3600, new KeplerianOrbit(7200000,
            0.001, MathLib.toRadians(100), 0, 0, 0,
            PositionAngle.ECCENTRIC, refFrame, date, mu), outputDirectory);

        // Distance detector
        createFile(EventDetectorProvider.DISTANCE, propagator, 1 * period, 60, sat, outputDirectory);

        // Extremum distance detector
        createFile(EventDetectorProvider.DISTANCE_EXTREMUM, propagator, 1 * period, 60, sat, outputDirectory);

        // Extremum latitude detector
        createFile(EventDetectorProvider.LATITUDE_EXTREMUM, propagator, 3 * period, 60, sat, outputDirectory);

        // Local time detector
        createFile(EventDetectorProvider.LOCAL_TIME, propagator, 3 * period, 60, sat, outputDirectory);

        // Three bodies angle detector
        createFile(EventDetectorProvider.THREE_BODIES, propagator, 3 * period, 60, sat, outputDirectory);

        System.out.println("done !");

    }

    /**
     * Creates the reference and the result files in the temporary directory.
     * 
     * @param det
     *        : detector provider
     * @param propagator
     *        : numerical propagator
     * @param propagationDuration
     *        : propagation duration
     * @param step
     *        : sampling step
     * @param orbit
     *        : satellite orbit
     * @param file
     *        : output directory
     * @throws IOException
     * @throws PatriusException
     */
    private static
            void
            createFile(final EventDetectorProvider det, final NumericalPropagator propagator,
                       final double propagationDuration, final int step, final Orbit orbit, final File file)
                                                                                                            throws IOException,
                                                                                                            PatriusException {
        final File tmp = new File(file + File.separator + det.toString());
        tmp.mkdir();

        final File resultG = new File(tmp + File.separator + "g.txt");
        final File resultRef = new File(tmp + File.separator + "ref.txt");

        final AbsoluteDate startDate = orbit.getDate();
        final AbsoluteDate finalDate = startDate.shiftedBy(propagationDuration);

        final EventDetector detector = det.getEventDetector();
        propagator.addEventDetector(detector);

        propagator.setEphemerisMode();

        propagator.resetInitialState(new SpacecraftState(orbit));

        propagator.propagate(startDate, finalDate);

        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // writers
        final BufferedWriter writerG = new BufferedWriter(new FileWriter(resultG));
        final BufferedWriter writerRef = new BufferedWriter(new FileWriter(resultRef));

        final String space = "    ";

        AbsoluteDate currentDate;
        SpacecraftState currentState;

        for (int i = 0; i < propagationDuration; i += step) {
            currentDate = startDate.shiftedBy(i);
            currentState = ephemeris.propagate(currentDate);

            writerG.write(i + space + detector.g(currentState));
            writerG.newLine();

            writerRef.write(i + space + det.getParameter(currentState.getOrbit()));
            writerRef.newLine();
        }
        writerG.close();
        writerRef.close();
    }

    /**
     * @description delete directory
     * 
     * @param path
     *        directory to be deleted
     * @return boolean
     * @since 1.0
     */
    private static boolean deleteDirectory(final File path) {
        boolean resultat = true;

        if (path.exists()) {
            final File[] files = path.listFiles();
            for (final File file : files) {
                if (file.isDirectory()) {
                    resultat &= deleteDirectory(file);
                } else {
                    resultat &= file.delete();
                }
            }
        }
        resultat &= path.delete();
        return (resultat);
    }

    /**
     * Event detectors provider.
     * 
     * @author antonj
     * 
     */
    private enum EventDetectorProvider implements IEventDetectorProvider {
        /** Anomaly detector. */
        ANOMALY("anomaly") {
            @Override
            public EventDetector getEventDetector() {
                return new AnomalyDetector(PositionAngle.ECCENTRIC, MathLib.toRadians(30)){
                    /** UID */
                    private static final long serialVersionUID = 5772527031504631726L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) {
                final Orbit o = OrbitType.KEPLERIAN.convertType(orbit);
                return ((KeplerianOrbit) o).getAnomaly(PositionAngle.ECCENTRIC);
            }
        },
        /** Seta detector. */
        BETA("beta") {
            @Override
            public EventDetector getEventDetector() throws PatriusException {
                return new BetaAngleDetector(MathLib.toRadians(-10)){
                    /** UID */
                    private static final long serialVersionUID = 4227264512760340993L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) throws PatriusException {

                final AbsoluteDate date = orbit.getDate();
                final Frame frame = orbit.getFrame();
                final Vector3D momentum = orbit.getPVCoordinates().getMomentum();
                final IDirection direction = new GenericTargetDirection(sun);
                final Vector3D EarthSun = direction.getVector(earth, date, frame);

                return MathUtils.HALF_PI - Vector3D.angle(momentum, EarthSun);
            }
        },
        /** Distance detector. */
        DISTANCE("distance") {

            @Override
            public EventDetector getEventDetector() throws PatriusException {
                return new DistanceDetector(otherSat, 500000){
                    /** IUD */
                    private static final long serialVersionUID = -3119681191395165998L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) throws PatriusException {
                final AbsoluteDate date = orbit.getDate();
                final Frame frame = orbit.getFrame();
                final Vector3D satPV = orbit.getPVCoordinates().getPosition();
                final Vector3D sat2 = otherSat.getPVCoordinates(date, frame).getPosition();
                final double distance = sat2.subtract(satPV).getNorm();
                return distance;
            }

        },
        /** Extremum distance detector. */
        DISTANCE_EXTREMUM("extremum distance") {

            @Override
            public EventDetector getEventDetector() throws PatriusException {
                return new ExtremaDistanceDetector(otherSat, 0){
                    /** IUD */
                    private static final long serialVersionUID = 70936179113679755L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) throws PatriusException {
                final AbsoluteDate date = orbit.getDate();
                final Frame frame = orbit.getFrame();
                final Vector3D satPV = orbit.getPVCoordinates().getPosition();
                final Vector3D sat2 = otherSat.getPVCoordinates(date, frame).getPosition();
                final double distance = sat2.subtract(satPV).getNorm();
                return distance;
            }
        },
        /** Extremum latitude detector. */
        LATITUDE_EXTREMUM("extremum latitude") {

            @Override
            public EventDetector getEventDetector() throws PatriusException {
                return new ExtremaLatitudeDetector(0, FramesFactory.getGCRF()){
                    /** IUD */
                    private static final long serialVersionUID = 2986825505780731490L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) throws PatriusException {
                final AbsoluteDate date = orbit.getDate();
                final Frame frame = orbit.getFrame();
                final GeodeticPoint point = earthBody.transform(orbit.getPVCoordinates().getPosition(), frame, date);
                return point.getLatitude();
            }

        },
        /** Local time detector. */
        LOCAL_TIME("local time") {

            @Override
            public EventDetector getEventDetector() throws PatriusException {
                return new LocalTimeAngleDetector(4 * MathUtils.TWO_PI / 24){
                    /** IUD */
                    private static final long serialVersionUID = 5743921037994851730L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) throws PatriusException {
                final AbsoluteDate date = orbit.getDate();
                final Vector3D sunPV = sun.getPVCoordinates(date, FramesFactory.getGCRF()).getPosition();
                final Vector3D sunProj = new Vector3D(sunPV.getX(), sunPV.getY(), 0);
                final Vector3D satPV = orbit.getPVCoordinates(date, FramesFactory.getGCRF()).getPosition();
                final Vector3D satProj = new Vector3D(satPV.getX(), satPV.getY(), 0);

                // cross product of the 2 vectors that define the plane
                final Vector3D crossProduct = Vector3D.crossProduct(satProj, sunProj);
                // non oriented angle between the 2 formers vectors
                final double angleResult = Vector3D.angle(satProj, sunProj);
                double rez;
                if (crossProduct.getZ() > 0) {
                    rez = 24 - angleResult * 24 / MathUtils.TWO_PI;
                } else {
                    rez = angleResult * 24 / MathUtils.TWO_PI;
                }
                return rez;
            }

        },
        /** Three bodies angle detector. */
        THREE_BODIES("three bodies angle") {

            @Override
            public EventDetector getEventDetector() throws PatriusException {
                return new ThreeBodiesAngleDetector(sun, earth, sat, MathLib.toRadians(105)){
                    /** IUD */
                    private static final long serialVersionUID = -5607675037950111603L;

                    @Override
                    public
                            Action
                            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                                   throws PatriusException {
                        return Action.CONTINUE;
                    }
                };
            }

            @Override
            public double getParameter(final Orbit orbit) throws PatriusException {
                final AbsoluteDate date = orbit.getDate();
                final Frame frame = orbit.getFrame();
                final Vector3D SunEarth = sun.getPVCoordinates(date, frame).getPosition()
                    .subtract(earth.getPVCoordinates(date, frame).getPosition());
                final Vector3D SatEarth = orbit.getPVCoordinates().getPosition()
                    .subtract(earth.getPVCoordinates(date, frame).getPosition());
                return Vector3D.angle(SunEarth, SatEarth);
            }

        };
        /**
         * Detector name
         */
        private final String name;

        /**
         * Constructor.
         * 
         * @param s
         *        : detector name
         */
        private EventDetectorProvider(final String s) {
            this.name = s;
        }

        @Override
        public String toString() {
            return this.name;
        }

        /*
         * Below : workaround for the javac bug number 6947909,
         * only corrected for JVM 7...
         */
        @Override
        public abstract EventDetector getEventDetector() throws PatriusException;

        /*
         * Below : workaround for the javac bug number 6947909,
         * only corrected for JVM 7...
         */
        @Override
        public abstract double getParameter(Orbit orbit) throws PatriusException;
    }

    /**
     * Event detector provider interface.
     * 
     * @author antonj
     * 
     */
    private interface IEventDetectorProvider {
        /**
         * Gets the proper event detector.
         * 
         * @return EventDetector
         * @throws PatriusException
         */
        EventDetector getEventDetector() throws PatriusException;

        /**
         * Returns the parameter value that conditions the event occurrence.
         * 
         * @param orbit
         *        : satellite orbit
         * @return : parameter value
         * @throws PatriusException
         */
        double getParameter(Orbit orbit) throws PatriusException;
    }
}
