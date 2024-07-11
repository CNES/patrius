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
 * @history created 10/07/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Unit tests for {@link LatitudeDetector}.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: LatitudeDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class LatitudeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the satellite latitude detector
         * 
         * @featureDescription Validate the satellite latitude detector
         * 
         * @coveredRequirements DV-EVT_121
         */
        VALIDATE_LATITUDE_DETECTOR
    }

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link LatitudeDetector#g(SpacecraftState) }
     * @testedMethod {@link LatitudeDetector#getLatitudeToDetect() }
     * @testedMethod {@link LatitudeDetector#getEarthShape() }
     * 
     * @description test of the latitude detection
     * 
     * @input a simple circular orbit, a latitude detector
     * 
     * @output the detected dates
     * 
     * @testPassCriteria the dates are the expected ones : when the spacecraft
     *                   reaches the right latitude
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testLatitudeDetector() throws PatriusException {
        // propagator
        Propagator propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);

        final LatitudeDetector detector =
            new LatitudeDetector(this.latitudeToDetect, this.earth, LatitudeDetector.UP_DOWN);
        propagator.addEventDetector(detector);

        // test
        SpacecraftState endState = propagator.propagate(this.date.shiftedBy(10000.));
        final double expectedTime = this.period / 8.;
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.date), this.datesComparisonEpsilon);

        // same test with a constructor with two actions defined
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        final EventDetector detector2 = new LatitudeDetector(this.latitudeToDetect, this.earth,
            AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        propagator.addEventDetector(detector2);
        endState = propagator.propagate(this.date.shiftedBy(10000.));
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.date), this.datesComparisonEpsilon);

        // same constructor with LatitudeDetector.UP
        propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);
        final EventDetector detector3 = new LatitudeDetector(this.latitudeToDetect, this.earth, LatitudeDetector.UP);
        propagator.addEventDetector(detector3);
        endState = propagator.propagate(this.date.shiftedBy(10000.));
        Assert.assertEquals(expectedTime, endState.getDate().durationFrom(this.date), this.datesComparisonEpsilon);

        /*
         * Test getters
         */
        Assert.assertEquals(this.latitudeToDetect, detector.getLatitudeToDetect(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(this.earth.hashCode(), detector.getEarthShape().hashCode());
    }

    /**
     * @throws PropagationException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link LatitudeDetector#LatitudeDetector(double, BodyShape, int, double, double, Action) }
     * 
     * @description Test that defined action for occurring event is taken into account.
     * 
     * @input a LatitudeDetector with RESET_STATE at latitude detection + a propagator
     * 
     * @output a final state
     * 
     * @testPassCriteria The propagation should end at the expected final date. The state is reset at latitude
     *                   detection.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testDefinedAction() throws PropagationException {
        // propagator
        final Propagator propagator = new KeplerianPropagator(this.initialOrbit, this.attitudeProv);

        final double latitudeToDetect = FastMath.PI / 4.;
        final MyLatitudeDetector detector = new MyLatitudeDetector(latitudeToDetect, this.earth, LatitudeDetector.UP,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE);
        propagator.addEventDetector(detector);

        // test
        final SpacecraftState endState = propagator.propagate(this.date.shiftedBy(10000.));
        Assert.assertEquals(10000., endState.getDate().durationFrom(this.date), this.datesComparisonEpsilon);
        final double expectedTime = this.period / 8.;
        Assert.assertEquals(expectedTime, detector.dateReset.get(0).durationFrom(this.date),
            this.datesComparisonEpsilon);
    }

    /*
     * Custom LatitudeDetector
     */
    class MyLatitudeDetector extends LatitudeDetector {
        private final ArrayList<AbsoluteDate> dateReset;

        public MyLatitudeDetector(final double latitudeToDetect, final BodyShape earth, final int slopeType,
            final double maxCheck,
            final double threshold, final Action action) {
            super(latitudeToDetect, earth, slopeType, maxCheck, threshold, action);
            this.dateReset = new ArrayList<AbsoluteDate>();
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            this.dateReset.add(oldState.getDate());
            return oldState;
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link LatitudeDetector#LatitudeDetector(double, BodyShape, int) }
     * 
     * @description Test exceptions thrown if latitude is not between -PI/2 and PI/2)
     * 
     * @input a latitude detector defined with a single action and altitude = PI/2
     * 
     * @output an exception
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testConstructorException1() {
        new LatitudeDetector(FastMath.PI, this.earth, LatitudeDetector.UP_DOWN);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link LatitudeDetector#LatitudeDetector(double, BodyShape, double, double, Action, Action) }
     * 
     * @description Test exceptions thrown if latitude is not between -PI/2 and PI/2)
     * 
     * @input a latitude detector defined with two actions and altitude = PI/2
     * 
     * @output an exception
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testConstructorException2() {
        new LatitudeDetector(FastMath.PI, this.earth, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link LatitudeDetector#LatitudeDetector(double, BodyShape, double, double, Action, Action) }
     * 
     * @description Test LatitudeDetector with local decreasing latitude detection
     * 
     * @input LatitudeDetector with DOWN
     * 
     * @output action return by eventOccured
     * 
     * @testPassCriteria The action return by the eventOccurred is the expected one
     * 
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testConstructorDOWN() throws PatriusException {
        EventDetector detector = new LatitudeDetector(this.latitudeToDetect, this.earth, LatitudeDetector.DOWN,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE);
        final SpacecraftState s = new SpacecraftState(this.initialOrbit);
        Assert.assertEquals(Action.RESET_STATE, detector.eventOccurred(s, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector.eventOccurred(s, true, false));
        Assert.assertEquals(Action.RESET_STATE, detector.eventOccurred(s, false, true));
        Assert.assertEquals(Action.RESET_STATE, detector.eventOccurred(s, false, false));

        detector = new LatitudeDetector(this.latitudeToDetect, this.earth,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, Action.STOP);
        final LatitudeDetector detector2 = (LatitudeDetector) detector.copy();
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(s, true, true));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(s, true, false));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(s, false, true));
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(s, false, false));
    }

    @Before
    public void setUp() {
        // frame and date
        this.EME2000Frame = FramesFactory.getEME2000();
        this.date = AbsoluteDate.J2000_EPOCH;

        // propagator
        this.attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7000000.0;
        this.initialOrbit = new KeplerianOrbit(7000000.0, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, this.EME2000Frame, this.date, Utils.mu);

        // earth shape
        final double r = 6000000.0;
        this.earth = new OneAxisEllipsoid(r, 0.0, this.EME2000Frame);

        // period
        this.period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // latitude to detect
        this.latitudeToDetect = FastMath.PI / 4.;
    }

    /** Frame */
    private Frame EME2000Frame;

    /** Frame */
    private AbsoluteDate date;

    /** Attitude provider */
    private AttitudeProvider attitudeProv;

    /** Initial orbit */
    private Orbit initialOrbit;

    /** Earth */
    private BodyShape earth;

    /** Period */
    private double period;

    /** Latitude to detect */
    private double latitudeToDetect;
}
