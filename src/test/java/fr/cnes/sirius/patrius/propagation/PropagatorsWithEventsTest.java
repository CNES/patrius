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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:578:23/03/2016:Bug in management of deleted events
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
/**
 * 
 * 
 * @history created 29/03/2016
 */
package fr.cnes.sirius.patrius.propagation;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for Orekit analytical propagator with event.
 * This class was be set in PATRIUS since test cases requires some PATRIUS objects.
 * 
 * @version $Id$
 * @since 3.2
 */
public class PropagatorsWithEventsTest {

    /** numerical extrapolator */
    NumericalPropagator numericalExtrapolator;

    /** analytical extrapolator */
    KeplerianPropagator analyticalExtrapolator;

    /** init date */
    AbsoluteDate initDate;

    /** init orbit */
    Orbit initialOrbit;

    /**
     * FT-578.
     * 
     * @testType UT
     * 
     * @description Perform analytical and numerical propagation with detector removal. Only one event should be
     *              detected
     * 
     * @testPassCriteria only one event is detected in both propagation (analytical and numerical)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testRdvDateDetector() throws PatriusException {

        // Analytical propagation
        Assert.assertTrue(this.propagate(true) == 1);

        // Numerical propagation
        Assert.assertTrue(this.propagate(false) == 1);
    }

    /**
     * Private method to perform analytical of numerical propagation
     * 
     * @throws PatriusException
     * @return number of events detected
     */
    private int propagate(final boolean analytical) throws PatriusException {

        Propagator extrapolator;
        if (analytical) {
            extrapolator = this.analyticalExtrapolator;
        } else {
            extrapolator = this.numericalExtrapolator;
        }

        // Time of extrapolation: 1 year
        final double delta_t_oneYear = 60 * 60 * 24 * 30 * 12;
        final AbsoluteDate extrapDate = this.initDate.shiftedBy(delta_t_oneYear);

        // Date event: 1 month
        final double delta_t_oneMonth = 60 * 60 * 24 * 30;
        final RdvDateDetector dateDetector = new RdvDateDetector(this.initDate.shiftedBy(delta_t_oneMonth));
        extrapolator.addEventDetector(dateDetector);

        // Propagation
        extrapolator.propagate(extrapDate);

        // Return number of event detected
        return dateDetector.getNumberEventDetected();
    }

    /**
     * Private class for date detector.
     */
    private class RdvDateDetector extends AbstractDetector {

        /** serial id */
        private static final long serialVersionUID = -6261954145648560754L;

        /** Rdv date */
        private final AbsoluteDate rdvDate;

        /** Boolean: event was detected */
        private boolean eventWasDetected;

        /** Number of times event id detected */
        private int numberEventDetected;

        /** Event detector to detect the nth occurrence of a certain aol */
        public RdvDateDetector(final AbsoluteDate rdvDate) {
            super(EventDetector.INCREASING, AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD * 0.0000001);

            this.rdvDate = rdvDate;
            this.eventWasDetected = false;

        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            this.eventWasDetected = true;
            this.numberEventDetected++;
            return Action.CONTINUE;
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            if (this.eventWasDetected) {
                // Should never get there!
                Assert.fail();
                this.numberEventDetected++;
            }

            return s.getDate().durationFrom(this.rdvDate);
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            return oldState;
        }

        @Override
        public boolean shouldBeRemoved() {
            return true;
        }

        /**
         * @return the numberEventDetected
         */
        public int getNumberEventDetected() {
            return this.numberEventDetected;
        }

        @Override
        public EventDetector copy() {
            return null;
        }
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {

        // mu
        final double mu = 3.9860047e14;

        // Set date
        this.initDate = AbsoluteDate.J2000_EPOCH;

        // Create new orbit
        this.initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9, 6.2, PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.initDate, mu);

        // // // Analytical Propagator // // //
        this.analyticalExtrapolator = new KeplerianPropagator(this.initialOrbit);

        // // // Numerical Propagator // // //

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("body");
        builder.addProperty(new MassProperty(5000.), "body");
        final Assembly assembly = builder.returnAssembly();

        // Initial mass provider
        final MassProvider massModel = new MassModel(assembly);

        // Initial Spacecraft State
        final SpacecraftState initState = new SpacecraftState(this.initialOrbit, massModel);

        // Integrator and propagator
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(450);

        this.numericalExtrapolator = new NumericalPropagator(integrator);

        // Initialize state
        this.numericalExtrapolator.setInitialState(initState);

        // add equations associated with mass model
        this.numericalExtrapolator.setMassProviderEquation(massModel);
    }
}
