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
 * Copyright 2002-2011 CS Communication & Systèmes
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

import org.junit.After;
import org.junit.Assert;
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
import fr.cnes.sirius.patrius.propagation.events.AlignmentDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for AlignmentDetector.<br>
 * Class to be merged with the existing AlignmentDetectorTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author cardosop
 * 
 * @version $Id: AlignmentDetectorTest.java 17917 2017-09-11 12:55:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AlignmentDetectorTest {

    /*
     * The following code is lifted form Orekit.
     * It should NOT BE MERGED BACK into Orekit!
     */

    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private final double timeEpsilon = 1.0e-4;

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

            final double mu = 3.9860047e14;
            final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.iniDate, mu);
            this.initialState = new SpacecraftState(orbit);
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
        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
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
         * @featureTitle Validation of the alignment detector
         * 
         * @featureDescription Validation of the alignment detector
         * 
         * @coveredRequirements DV-EVT_120
         */
        VALIDATION_ALIGNMENT_DETECTOR;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ALIGNMENT_DETECTOR}
     * 
     * @testedMethod {@link AlignmentDetector#AlignmentDetector(Orbit, PVCoordinatesProvider, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output an {@link AlignmentDetector}
     * 
     * @testPassCriteria the {@link AlignmentDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testAlignmentDetectordoubleOrbitPVCdouble() throws PatriusException {
        final double expectedThr = 0.001232;
        final double alignAngle = MathLib.toRadians(0.0);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AlignmentDetector alignDetector =
            new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle, expectedThr);
        Assert.assertEquals(expectedThr, alignDetector.getThreshold(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ALIGNMENT_DETECTOR}
     * 
     * @testedMethod {@link AlignmentDetector#getPVCoordinatesProvider()}
     * @testedMethod {@link AlignmentDetector#getAlignAngle()}
     * 
     * @description test for the simple getters of {@link AlignmentDetector}
     * 
     * @input constructor parameters
     * 
     * @output getters' results from an {@link AlignmentDetector} instance
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
    public final void testAlignmentDetectorSimpleGetters() throws PatriusException {
        final double expectedThr = 0.001232;
        final double alignAngle = MathLib.toRadians(0.4432);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AlignmentDetector alignDetector =
            new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle, expectedThr);
        Assert.assertEquals(sun, alignDetector.getPVCoordinatesProvider());
        Assert.assertEquals(alignAngle, alignDetector.getAlignAngle(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ALIGNMENT_DETECTOR}
     * 
     * @testedMethod {@link AlignmentDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description unit test for eventOccured
     * 
     * @input constructor parameters and a {@link SpacecraftState} instance
     * 
     * @output two return values of eventOccured
     * 
     * @testPassCriteria the two return values are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testEventOccured() throws PatriusException {
        final double expectedThr = 0.001232;
        final double alignAngle = MathLib.toRadians(0.0);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AlignmentDetector alignDetector =
            new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle, expectedThr);
        // We check the two possible eventOccured outputs
        Action rez = alignDetector.eventOccurred(this.initialState, true, true);
        Assert.assertEquals(Action.STOP, rez);
        rez = alignDetector.eventOccurred(this.initialState, false, true);
        Assert.assertEquals(Action.CONTINUE, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ALIGNMENT_DETECTOR}
     * 
     * @testedMethod {@link AlignmentDetector#g(SpacecraftState)}
     * 
     * @description unit test, added for code coverage of the g method
     * 
     * @input constructor parameters and a {@link SpacecraftState} instance
     * 
     * @output alignment detection results
     * 
     * @testPassCriteria alignment correctly detected, with an epsilon of 1.e-4 on time
     *                   due to the precision of the reference value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testAlignment2() throws PatriusException {
        final double alignAngle = MathLib.toRadians(90.);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AlignmentDetector alignDetector =
            new AlignmentDetector(this.initialState.getOrbit(), sun, alignAngle);
        this.propagator.addEventDetector(alignDetector);
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(4820.3648, finalState.getDate().durationFrom(this.iniDate), this.timeEpsilon);
    }

}
