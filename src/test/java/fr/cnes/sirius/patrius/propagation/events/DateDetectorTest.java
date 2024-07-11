/**
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:394:30/03/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 *
 */
public class DateDetectorTest {

    private int evtno = 0;
    private double maxCheck;
    private double threshold;
    private double dt;
    private Orbit iniOrbit;
    private AbsoluteDate iniDate;
    private AbsoluteDate nodeDate;
    private DateDetector dateDetector;
    private NumericalPropagator propagator;

    @Test
    public void testSimpleTimer() throws PatriusException {
        final EventDetector dateDetector =
            new DateDetector(this.iniDate.shiftedBy(2.0 * this.dt), this.maxCheck, this.threshold);
        this.propagator.addEventDetector(dateDetector);
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt));

        Assert.assertEquals(2.0 * this.dt, finalState.getDate().durationFrom(this.iniDate), this.threshold);
    }

    @Test
    public void testEmbeddedTimer() throws PatriusException {
        this.dateDetector = new DateDetector(this.maxCheck, this.threshold);
        final EventDetector nodeDetector = new NodeDetector(this.iniOrbit, this.iniOrbit.getFrame(), 2){
            private static final long serialVersionUID = 3583432139818469589L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    DateDetectorTest.this.nodeDate = s.getDate();
                    DateDetectorTest.this.dateDetector.addEventDate(DateDetectorTest.this.nodeDate
                        .shiftedBy(DateDetectorTest.this.dt));
                }
                return Action.CONTINUE;
            }
        };

        this.propagator.addEventDetector(nodeDetector);
        this.propagator.addEventDetector(this.dateDetector);
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt));

        Assert.assertEquals(this.dt, finalState.getDate().durationFrom(this.nodeDate), this.threshold);
    }

    @Test
    public void testAutoEmbeddedTimer() throws PatriusException {
        this.dateDetector = new DateDetector(this.iniDate.shiftedBy(-this.dt), this.maxCheck, this.threshold){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                final AbsoluteDate nextDate = s.getDate().shiftedBy(-DateDetectorTest.this.dt);
                this.addEventDate(nextDate);
                ++DateDetectorTest.this.evtno;
                return Action.CONTINUE;
            }
        };
        this.propagator.addEventDetector(this.dateDetector);
        this.propagator.propagate(this.iniDate.shiftedBy(-100. * this.dt));

        Assert.assertEquals(99, this.evtno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionTimer() throws PatriusException {
        this.dateDetector = new DateDetector(this.iniDate.shiftedBy(this.dt), this.maxCheck, this.threshold){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                final double step =
                    (DateDetectorTest.this.evtno % 2 == 0) ? 2. * DateDetectorTest.this.maxCheck
                        : DateDetectorTest.this.maxCheck / 2.;
                final AbsoluteDate nextDate = s.getDate().shiftedBy(step);
                this.addEventDate(nextDate);
                ++DateDetectorTest.this.evtno;
                return Action.CONTINUE;
            }
        };
        this.propagator.addEventDetector(this.dateDetector);
        this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt));
    }

    @Test
    public void testConstructor() throws PatriusException {
        final DateDetector detector1 = new DateDetector(this.maxCheck, this.threshold, Action.STOP);
        // test getter
        Assert.assertEquals(null, detector1.getDate());
        final DateDetector detector2 =
            new DateDetector(this.iniDate.shiftedBy(2.0 * this.dt), this.maxCheck, this.threshold, Action.STOP);
        final DateDetector detector3 = (DateDetector) detector2.copy();
        // test getter
        Assert.assertEquals(this.iniDate.shiftedBy(2.0 * this.dt), detector3.getDate());

        Assert.assertEquals(Action.STOP, detector3.getAction());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        this.iniOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.iniDate, mu);
        final SpacecraftState initialState = new SpacecraftState(this.iniOrbit);
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
        };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
        };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        this.propagator = new NumericalPropagator(integrator);
        this.propagator.setInitialState(initialState);
        this.dt = 60.;
        this.maxCheck = 10.;
        this.threshold = 10.e-10;
        this.evtno = 0;
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.propagator = null;
        this.dateDetector = null;
    }

}
