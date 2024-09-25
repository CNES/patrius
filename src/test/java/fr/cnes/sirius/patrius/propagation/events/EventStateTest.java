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
 * @history created 15/11/11
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.utils.EventState;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Unit tests for EventState.
 * 
 * @author clauded
 * 
 * @version $Id: EventStateTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EventStateTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EventState
         * 
         * @featureDescription the state for one {@link EventDetector event detector} during integration steps.
         * 
         * @coveredRequirements DV-EVT_10
         */
        VALIDATE_EVENTSTATE
    }

    /**
     * mu
     */
    private double mu;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description simple test
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria successfully detection
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEventState() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date,
            this.mu);

        final Propagator propagator = new KeplerianPropagator(orbit);
        final DateDetector detector = new DateDetector(date.shiftedBy(10.0), 0.5, 10);
        detector.addEventDate(date.shiftedBy(3));
        SpacecraftState s;
        propagator.addEventDetector(detector);
        s = propagator.propagate(date.shiftedBy(1));
        Assert.assertTrue(detector.g(s) > 0);

        detector.resetState(s);

        final Collection<EventDetector> events = propagator.getEventsDetectors();
        Assert.assertTrue(events.size() == 1);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description simple test that covers a special case, the corner case, when the convergence is reached.
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria successfully detection, no exception raised.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testCornerCaseNoException() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        this.mu = Constants.EGM96_EARTH_MU;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7500000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN, referenceFrame, date,
            this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final Propagator propagator = new KeplerianPropagator(orbit);

        propagator.resetInitialState(initialState);

        final EventDetector detector = new EventMock(date, 5, 0.05);
        propagator.addEventDetector(detector);

        propagator.propagate(date, date.shiftedBy(30));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description simple test that covers a case when the event's g() method raises an OrekitException.
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria a PropagationException is raised (it wraps the event detector exception).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         expected, as a PropagationException subclass.
     */
    @Test(expected = PropagationException.class)
    public void testGException() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        this.mu = Constants.EGM96_EARTH_MU;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7500000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN, referenceFrame, date,
            this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final Propagator propagator = new KeplerianPropagator(orbit);

        propagator.resetInitialState(initialState);

        final EventMock detector = new EventMock(date, 5, 0.05);
        propagator.addEventDetector(detector);
        // Makes the event detector fail after some calls
        detector.makeGFail(true, 10);

        propagator.propagate(date, date.shiftedBy(30));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description miscelaneous tests for coverage.
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria none
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testMiscCoverage() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        this.mu = Constants.EGM96_EARTH_MU;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7500000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN, referenceFrame, date,
            this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final Propagator propagator = new KeplerianPropagator(orbit);

        propagator.resetInitialState(initialState);

        final EventDetector detector = new DateDetector(initialState.getDate());

        final EventState myEventState = new EventState(detector);

        // Reset called as the detector is set for the initial date exactly (special case) => returned state should be
        // null
        final SpacecraftState nullState = myEventState.reset(initialState);
        Assert.assertNull(nullState);

    }

    /**
     * setup
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        this.mu = Constants.EIGEN5C_EARTH_MU;
    }
}
