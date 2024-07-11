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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

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
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DateDetectorTest {

    /** Maximum checking intervals. */
    private double maxCheck;
    /** Threshold. */
    private double threshold;
    /** Step time. */
    private double dt;
    /** Initial orbit. */
    private Orbit iniOrbit;
    /** Initial date. */
    private AbsoluteDate iniDate;
    /** Propagator. */
    private NumericalPropagator propagator;

    /**
     * Set up.
     */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataCNES-2003");
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
    }

    /**
     * Teardown.
     */
    @After
    public void tearDown() {
        this.iniDate = null;
        this.propagator = null;
    }

    /*
     * ****
     * The unit tests below are to be merged within Orekit eventually.
     * Consider handling the SIRIUS specific data in some proper way.
     * ****
     */

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the date detector
         * 
         * @featureDescription Validation of the date detector
         * 
         * @coveredRequirements DV-EVT_120
         */
        VALIDATION_DATE_DETECTOR;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DATE_DETECTOR}
     * 
     * @testedMethod {@link DateDetector#getMaxCheckInterval()}
     * @testedMethod {@link DateDetector#getDate()}
     * 
     * @description test for the simple getters of {@link DateDetector}
     * 
     * @input constructor parameters
     * 
     * @output getters' results from a {@link DateDetector} instance
     * 
     * @testPassCriteria getters' results as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testDateDetectorSimpleGetters() throws PatriusException {
        DateDetector detector = new DateDetector(this.maxCheck, this.threshold);
        Assert.assertEquals(this.maxCheck, detector.getMaxCheckInterval(), 0.);
        Assert.assertEquals(null, detector.getDate());
        detector = new DateDetector(this.iniDate.shiftedBy(2.0 * this.dt), this.maxCheck, this.threshold);
        Assert.assertEquals(this.iniDate.shiftedBy(2.0 * this.dt), detector.getDate());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DATE_DETECTOR}
     * 
     * @testedMethod {@link DateDetector#addEventDate(AbsoluteDate)}
     * 
     * @description test for coverage of addEventDate
     * 
     * @input several event dates added to the DateDetector
     * 
     * @output none
     * 
     * @testPassCriteria no exception raised while adding event dates. An epsilon of 1.e-9
     *                   is used for duration comparisons (threshold).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testAddEventDate() throws PatriusException {
        final DateDetector detector = new DateDetector(this.maxCheck, this.threshold);
        this.propagator.addEventDetector(detector);
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt));
        // The date detector is at this point dateless
        // so the propagation proceeds to the end
        Assert.assertEquals(100. * this.dt, finalState.getDate().durationFrom(this.iniDate), this.threshold);
        // The remainder is for code coverage mostly and check that event have been added :
        // add a date to detect before the last date sent to the detector
        detector.addEventDate(this.iniDate.shiftedBy(99. * this.dt));
        Assert.assertEquals(100. * this.dt,
            this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt)).getDate().durationFrom(this.iniDate),
            this.threshold);
        // and another one before
        detector.addEventDate(this.iniDate.shiftedBy(98. * this.dt));
        Assert.assertEquals(100. * this.dt,
            this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt)).getDate().durationFrom(this.iniDate),
            this.threshold);

        // and another one before
        detector.addEventDate(this.iniDate.shiftedBy(97. * this.dt));
        Assert.assertEquals(100. * this.dt,
            this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt)).getDate().durationFrom(this.iniDate),
            this.threshold);

        // and now, one after
        detector.addEventDate(this.iniDate.shiftedBy(101. * this.dt));
        Assert.assertEquals(100. * this.dt,
            this.propagator.propagate(this.iniDate.shiftedBy(100. * this.dt)).getDate().durationFrom(this.iniDate),
            this.threshold);
        // All should have worked without raising an exception
    }

}
