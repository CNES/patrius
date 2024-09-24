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
 * @history created 11/06/12
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-80:30/06/2023:[PATRIUS] Discriminer le "increasing" et "decreasing" dans NadirSolarIncidenceDetector (suite)
 * VERSION:4.11:DM:DM-3291:22/05/2023:[PATRIUS] Discriminer le "increasing" et "decreasing" dans NadirSolarIncidenceDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2502:27/01/2021:[PATRIUS] Choix des ephemerides solaires dans certains detecteurs - manque de cas de test 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:215:08/04/2014:modification of the nadir solar incidence definition
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:394:30/03/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link NadirSolarIncidenceDetector}.<br>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: NadirSolarIncidenceDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @see NadirSolarIncidenceDetector
 * 
 * @since 1.1
 * 
 */
public class NadirSolarIncidenceDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the nadir point solar incidence detector
         * 
         * @featureDescription Validate the nadir point solar incidence detector
         * 
         * @coveredRequirements DV-EVT_130
         */
        VALIDATE_NADIR_SOLAR_INCIDENCE
    }

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NADIR_SOLAR_INCIDENCE}
     * 
     * @testedMethod {@link NadirSolarIncidenceDetector#g(SpacecraftState)}
     * @testedMethod {@link NadirSolarIncidenceDetector#eventOccurred(SpacecraftState, boolean)}
     * 
     * @description Test of the nadir point solar incidence detector
     * 
     * @input a simple polar circular orbit, the earth shape
     * 
     * @output the detected event's dates
     * 
     * @testPassCriteria the dates are the expected ones
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testNAdirSunIncidenceDetector() throws PatriusException {

        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7500000.0;
        final Orbit tISSOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        final Propagator propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);
        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // earth
        final double r = 6000000.0;
        final BodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);

        // sun (the sun is remplaced by a simple position for this test
        final double sunDist = 1.e9;
        final Vector3D sunPos = new Vector3D(sunDist, 0., 0.);
        final PVCoordinates sunPV = new PVCoordinates(sunPos, Vector3D.ZERO);
        final BasicPVCoordinatesProvider sun = new BasicPVCoordinatesProvider(sunPV, EME2000Frame);

        // detector
        final double incidenceToDetect = 3 * FastMath.PI / 8.;
        final NadirSolarIncidenceDetector detector = new NadirSolarIncidenceDetector(incidenceToDetect, earth,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        detector.setSun(sun);
        final NadirSolarIncidenceDetector detector2 = (NadirSolarIncidenceDetector) detector.copy();

        propagator.addEventDetector(detector2);

        // physical angle from earth center to detect (the stop date is when the g function is decreasing)
        // sunDist / sin(PI - incidenceToDetect) = r / sin (incidenceToDetect - angleFromEarthCenter)
        final double angleFromEarthCenter = incidenceToDetect -
                MathLib.asin(r * MathLib.sin(FastMath.PI - incidenceToDetect) / sunDist);

        // associated date from propagation beginning
        final double timeDetected = period * angleFromEarthCenter / (2. * FastMath.PI);

        // test
        final SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);
        Assert.assertEquals(sun, detector2.getSun());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NADIR_SOLAR_INCIDENCE}
     * 
     * @testedMethod {@link NadirSolarIncidenceDetector#NadirSolarIncidenceDetector(double, double, double, double, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the incidence, earth the earth shape, the max check
     *        value and the threshold value and the STOP Action.
     * 
     * @output a {@link NadirSolarIncidenceDetector}
     * 
     * @testPassCriteria the {@link NadirSolarIncidenceDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testNadirSolarIncidenceDetectorCtor() throws PatriusException {

        final double incidenceToDetect = 3 * FastMath.PI / 8.;
        // earth
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final double r = 6000000.0;
        final BodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);

        final NadirSolarIncidenceDetector detector = new NadirSolarIncidenceDetector(incidenceToDetect, earth,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP);
        // Test getters
        Assert.assertEquals(earth, detector.getBodyShape());
        Assert.assertEquals(incidenceToDetect, detector.getIncidence(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NADIR_SOLAR_INCIDENCE}
     * 
     * @testedMethod {@link NadirSolarIncidenceDetector#NadirSolarIncidenceDetector(double, BodyShape, double, double, Action, boolean, CelestialBody)}
     * 
     * @description checks user Sun model is properly taken into account
     * 
     * @input constructor parameters : Sun model
     * 
     * @output g value
     * 
     * @testPassCriteria g value is the same when using default Sun and CelestialBodyFactory.getSun() and different when
     *                   using MeeusSun
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.11.1
     */
    @Test
    public void sunConstructorTest() throws PatriusException {
        // Initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        final BodyShape earth = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 0.0,
            FramesFactory.getGCRF());
        final Orbit orbit = new KeplerianOrbit(7500000.0, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Utils.mu);
        final SpacecraftState state = new SpacecraftState(orbit);
        // Different detector (Action.STOP instead of Action.CONTINUE)
        final NadirSolarIncidenceDetector detector1 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0);
        // Same detectors
        final NadirSolarIncidenceDetector detector2 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0,
            Action.CONTINUE);
        final NadirSolarIncidenceDetector detector3 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0,
            Action.CONTINUE, false);
        final NadirSolarIncidenceDetector detector4 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0,
            Action.CONTINUE, false, CelestialBodyFactory.getSun());
        final NadirSolarIncidenceDetector detector5 = new NadirSolarIncidenceDetector(2, -0.2, earth, 0, 0,
            Action.CONTINUE, Action.CONTINUE, false, false, CelestialBodyFactory.getSun());
        // Different detector (new MeeusSun() instead of CelestialBodyFactory.getSun())
        final NadirSolarIncidenceDetector detector6 = new NadirSolarIncidenceDetector(2, -0.2, earth, 0, 0,
            Action.CONTINUE, Action.CONTINUE, false, false, new MeeusSun());
        // Different detector (slope selection at 2, Action.CONTINUE at entry, Action.STOP at exit, remove true at final
        // entry and remove false at exit)
        final NadirSolarIncidenceDetector detector7 = new NadirSolarIncidenceDetector(2, -0.2, earth, 0, 0,
            Action.CONTINUE, Action.STOP, true, false, CelestialBodyFactory.getSun());
        // Different detector (slope selection at 0, Action.CONTINUE at entry, Action.STOP at exit, remove true at final
        // entry and remove false at exit)
        final NadirSolarIncidenceDetector detector8 = new NadirSolarIncidenceDetector(0, -0.2, earth, 0, 0,
            Action.CONTINUE, Action.STOP, true, false, CelestialBodyFactory.getSun());
        // Different detector (slope selection at 1, Action.CONTINUE at entry, Action.STOP at exit, remove true at final
        // entry and remove false at exit)
        final NadirSolarIncidenceDetector detector9 = new NadirSolarIncidenceDetector(1, -0.2, earth, 0, 0,
            Action.CONTINUE, Action.STOP, true, false, CelestialBodyFactory.getSun());
        // Different detector (slope selection at 1, Action.CONTINUE at entry, Action.STOP at exit, remove true at final
        // entry and remove false at exit and new MeeusSun() instead of CelestialBodyFactory.getSun())
        final NadirSolarIncidenceDetector detector10 = new NadirSolarIncidenceDetector(1, -0.2, earth, 0, 0,
            Action.CONTINUE, Action.STOP, true, false, new MeeusSun());

        // Checks on the copy function for the most complete and different detector (with respect to the one built by
        // using the simplest default constructor)
        final NadirSolarIncidenceDetector detector10copy = (NadirSolarIncidenceDetector) detector10.copy();
        Assert.assertTrue(detector10copy.actionAtEntry == detector10.actionAtEntry);
        Assert.assertTrue(detector10copy.actionAtExit == detector10.actionAtExit);
        Assert.assertTrue(detector10copy.removeAtEntry == detector10.removeAtEntry);
        Assert.assertTrue(detector10copy.removeAtExit == detector10.removeAtExit);
        Assert.assertTrue(detector10copy.shouldBeRemovedFlag == detector10.shouldBeRemovedFlag);
        Assert.assertTrue(detector10copy.getMaxCheckInterval() == detector10.getMaxCheckInterval());
        Assert.assertTrue(detector10copy.getPropagationDelayType() == detector10.getPropagationDelayType());
        Assert.assertTrue(detector10copy.getSlopeSelection() == detector10.getSlopeSelection());
        Assert.assertTrue(detector10copy.getSun() == detector10.getSun());
        Assert.assertTrue(detector10copy.getThreshold() == detector10.getThreshold());

        // Checks on the g function
        // Even if they have different actions, the g function of detectors 1 and 2 is the same
        Assert.assertTrue(detector1.g(state) == detector2.g(state));
        // The g function of detectors 2 and 3 is the same, because they are the same detector
        Assert.assertTrue(detector2.g(state) == detector3.g(state));
        // The g function of detectors 3 and 4 is the same, because they are the same detector
        Assert.assertTrue(detector3.g(state) == detector4.g(state));
        // The g function of detectors 4 and 5 is the same, because they are the same detector
        Assert.assertTrue(detector4.g(state) == detector5.g(state));
        // The g function of detectors 4 and 6 is different, because the Sun model is different
        Assert.assertFalse(detector4.g(state) == detector6.g(state));
        // Even if they have different actions, the g function of detectors 4 and 7 is the same
        Assert.assertTrue(detector4.g(state) == detector7.g(state));
        // Even if they have different actions and slope selections, the g function of detectors 4 and 8 is the same
        Assert.assertTrue(detector4.g(state) == detector8.g(state));
        // Even if they have different actions and slope selections, the g function of detectors 4 and 9 is the same
        Assert.assertTrue(detector4.g(state) == detector9.g(state));
        // The g function of detectors 4 and 10 is different, because the Sun model is different (not because they have
        // different actions and slope selections)
        Assert.assertFalse(detector4.g(state) == detector10.g(state));

        // Create a list of detectors
        final ArrayList<NadirSolarIncidenceDetector> detectorsList = new ArrayList<>();
        detectorsList.add(detector1);
        detectorsList.add(detector2);
        detectorsList.add(detector3);
        detectorsList.add(detector4);
        detectorsList.add(detector5);
        detectorsList.add(detector6);
        detectorsList.add(detector7);
        detectorsList.add(detector8);
        detectorsList.add(detector9);
        detectorsList.add(detector10);
        // Create a list with all possible values of the increasing boolean
        final ArrayList<Boolean> increasingList = new ArrayList<>();
        increasingList.add(true);
        increasingList.add(false);
        // Create a list with all possible values of the forward boolean
        final ArrayList<Boolean> forwardList = new ArrayList<>();
        forwardList.add(true);
        forwardList.add(false);
        // Define the ifCaseValue boolean to be computed with the ^ (exclusive or) operator
        boolean ifCaseValue;

        // Check on the eventOccurred function for all built detectors and for all increasing and forward values
        // possibilities
        for (final NadirSolarIncidenceDetector detector : detectorsList) {
            for (final boolean increasing : increasingList) {
                for (final boolean forward : forwardList) {
                    // Compute the if value built with the ^ (exclusive or) operator
                    ifCaseValue = ((forward == true && (!increasing) == false) || (forward == false && (!increasing) == true))
                        ? true : false;
                    // Check the action for the eventOccurred function
                    Assert.assertTrue(detector.eventOccurred(state, increasing, forward) == (ifCaseValue ? detector
                        .getActionAtEntry() : detector.getActionAtExit()));
                    // Check the shouldBeRemoved flag for the eventOccurred function
                    Assert.assertTrue(detector.shouldBeRemoved() == (ifCaseValue ? detector.isRemoveAtEntry()
                        : detector.isRemoveAtExit()));
                }
            }
        }
    }
}
