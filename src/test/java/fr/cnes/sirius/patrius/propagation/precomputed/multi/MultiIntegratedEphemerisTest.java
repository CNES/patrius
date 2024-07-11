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
 * @history creation 27/04/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed.multi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.IntegratedEphemerisTest;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * Validation class for {@link MultiIntegratedEphemeris}
 * </p>
 * Validation class copied from {@link IntegratedEphemerisTest}
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MultiIntegratedEphemerisTest {

    /**
     * First state id
     */
    private static final String STATE1 = "state1";

    /**
     * Initial orbit
     */
    private Orbit initialOrbit;

    /**
     * Initial attitude
     */
    private Attitude initialAttitude;

    /**
     * Multi numerical propagator
     */
    private MultiNumericalPropagator numericalPropagator;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test class MultiIntegratedEphemeris
         * 
         * @featureDescription Test class MultiIntegratedEphemeris
         */
        VALIDATE_MULTI_INTEGRATED_EPHEMERIS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_INTEGRATED_EPHEMERIS}
     * 
     * @testedMethod {@link MultiNumericalPropagator#setEphemerisMode()}
     * @testedMethod {@link MultiNumericalPropagator#getGeneratedEphemeris(String)}
     * 
     * @description Test MultiIntegratedEphemeris
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testNormalKeplerIntegration() throws PatriusException {

        // Keplerian propagator definition
        final KeplerianPropagator keplerEx = new KeplerianPropagator(this.initialOrbit);

        // Integrated ephemeris

        // Propagation
        final MassProvider mass = new SimpleMassModel(20.0, "part");
        final AbsoluteDate finalDate = this.initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        this.numericalPropagator.setEphemerisMode();
        this.numericalPropagator.addInitialState(new SpacecraftState(this.initialOrbit, mass),
                STATE1);
        this.numericalPropagator.setMassProviderEquation(mass, STATE1);
        this.numericalPropagator.propagate(finalDate);
        Assert.assertTrue(this.numericalPropagator.getCalls() < 3200);
        final MultiIntegratedEphemeris ephemeris = (MultiIntegratedEphemeris) this.numericalPropagator
                .getGeneratedEphemeris(STATE1);

        // tests
        for (int i = 1; i <= Constants.JULIAN_DAY; i++) {
            final AbsoluteDate intermediateDate = this.initialOrbit.getDate().shiftedBy(i);
            final SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            final SpacecraftState numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            final Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            final Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
            Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 0.06);
        }
        this.setUp();
        // test inv
        final AbsoluteDate intermediateDate = this.initialOrbit.getDate().shiftedBy(41589);
        final SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        final SpacecraftState state = keplerEx.propagate(finalDate);
        this.numericalPropagator.addInitialState(state, STATE1);
        this.numericalPropagator.setEphemerisMode();
        this.numericalPropagator.propagate(this.initialOrbit.getDate());
        final BoundedPropagator invEphemeris = this.numericalPropagator
                .getGeneratedEphemeris(STATE1);
        final SpacecraftState numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        final Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        final Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
        Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 10e-2);

        // getPVCoordinates checks:
        final PVCoordinates pv = ephemeris.getPVCoordinates(finalDate, FramesFactory.getEME2000());
        final PVCoordinates pvExp = this.numericalPropagator.getPVCoordinates(finalDate,
                FramesFactory.getEME2000(), STATE1);
        Assert.assertEquals(0, pv.getPosition().subtract(pvExp.getPosition()).getNorm(), 10e-2);
        Assert.assertEquals(0, pv.getVelocity().subtract(pvExp.getVelocity()).getNorm(), 10e-2);

        // resetInitialState exception:
        boolean rez = false;
        try {
            ephemeris.resetInitialState(state);
            Assert.fail();
        } catch (final PropagationException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // setInterpolatedDate exception:
        rez = false;
        try {
            ephemeris.propagateOrbit(finalDate.shiftedBy(10000.0));
            Assert.fail();
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // mass verification:
        final SpacecraftState finalState = ephemeris.propagate(finalDate);
        Assert.assertEquals(20.0, finalState.getMass("part"), 0.0);
        // propagateOrbit coverage:
        Assert.assertNotNull(ephemeris.propagateOrbit(finalDate));
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the multi integrated ephemeris serialization / deserialization process.
     *
     * @testPassCriteria The multi integrated ephemeris can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final AbsoluteDate initialDate = this.initialOrbit.getDate();
        final AbsoluteDate finalDate = initialDate.shiftedBy(Constants.JULIAN_DAY);
        final Frame frame = FramesFactory.getGCRF();

        final MassProvider mass = new SimpleMassModel(20.0, "part");
        this.numericalPropagator.setEphemerisMode();
        this.numericalPropagator.addInitialState(new SpacecraftState(this.initialOrbit, mass),
                STATE1);
        this.numericalPropagator.setMassProviderEquation(mass, STATE1);
        this.numericalPropagator.propagate(finalDate);

        final MultiIntegratedEphemeris ephemeris = (MultiIntegratedEphemeris) this.numericalPropagator
                .getGeneratedEphemeris(STATE1);
        final MultiIntegratedEphemeris deserializedEphemeris = TestUtils
                .serializeAndRecover(ephemeris);

        for (int i = 0; i < 10; i++) {
            final AbsoluteDate currentDate = initialDate.shiftedBy(i * 3600.);
            Assert.assertEquals(ephemeris.getPVCoordinates(currentDate, frame),
                    deserializedEphemeris.getPVCoordinates(currentDate, frame));
        }
    }

    @Before
    public void setUp() throws PatriusException {
        // Definition of initial conditions with position and velocity
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final double mu = 3.9860047e14;

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        this.initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), initDate, mu);
        this.initialAttitude = new LofOffset(this.initialOrbit.getFrame(), LOFType.LVLH)
                .getAttitude(this.initialOrbit, this.initialOrbit.getDate(),
                        this.initialOrbit.getFrame());

        // Numerical propagator definition
        final double[] absTolerance = { 0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8 };
        final double[] relTolerance = { 1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 500,
                absTolerance, relTolerance);
        integrator.setInitialStepSize(100);
        this.numericalPropagator = new MultiNumericalPropagator(integrator);
    }
}
