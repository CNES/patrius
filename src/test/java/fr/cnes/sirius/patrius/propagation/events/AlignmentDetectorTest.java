/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AlignmentDetectorTest {

    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private Orbit orbit;

    @Test
    public void testAlignment() throws PatriusException {

        final double alignAngle = MathLib.toRadians(10.0);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // System.out.println(initialState.getPVCoordinates(FramesFactory.getGCRF()).getPosition());
        final AlignmentDetector detector =
            new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle);
        AlignmentDetector detector2 = (AlignmentDetector) detector.copy();
        this.propagator.addEventDetector(detector2);
        System.out.println(this.orbit.getKeplerianPeriod());
        SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(this.orbit.getKeplerianPeriod()));
        System.out.println(finalState.getDate().durationFrom(this.iniDate));
        // Assert.assertEquals(383.3662, finalState.getDate().durationFrom(iniDate), 1.0e-3);

        detector2 = new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle, 1.0e-13 * this.initialState
            .getOrbit().getKeplerianPeriod(), Action.CONTINUE, Action.CONTINUE);
        this.setUp();
        this.propagator.addEventDetector(detector2);
        finalState = this.propagator.propagate(this.iniDate.shiftedBy(this.orbit.getKeplerianPeriod()));
        Assert.assertEquals(this.iniDate.shiftedBy(this.orbit.getKeplerianPeriod()), finalState.getDate());

        detector2 = new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle, 1.0e-13 * this.initialState
            .getOrbit().getKeplerianPeriod(), Action.RESET_STATE, Action.CONTINUE);
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(finalState, true, false));
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(finalState, true, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(finalState, false, false));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(finalState, false, true));
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        this.orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.iniDate, mu);
        this.initialState = new SpacecraftState(this.orbit);
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
        this.propagator.setInitialState(this.initialState);
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
        this.propagator = null;
    }

}
