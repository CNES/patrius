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
 * @history creation 06/08/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for the ground zone detector.
 * </p>
 * 
 * @see EarthZoneDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class EarthZoneDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle earth zone detector
         * 
         * @featureDescription Detector for event "entering in a earth zone".
         * 
         * @coveredRequirements DV-EVT_130
         */
        EARTH_ZONE_DETECTOR
    }

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_ZONE_DETECTOR}
     * 
     * @testedMethod {@link EarthZoneDetector#g(SpacecraftState)}
     * @testedMethod {@link EarthZoneDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Here we test the dates at which the event "entering in a earth zone" has occured
     * 
     * @input a spacecraft, its orbit, a earth zone (some geodectic points, or some direction
     *        vectors)
     * 
     * @output dates of the detected events
     * 
     * @testPassCriteria the g function has the expected sign and the dates of the detected events
     *                   are the expected ones.
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void detectorTest() throws PatriusException {

        // propagator
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;
        Orbit initialOrbit = new KeplerianOrbit(a, 0.0, FastMath.PI / 2., 0.0, 0.0, 0.0,
            PositionAngle.TRUE, eme2000Frame, date, Utils.mu);
        Propagator propagator = new KeplerianPropagator(initialOrbit);
        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // earth
        final double r = 6000000.0;
        final BodyShape earth = new OneAxisEllipsoid(r, 0.0, eme2000Frame);

        // test 1
        // zone creation
        final double[][] zone = { { FastMath.PI / 4., FastMath.PI / 2. },
            { FastMath.PI / 4., FastMath.PI }, { FastMath.PI / 4., -FastMath.PI / 2. },
            { FastMath.PI / 4., 0. } };

        // detector creation
        List<double[][]> zones = new ArrayList<>();
        zones.add(zone);
        EventDetector detector = new EarthZoneDetector(earth, zones);
        propagator.addEventDetector(detector);

        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));

        // expected date from propagation beginning
        double timeDetected = period / 8.;
        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // zone leaving
        detector = new EarthZoneDetector(earth, zones){
            /** Serializable UID. */
            private static final long serialVersionUID = -3166374927699533986L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                return increasing ? Action.CONTINUE : Action.STOP;
            }
        };
        propagator = new KeplerianPropagator(endState.getOrbit());
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(10000.0));

        // expected date from propagation beginning (zone entering)
        timeDetected = 3. * period / 8.;
        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // test 2
        // zone creation
        final double[][] zone2 = { { FastMath.PI / 4., FastMath.PI / 4. },
            { FastMath.PI / 4., -FastMath.PI / 4. }, { -FastMath.PI / 4., -FastMath.PI / 4. },
            { -FastMath.PI / 4., FastMath.PI / 4. } };

        // detector creation
        zones = new ArrayList<>();
        zones.add(zone2);
        detector = new EarthZoneDetector(earth, zones);
        propagator.addEventDetector(detector);

        initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE,
            eme2000Frame, date, Utils.mu);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(10000.0));

        // expected date from propagation beginning
        timeDetected = 7. * period / 8.;
        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // test 3
        // zone creation (from vectors)
        final Vector3D[] zone3 = new Vector3D[4];
        zone3[0] = new Vector3D(FastMath.PI / 4., FastMath.PI / 4.);
        zone3[1] = new Vector3D(-FastMath.PI / 4., FastMath.PI / 4.);
        zone3[2] = new Vector3D(-FastMath.PI / 4., -FastMath.PI / 4.);
        zone3[3] = new Vector3D(FastMath.PI / 4., -FastMath.PI / 4.);

        // detector creation
        final List<Vector3D[]> zonesVect = new ArrayList<>();
        zonesVect.add(zone3);
        detector = new EarthZoneDetector(zonesVect, earth.getBodyFrame());
        propagator.addEventDetector(detector);

        initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE,
            eme2000Frame, date, Utils.mu);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(10000.0));

        // expected date from propagation beginning
        timeDetected = 7. * period / 8.;
        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_ZONE_DETECTOR}
     * 
     * @testedMethod {@link EarthZoneDetector#EarthZoneDetector(BodyShape, List, double, double)}
     * 
     * @description Testing thrown errors when zones are flawed
     * 
     * @input some points of space creating an incorrect zone (same consecutive points or crossing
     *        arcs)
     * 
     * @output The illegal argument exception
     * 
     * @testPassCriteria the right illegal argument exception is thrown.
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void wrongZoneTest() throws PatriusException {

        // zone creation (from vectors) with two identical consecutive points
        try {
            final Vector3D[] zone3 = new Vector3D[4];
            zone3[0] = new Vector3D(FastMath.PI / 4., FastMath.PI / 4.);
            zone3[1] = new Vector3D(-FastMath.PI / 4., FastMath.PI / 4.);
            zone3[2] = new Vector3D(-FastMath.PI / 4., FastMath.PI / 4.);
            zone3[3] = new Vector3D(FastMath.PI / 4., -FastMath.PI / 4.);
            final List<Vector3D[]> zones = new ArrayList<>();
            zones.add(zone3);
            new EarthZoneDetector(zones, FramesFactory.getGCRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // zone creation (from vectors) with arc crossing
        try {
            final Vector3D[] zone3 = new Vector3D[5];
            zone3[0] = new Vector3D(FastMath.PI / 4., FastMath.PI / 4.);
            zone3[1] = new Vector3D(0., FastMath.PI / 4.);
            zone3[2] = new Vector3D(-FastMath.PI / 4., FastMath.PI / 4.);
            zone3[3] = new Vector3D(FastMath.PI / 4., -FastMath.PI / 4.);
            zone3[4] = new Vector3D(-FastMath.PI / 4., -FastMath.PI / 4.);
            final List<Vector3D[]> zones = new ArrayList<>();
            zones.add(zone3);
            new EarthZoneDetector(zones, FramesFactory.getGCRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // zone creation (from vectors) with too few directions
        try {
            final Vector3D[] zone3 = new Vector3D[2];
            zone3[0] = new Vector3D(FastMath.PI / 4., FastMath.PI / 4.);
            zone3[1] = new Vector3D(-FastMath.PI / 4., FastMath.PI / 4.);
            final List<Vector3D[]> zones = new ArrayList<>();
            zones.add(zone3);
            new EarthZoneDetector(zones, FramesFactory.getGCRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_ZONE_DETECTOR}
     * 
     * @testedMethod {@link EarthZoneDetector#EarthZoneDetector(double, double, double, double, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: earth , zones, the max check value and the threshold value and
     *        the STOP Action.
     * 
     * @output a {@link EarthZoneDetector}
     * 
     * @testPassCriteria the {@link EarthZoneDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {

        // First constructeur
        // earth
        final double r = 6000000.0;
        final BodyShape earth = new OneAxisEllipsoid(r, 0.0, FramesFactory.getEME2000());
        // zone
        final double[][] zone = { { FastMath.PI / 4., FastMath.PI / 2. },
            { FastMath.PI / 4., FastMath.PI }, { FastMath.PI / 4., -FastMath.PI / 2. },
            { FastMath.PI / 4., 0. } };
        // detector creation
        final List<double[][]> zones = new ArrayList<>();
        zones.add(zone);

        final EarthZoneDetector detector = new EarthZoneDetector(earth, zones,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP);
        // Test getters
        Assert.assertEquals(earth, detector.getCentralBodyShape());
        Assert.assertNotNull(detector.getFOV());

        // Second constructeur
        final Vector3D[] zone3 = new Vector3D[4];
        zone3[0] = new Vector3D(FastMath.PI / 4., FastMath.PI / 4.);
        zone3[1] = new Vector3D(-FastMath.PI / 4., FastMath.PI / 4.);
        zone3[2] = new Vector3D(-FastMath.PI / 4., -FastMath.PI / 4.);
        zone3[3] = new Vector3D(FastMath.PI / 4., -FastMath.PI / 4.);
        final List<Vector3D[]> zones2 = new ArrayList<>();
        zones2.add(zone3);
        final EarthZoneDetector detector2 = new EarthZoneDetector(zones2, FramesFactory.getGCRF(),
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP);

        // Test getters
        Assert.assertEquals(FramesFactory.getGCRF(), detector2.getFrame());

        // Copy
        final List<IFieldOfView> fieldIn = new ArrayList<>();
        fieldIn.add(new CircularField("field", 0.2, Vector3D.PLUS_I));
        final EarthZoneDetector detector3 = new EarthZoneDetector(fieldIn, earth,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP, false, false);

        final EarthZoneDetector detectorCopy = (EarthZoneDetector) detector3.copy();
        Assert.assertEquals(detector3.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);

    }
}
