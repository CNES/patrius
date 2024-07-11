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
 * @history created 16/05/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:1307:11/09/2017:correct formulation of g() function
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link ExtremaElevationDetector}.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ExtremaElevationDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ExtremaElevationDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Elevation extrema detector
         * 
         * @featureDescription Validate the distance elevation extrema detector
         * 
         * @coveredRequirements DV-EVT_150, DV-EVT_50
         */
        VALIDATE_ELEVATION_EXTREMA_DETECTOR
    }

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ELEVATION_EXTREMA_DETECTOR}
     * 
     * @testedMethod {@link ExtremaElevationDetector#ExtremaElevationDetector(TopocentricFrame, int, double)}
     * @testedMethod {@link ExtremaElevationDetector#g(SpacecraftState)}
     * 
     * @description Test of the extrema elevation detector.
     * 
     * @input a simple polar circular orbit, and a station position
     *        close to the north pole, so that max elevation happens
     *        at T/4 and min elevation happens at 3T/4 (T the period of the orbit)
     * 
     * @output the detected event's dates
     * 
     * @testPassCriteria the dates are the expected ones : T/4 and 3T/4
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testElevationExtremaDetector() throws PatriusException {
        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7500000.0;
        final Orbit tISSOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        Propagator propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);
        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // station frame creation
        final BodyShape earth = new OneAxisEllipsoid(6000000.0, 0.0, EME2000Frame);
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(80),
            MathLib.toRadians(90),
            0.0);
        final TopocentricFrame topoFrame = new TopocentricFrame(earth, point, "Gstation");

        // event detector
        final double maxCheck = AbstractDetector.DEFAULT_MAXCHECK;

        final EventDetector elevationMaxDetector = new ExtremaElevationDetector(topoFrame,
            ExtremaElevationDetector.MAX, maxCheck);
        final EventDetector elevationMinDetector = new ExtremaElevationDetector(topoFrame,
            ExtremaElevationDetector.MIN, maxCheck);

        // test the throwing of an exception when a constructor parameter is not supported:
        boolean asExpected = false;
        try {
            new ExtremaElevationDetector(topoFrame, 5, maxCheck, 10E-5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);

        propagator.addEventDetector(elevationMaxDetector);
        // test
        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(1.0 / 4.0 * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);

        propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);
        propagator.addEventDetector(elevationMinDetector);
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(3.0 / 4.0 * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);
    }

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ELEVATION_EXTREMA_DETECTOR}
     * 
     * @testedMethod {@link ExtremaElevationDetector#ExtremaElevationDetector(TopocentricFrame, int, double)}
     * @testedMethod {@link ExtremaElevationDetector#g(SpacecraftState)}
     * 
     * @description Test of the extrema elevation detector: performs a propagation that should stop at elevation
     *              extremum.
     * 
     * @input a random orbit and a random station
     * 
     * @output the detected event's dates
     * 
     * @testPassCriteria propagation stops at elevation extremum (check is performed using elevation rate of topocentric
     *                   frame which is supposed to be 0)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public final void testElevationExtremaDetectorGeneral() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Utils.mu);

        // Station
        final BodyShape earth = new OneAxisEllipsoid(6378000.0, Constants.GRS80_EARTH_FLATTENING,
            FramesFactory.getGCRF());
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(42), MathLib.toRadians(72), 122.0);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(earth, point, "Station");

        // Propagation
        final Propagator propagator = new KeplerianPropagator(orbit);
        final EventDetector elevationMaxDetector = new ExtremaElevationDetector(topocentricFrame,
            ExtremaElevationDetector.MAX, AbstractDetector.DEFAULT_MAXCHECK, 1E-14);
        propagator.addEventDetector(elevationMaxDetector);
        final SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(10000.0));

        // Check that elevation rate is 0 at event date
        Assert.assertEquals(
            0.,
            topocentricFrame.getElevationRate(finalState.getPVCoordinates(), finalState.getFrame(),
                finalState.getDate()), 1E-14);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_ELEVATION_EXTREMA_DETECTOR}
     * 
     * @testedMethod {@link ExtremaElevationDetector#ExtremaElevationDetector(TopocentricFrame, int, double, Action)} *
     * @description simple constructor test
     * 
     * @input constructor parameters: topocentric frame, extremumType, the max check
     *        value and the threshold value and the STOP Action.
     * 
     * @output a {@link ExtremaElevationDetector}
     * 
     * @testPassCriteria the {@link ExtremaElevationDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testExtremaElevationDetectorCtor() throws PatriusException {

        // earth
        final Frame EME2000Frame = FramesFactory.getEME2000();
        // station frame creation
        final BodyShape earth = new OneAxisEllipsoid(6000000.0, 0.0, EME2000Frame);
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(80),
            MathLib.toRadians(90),
            0.0);
        final TopocentricFrame topoFrame = new TopocentricFrame(earth, point, "Gstation");

        final ExtremaElevationDetector detector = new ExtremaElevationDetector(topoFrame, ExtremaElevationDetector.MAX,
            10, 0.1, Action.STOP);
        final ExtremaElevationDetector detector2 = (ExtremaElevationDetector) detector.copy();
        // Test getters
        Assert.assertEquals(topoFrame, detector2.getTopocentricFrame());

    }
}
