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
import fr.cnes.sirius.patrius.propagation.events.LocalTimeAngleDetectorTest.features;
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
    public final void testNAdirSunIncidenceDetector() throws PatriusException {

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
    public final void testNadirSolarIncidenceDetectorCtor() throws PatriusException {

        final double incidenceToDetect = 3 * FastMath.PI / 8.;
        // earth
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final double r = 6000000.0;
        final BodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);

        final NadirSolarIncidenceDetector detector = new NadirSolarIncidenceDetector(incidenceToDetect, earth,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP);
        // Test getters
        Assert.assertEquals(earth, detector.getEarthShape());
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
     * @testPassCriteria g value is the same when using default Sun and CelestialBodyFactory.getSun() and different when using MeeusSun
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void sunConstructorTest() throws PatriusException {
        // Initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        final BodyShape earth = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 0.0, FramesFactory.getGCRF());
        final Orbit orbit = new KeplerianOrbit(7500000.0, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Utils.mu);
        final SpacecraftState state = new SpacecraftState(orbit);
        // Same detectors
        final EventDetector detector2 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0, Action.CONTINUE, false);
        final EventDetector detector3 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0, Action.CONTINUE, false, CelestialBodyFactory.getSun());
        // Different detector
        final EventDetector detector1 = new NadirSolarIncidenceDetector(-0.2, earth, 0, 0, Action.CONTINUE, false, new MeeusSun());
        // Checks
        Assert.assertFalse(detector1.g(state) == detector2.g(state));
        Assert.assertTrue(detector2.g(state) == detector3.g(state));
    }
}
