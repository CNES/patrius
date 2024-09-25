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
 * VERSION:4.13.1:FA:FA-176:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.TwoDirectionAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.directions.ConstantVectorDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.CentralBodyMaskCircularFOVDetector;
import fr.cnes.sirius.patrius.events.detectors.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link CentralBodyMaskCircularFOVDetector}.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class CentralBodyMaskCircularFOVDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle CentralBodyMaskCircularFOVDetector
         * 
         * @featureDescription CentralBodyMaskCircularFOVDetector
         * 
         * @coveredRequirements DV-EVT_130
         */
        CENTRALBODYMASKCIRCULARFOVDETECTOR
    }

    /** String. */
    private static final String SPHERO_EARTH = "spheroEarth";

    /** Event : machCheck. */
    private static final double MAXCHK = 30.;

    /** Event threshold : 1/10th millisecond */
    private static final double THRS = 1e-4;

    /** Frame definition. */
    private static Frame itrf;

    /** The Earth. */
    private static CelestialBody earth;

    /** A generic orbit. */
    private static KeplerianOrbit orbit;

    /** Initial propagation date. */
    private static AbsoluteDate date;

    /**
     * Setup before class.
     * 
     * @throws PatriusException should not happen
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialisation
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        itrf = FramesFactory.getITRF();
    }

    /**
     * Setup.
     * 
     * @throws PatriusException should not happen
     */
    @Before
    public void setUp() throws PatriusException {
        // Celestial bodies initialisation:
        earth = CelestialBodyFactory.getEarth();
        // Orbit initialisation:
        date = new AbsoluteDate("2002-04-01T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = earth.getGM();
        orbit = new KeplerianOrbit(12500000, 0.01, 0., 0, 0, FastMath.PI / 2, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, mu);
    }

    /**
     * @description Test this event detector wrap feature in {@link SignalPropagationWrapperDetector}
     * 
     * @input this event detector in INSTANTANEOUS & LIGHT_SPEED
     * 
     * @output the emitter & receiver dates
     * 
     * @testPassCriteria The results containers as expected (non regression)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSignalPropagationWrapperDetector() throws PatriusException {

        // The target is a fixed point in the ITRF frame in the earth's equatorial plane, far away.
        final Vector3D targPos = new Vector3D(1e8, 0., 0.);
        final Vector3D targVel = Vector3D.ZERO;
        final double targRadius = 1000000.;
        final PVCoordinates targPV = new PVCoordinates(targPos, targVel);
        final PVCoordinatesProvider target = new BasicPVCoordinatesProvider(targPV, itrf);
        final double halfAp = FastMath.PI / 100.;
        final EllipsoidBodyShape spheroEarth = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, earth.getRotatingFrame(IAUPoleModelType.TRUE), SPHERO_EARTH);

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final CentralBodyMaskCircularFOVDetector eventDetector1 = new CentralBodyMaskCircularFOVDetector(
            target, targRadius, spheroEarth, false, targPos, halfAp, 600, 0.001, Action.CONTINUE);
        final CentralBodyMaskCircularFOVDetector eventDetector2 = (CentralBodyMaskCircularFOVDetector) eventDetector1
            .copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, new BodyCenterPointing(itrf));
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2002-04-02T04:19:04.243"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2002-04-02T04:19:04.243"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2002-04-02T04:21:54.099"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2002-04-02T04:21:54.099"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2002-04-02T04:19:03.911"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2002-04-02T04:19:04.243"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2002-04-02T04:21:53.769"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2002-04-02T04:21:54.099"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        final EclipseDetector eclipseDetector = eventDetector1.getEclipseDetector();
        Assert.assertEquals(eclipseDetector.getEmitter(finalState), eventDetector1.getEmitter(finalState));
        Assert.assertEquals(eclipseDetector.getReceiver(finalState), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(eclipseDetector.getDatationChoice(), eventDetector1.getDatationChoice());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRALBODYMASKCIRCULARFOVDETECTOR}
     * 
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#CentralBodyMaskCircularFOVDetector(PVCoordinatesProvider, double, fr.cnes.sirius.patrius.bodies.PatriusBodyShape, boolean, Vector3D, double)}
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description simple test for several methods (mainly for coverage)
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria method results as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testMisc() throws PatriusException {
        // The target is a fixed point in the ITRF frame
        // in the earth's equatorial plane, far away.
        final Vector3D targPos = new Vector3D(1e8, 0., 0);
        final Vector3D targVel = Vector3D.ZERO;
        final double targRadius = 1000000;
        final PVCoordinates targPV = new PVCoordinates(targPos, targVel);
        final PVCoordinatesProvider target = new BasicPVCoordinatesProvider(targPV, itrf);

        final double halfAp = FastMath.PI / 100;
        final EllipsoidBodyShape spheroEarth = new OneAxisEllipsoid(
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING,
            earth.getRotatingFrame(IAUPoleModelType.TRUE), SPHERO_EARTH);

        // CentralBody
        final CentralBodyMaskCircularFOVDetector cbmcfovd = new CentralBodyMaskCircularFOVDetector(
            target, targRadius, spheroEarth, false, targPos, halfAp);

        Assert.assertEquals(Action.STOP, cbmcfovd.eventOccurred(null, false, true));
        Assert.assertEquals(Action.STOP, cbmcfovd.eventOccurred(null, true, true));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRALBODYMASKCIRCULARFOVDETECTOR}
     * 
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#CentralBodyMaskCircularFOVDetector(PVCoordinatesProvider, double, fr.cnes.sirius.patrius.bodies.PatriusBodyShape, boolean, Vector3D, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output a {@link CentralBodyMaskCircularFOVDetector}
     * 
     * @testPassCriteria the {@link CentralBodyMaskCircularFOVDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {
        // The target is a fixed point in the ITRF frame
        // in the earth's equatorial plane, far away.
        final Vector3D targPos = new Vector3D(1e8, 0., 0);
        final Vector3D targVel = Vector3D.ZERO;
        final double targRadius = 1000000;
        final PVCoordinates targPV = new PVCoordinates(targPos, targVel);
        final PVCoordinatesProvider target = new BasicPVCoordinatesProvider(targPV, itrf);

        final double halfAp = FastMath.PI / 100;
        final EllipsoidBodyShape spheroEarth = new OneAxisEllipsoid(
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING,
            earth.getRotatingFrame(IAUPoleModelType.TRUE), SPHERO_EARTH);

        // CentralBody
        final CentralBodyMaskCircularFOVDetector detector = new CentralBodyMaskCircularFOVDetector(
            target, targRadius, spheroEarth, false, targPos, halfAp, 600, 0.001, Action.STOP);
        // Test getters
        Assert.assertEquals(target, detector.getEclipseDetector().getOcculted());
        Assert.assertEquals(target, detector.getCircularFOVDetector().getPVTarget());
        Assert.assertEquals(false, detector.shouldBeRemoved());

        // Copy
        final CircularFieldOfViewDetector dectectorCircular = new CircularFieldOfViewDetector(
            CelestialBodyFactory.getEarth(), Vector3D.PLUS_I, 0.5, 600);
        final EclipseDetector eclipseDetector = new EclipseDetector(CelestialBodyFactory.getSun(), 6000,
            CelestialBodyFactory.getMoon(), 3000, 0.2, 600, 0.0001, Action.CONTINUE,
            Action.STOP);
        final CentralBodyMaskCircularFOVDetector detector2 = new CentralBodyMaskCircularFOVDetector(
            eclipseDetector, dectectorCircular, 600, 0.001, Action.STOP, false);
        final CentralBodyMaskCircularFOVDetector detectorCopy = (CentralBodyMaskCircularFOVDetector) detector2
            .copy();
        Assert.assertEquals(detector2.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRALBODYMASKCIRCULARFOVDETECTOR}
     * 
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#g(SpacecraftState)}
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagation test case 1 : a target far from earth in the equatorial plane,
     *              satellite close to equatorial
     * 
     * @input initial orbit, event detectors parameters, propagation duration
     * 
     * @output lists of event dates for reference events and for the
     *         CentralBodyMaskCircularFOVDetector.
     * 
     * @testPassCriteria the dates for CentralBodyMaskCircularFOVDetector match those of the
     *                   computed reference.<br>
     *                   Epsilon justification : unitary test on event detection needs not a great
     *                   accuracy. 1E-4 s is enough.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testTargetOne() throws PatriusException {
        // target definition
        // The target is a fixed point in the ITRF frame
        // in the earth's equatorial plane, far away,
        // and more or less on a symmetry axis of the orbit.
        final Vector3D targetPos = new Vector3D(1e8, 0., 0);
        final Vector3D targetVel = Vector3D.ZERO;
        final double targetRadius = 1000000;
        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);
        final PVCoordinatesProvider targetPVP = new BasicPVCoordinatesProvider(targetPV, itrf);
        // Reference circular FOV parameters
        final double halfAp = MathLib.toRadians(3.);
        // Propagation parameters
        final double propDuration = 25000;

        // Fixed directions in ITRF for the satellite
        // So that the FOV always points in a fixed direction
        final Vector3D fovDir = Vector3D.PLUS_I;
        final IDirection fstDir = new ConstantVectorDirection(fovDir, itrf);
        final IDirection sndDir = new ConstantVectorDirection(Vector3D.PLUS_J, itrf);
        final TwoDirectionAttitudeLaw aProv = new TwoDirectionAttitudeLaw(fstDir, sndDir,
            Vector3D.PLUS_I, Vector3D.PLUS_J);

        // Extrapolation runner
        final DatesEventDetector[] runrez = this.runner(targetRadius, targetPVP, halfAp, propDuration,
            fovDir, aProv);

        // List of event dates builder
        final DatesEventDetector dedCfvd = runrez[0];
        // final DatesEventDetector dedEed = runrez[1];
        final DatesEventDetector dedCbmcfovd = runrez[2];

        // Reference dates lists
        final List<AbsoluteDate> inFovDates = dedCfvd.increasingEvtDates();
        final List<AbsoluteDate> outFovDates = dedCfvd.decreasingEvtDates();
        final List<AbsoluteDate> inCbmfovdDates = dedCbmcfovd.increasingEvtDates();
        final List<AbsoluteDate> outCbmfovdDates = dedCbmcfovd.decreasingEvtDates();
        // final List<AbsoluteDate> inEclipseDates = dedEed.decreasingEvtDates();
        // final List<AbsoluteDate> outEclipseDates = dedEed.increasingEvtDates();
        // System.out.println("In FOV : " + inFovDates);
        // System.out.println("Out FOV : " + outFovDates);
        // System.out.println("In ECL : " + inEclipseDates);
        // System.out.println("Out ECL : " + outEclipseDates);
        // System.out.println("In CBFOV : " + inCbmfovdDates);
        // System.out.println("Out CBFOV : " + outCbmfovdDates);

        // Calculated reference : there are three inFovDates and outFovDates.
        // Only the second ones happen outside the eclipse, and therefore
        // should appear inside inCbmfovdDates and outCbmfovdDates.
        Assert.assertEquals(1, inCbmfovdDates.size());
        Assert.assertEquals(1, outCbmfovdDates.size());
        Assert.assertEquals(0., inCbmfovdDates.get(0).durationFrom(inFovDates.get(1)), THRS);
        Assert.assertEquals(0., outCbmfovdDates.get(0).durationFrom(outFovDates.get(1)), THRS);
    }

    /**
     * Prepares event detectors and runs the propagation.
     * 
     * @param targetRadius targetRadius
     * @param targetPVP targetPVP
     * @param halfAp halfAp
     * @param propDuration propDuration
     * @param fovDir fovDir
     * @param aProv tda
     * @return array of event detectors' wrappers
     * @throws PatriusException should not happen
     */
    private DatesEventDetector[] runner(final double targetRadius,
                                        final PVCoordinatesProvider targetPVP, final double halfAp,
                                        final double propDuration,
                                        final Vector3D fovDir, final AttitudeProvider aProv) throws PatriusException {

        // Reference CircularFOVDetector
        final CircularFieldOfViewDetector cfvd = new CircularFieldOfViewDetector(targetPVP, fovDir,
            halfAp, MAXCHK, THRS){
            /** SerialUID. */
            private static final long serialVersionUID = 7859669241525917726L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) {
                // final String str = increasing ? IN : OUT;
                // System.out.println("CFOV" + str + s.getDate());
                return Action.CONTINUE;
            }
        };
        // Reference EllipsoidEclipseDetector
        final EllipsoidBodyShape spheroEarth = new OneAxisEllipsoid(
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING,
            earth.getRotatingFrame(IAUPoleModelType.TRUE), SPHERO_EARTH);
        final EclipseDetector eed = new EclipseDetector(targetPVP, targetRadius, spheroEarth, 1,
            MAXCHK, THRS){
            /** SerialUID. */
            private static final long serialVersionUID = 7859000455917726L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) {
                // final String str = increasing ? OUT : IN;
                // System.out.println("EllEclipseD" + str + s.getDate());
                return Action.CONTINUE;
            }
        };

        // CentralBody
        final CentralBodyMaskCircularFOVDetector cbmcfovd = new CentralBodyMaskCircularFOVDetector(
            targetPVP, targetRadius, spheroEarth, false, fovDir, halfAp, MAXCHK, THRS){
            /** SerialUID. */
            private static final long serialVersionUID = 7859669241525917726L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) {
                // final String str = increasing ? IN : OUT;
                // System.out.println("CBMCFOV" + str + s.getDate());
                return Action.CONTINUE;
            }
        };

        // List of event dates builder
        final DatesEventDetector dedCfvd = new DatesEventDetector(cfvd);
        final DatesEventDetector dedEed = new DatesEventDetector(eed);
        final DatesEventDetector dedCbmcfovd = new DatesEventDetector(cbmcfovd);

        // Propagator
        final KeplerianPropagator kpo = new KeplerianPropagator(orbit, aProv);

        kpo.addEventDetector(dedCfvd);
        kpo.addEventDetector(dedEed);
        kpo.addEventDetector(dedCbmcfovd);

        kpo.propagate(date.shiftedBy(propDuration));
        // final String sep = "\t";
        // AbsoluteDate lastCdate = date;
        // PVCoordinates cordo;
        // final StringBuilder sb = new StringBuilder();
        // for (int i = 1; i < 1000; i++) {
        // final AbsoluteDate cdate = date.shiftedBy(i * ( propDuration / 1000. ));
        // final SpacecraftState rez = kpo.propagate(lastCdate, cdate);
        // lastCdate = cdate;
        // // sat
        // cordo = rez.getPVCoordinates();
        // sb.append(cordo.getPosition().getX());
        // sb.append(sep);
        // sb.append(cordo.getPosition().getY());
        // sb.append(sep);
        // sb.append(cordo.getPosition().getZ());
        // sb.append("\n");
        // }
        //
        // // targ
        // sb.append(targPos.getX());
        // sb.append(sep);
        // sb.append(targPos.getY());
        // sb.append(sep);
        // sb.append(targPos.getZ());
        // System.out.println(sb.toString());

        // Return the dateEventsDetectors
        final DatesEventDetector[] rez = { dedCfvd, dedEed, dedCbmcfovd };
        return rez;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRALBODYMASKCIRCULARFOVDETECTOR}
     * 
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#g(SpacecraftState)}
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagation test case 2 : a target far from earth in the equatorial plane,
     *              satellite close to equatorial, target not on an orbit symmetry axis.
     * 
     * @input initial orbit, event detectors parameters, propagation duration
     * 
     * @output lists of event dates for reference events and for the
     *         CentralBodyMaskCircularFOVDetector.
     * 
     * @testPassCriteria the dates for CentralBodyMaskCircularFOVDetector match those of the
     *                   computed reference.<br>
     *                   Epsilon justification : unitary test on event detection needs not a great
     *                   accuracy. 1E-4 s is enough.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testTargetTwo() throws PatriusException {
        // The target is a fixed point in the ITRF frame
        // in the earth's equatorial plane, not that far away,
        // but far from the symmetry axis of the orbit
        final Vector3D targetPos = new Vector3D(1000 * 20000, -1000 * 6000, 0);
        final Vector3D targetVel = Vector3D.ZERO;
        final double targetRadius = 1000000;
        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);
        final PVCoordinatesProvider targetPVP = new BasicPVCoordinatesProvider(targetPV, itrf);
        // Reference circular FOV parameters
        final double halfAp = MathLib.toRadians(3.);
        // Propagation parameters
        final double propDuration = 18000;

        // Fixed directions in ITRF for the satellite
        // So that the FOV always points in a fixed direction (x axis)
        final Vector3D fovDir = Vector3D.PLUS_I;
        final IDirection fstDir = new ConstantVectorDirection(fovDir, itrf);
        final IDirection sndDir = new ConstantVectorDirection(Vector3D.PLUS_J, itrf);
        final TwoDirectionAttitudeLaw aProv = new TwoDirectionAttitudeLaw(fstDir, sndDir,
            Vector3D.PLUS_I, Vector3D.PLUS_J);

        // Extrapolation runner
        final DatesEventDetector[] runrez = this.runner(targetRadius, targetPVP, halfAp, propDuration,
            fovDir, aProv);

        // List of event dates builder
        final DatesEventDetector dedCfvd = runrez[0];
        final DatesEventDetector dedEed = runrez[1];
        final DatesEventDetector dedCbmcfovd = runrez[2];

        // Reference dates lists
        final List<AbsoluteDate> inFovDates = dedCfvd.increasingEvtDates();
        final List<AbsoluteDate> outFovDates = dedCfvd.decreasingEvtDates();
        // final List<AbsoluteDate> inEclipseDates = dedEed.decreasingEvtDates();
        final List<AbsoluteDate> outEclipseDates = dedEed.increasingEvtDates();
        final List<AbsoluteDate> inCbmfovdDates = dedCbmcfovd.increasingEvtDates();
        final List<AbsoluteDate> outCbmfovdDates = dedCbmcfovd.decreasingEvtDates();
        // System.out.println("In FOV : " + inFovDates);
        // System.out.println("Out FOV : " + outFovDates);
        // System.out.println("In ECL : " + inEclipseDates);
        // System.out.println("Out ECL : " + outEclipseDates);
        // System.out.println("In CBFOV : " + inCbmfovdDates);
        // System.out.println("Out CBFOV : " + outCbmfovdDates);

        // Calculated reference : there are two inFovDates and outFovDates.
        // the first inFov happens inside the eclipse,
        // so the corresponding cbmFov only starts when the eclipse is exited
        Assert.assertEquals(2, inCbmfovdDates.size());
        Assert.assertEquals(2, outCbmfovdDates.size());
        Assert.assertEquals(0., inCbmfovdDates.get(0).durationFrom(outEclipseDates.get(0)), THRS);
        Assert.assertEquals(0., outCbmfovdDates.get(0).durationFrom(outFovDates.get(0)), THRS);
        // The second inFOV (and corresponding outFov) happen fully outside the eclipse
        Assert.assertEquals(0., inCbmfovdDates.get(1).durationFrom(inFovDates.get(1)), THRS);
        Assert.assertEquals(0., outCbmfovdDates.get(1).durationFrom(outFovDates.get(1)), THRS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRALBODYMASKCIRCULARFOVDETECTOR}
     * 
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#g(SpacecraftState)}
     * @testedMethod {@link CentralBodyMaskCircularFOVDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description propagation test case 3 : a target inside the orbit, satellite with ground
     *              pointing
     * 
     * @input initial orbit, event detectors parameters, propagation duration
     * 
     * @output lists of event dates for reference events and for the
     *         CentralBodyMaskCircularFOVDetector.
     * 
     * @testPassCriteria the dates for CentralBodyMaskCircularFOVDetector match those of the
     *                   computed reference.<br>
     *                   Epsilon justification : unitary test on event detection needs not a great
     *                   accuracy. 1E-4 s is enough.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testTargetThree() throws PatriusException {
        // The target is a fixed point in the ITRF frame
        // in the earth's equatorial plane, close to earth
        // inside the satellite's orbit
        final Vector3D targetPos = new Vector3D(1000 * 7700, -1000 * 7700, 0);
        final Vector3D targetVel = Vector3D.ZERO;
        final double targetRadius = 500000;
        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);
        final PVCoordinatesProvider targetPVP = new BasicPVCoordinatesProvider(targetPV, itrf);
        // Reference circular FOV parameters
        final double halfAp = MathLib.toRadians(3.);
        // Propagation parameters
        final double propDuration = 18000;

        // Ground pointing for the satellite
        // We also want the FOV to the ground, so Z axis
        final Vector3D fovDir = Vector3D.PLUS_K;
        final AttitudeProvider gpa = new BodyCenterPointing(itrf);

        // Extrapolation runner
        final DatesEventDetector[] runrez = this.runner(targetRadius, targetPVP, halfAp, propDuration,
            fovDir, gpa);

        // List of event dates builder
        final DatesEventDetector dedCfvd = runrez[0];
        // final DatesEventDetector dedEed = runrez[1];
        final DatesEventDetector dedCbmcfovd = runrez[2];
        // Reference dates lists
        // final List<AbsoluteDate> inEclipseDates = dedEed.decreasingEvtDates();
        // final List<AbsoluteDate> outEclipseDates = dedEed.increasingEvtDates();
        final List<AbsoluteDate> inFovDates = dedCfvd.increasingEvtDates();
        final List<AbsoluteDate> outFovDates = dedCfvd.decreasingEvtDates();
        final List<AbsoluteDate> inCbmfovdDates = dedCbmcfovd.increasingEvtDates();
        final List<AbsoluteDate> outCbmfovdDates = dedCbmcfovd.decreasingEvtDates();
        // System.out.println("In FOV : " + inFovDates);
        // System.out.println("Out FOV : " + outFovDates);
        // System.out.println("In ECL : " + inEclipseDates);
        // System.out.println("Out ECL : " + outEclipseDates);
        // System.out.println("In CBFOV : " + inCbmfovdDates);
        // System.out.println("Out CBFOV : " + outCbmfovdDates);

        // Calculated reference : there are two inFovDates and outFovDates.
        // the first inFov and outFov happens inside the eclipse.
        // The second inFOV (and corresponding outFov) happen fully outside the eclipse
        Assert.assertEquals(1, inCbmfovdDates.size());
        Assert.assertEquals(1, outCbmfovdDates.size());
        Assert.assertEquals(0., inCbmfovdDates.get(0).durationFrom(inFovDates.get(1)), THRS);
        Assert.assertEquals(0., outCbmfovdDates.get(0).durationFrom(outFovDates.get(1)), THRS);
    }

    /**
     * Wraps an event detector and records event dates.
     */
    private final class DatesEventDetector implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 9167375228709789339L;
        /** . */
        final EventDetector ine;
        /** . */
        final List<AbsoluteDate> incrEvtDates = new ArrayList<>();
        /** . */
        final List<AbsoluteDate> decrEvtDates = new ArrayList<>();

        /**
         * Ctor.
         * 
         * @param innerEventDetector wrapped event detector.
         */
        public DatesEventDetector(final EventDetector innerEventDetector) {
            this.ine = innerEventDetector;
        }

        /**
         * Getter for increasing event date.
         * 
         * @return list of dates
         */
        public List<AbsoluteDate> increasingEvtDates() {
            return this.incrEvtDates;
        }

        /**
         * Getter for decreasing event date.
         * 
         * @return list of dates
         */
        public List<AbsoluteDate> decreasingEvtDates() {
            return this.decrEvtDates;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
            this.ine.init(s0, t);
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            return this.ine.g(s);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            (increasing ? this.incrEvtDates : this.decrEvtDates).add(s.getDate());
            return this.ine.eventOccurred(s, increasing, forward);
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            return this.ine.resetState(oldState);
        }

        @Override
        public double getThreshold() {
            return this.ine.getThreshold();
        }

        @Override
        public double getMaxCheckInterval() {
            return this.ine.getMaxCheckInterval();
        }

        @Override
        public int getMaxIterationCount() {
            return this.ine.getMaxIterationCount();
        }

        @Override
        public int getSlopeSelection() {
            return this.ine.getSlopeSelection();
        }

        @Override
        public EventDetector copy() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean filterEvent(final SpacecraftState state, final boolean increasing, final boolean forward) {
            // Do nothing by default, event is not filtered
            return false;
        }
    }

    /**
     * A simple PVCoordinatesProvider based on a constant PVCoordinates object.
     */
    private final class BasicPVCoordinatesProvider implements PVCoordinatesProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8933655533492885164L;

        /** PVCoordinates point. */
        private final PVCoordinates coordinates;

        /** Expression frame. */
        private final Frame frame;

        /**
         * Builds a direction from an origin and a target described by their PVCoordinatesProvider
         * 
         * @param inCoordinates the PVCoordinates
         * @param inFrame the frame in which the coordinates are expressed
         * */
        public BasicPVCoordinatesProvider(final PVCoordinates inCoordinates, final Frame inFrame) {

            // Initialisation
            this.coordinates = inCoordinates;
            this.frame = inFrame;
        }

        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate pdate, final Frame inFrame)
                                                                                            throws PatriusException {

            // the coordinates are expressed in the output frame
            final Transform toOutputFrame = this.frame.getTransformTo(inFrame, pdate);
            final PVCoordinates outCoordinates = toOutputFrame.transformPVCoordinates(this.coordinates);

            return outCoordinates;
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) {
            return this.frame;
        }
    }
}
