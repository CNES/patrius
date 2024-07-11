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
 * @history creation 25/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:200:28/08/2014:(creation) dealing with a negative mass in the propagator
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NullMassPartDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AblationTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation test for DM 200 : ablation test case.
         * 
         * @featureDescription Tests the detectors for null mass parts and global mass
         * 
         */
        NULL_MASS_DETECTORS_TEST,
    }

    /**
     * Validation test for DM 200 : ablation test case.
     * 
     * @throws PatriusException
     *         bcs of TimeScalesFactory.getUTC()
     * @throws IllegalArgumentException
     *         bcs of TimeScalesFactory.getUTC()
     */
    @Test
    public void testAblation() throws IllegalArgumentException, PatriusException {

        // mass model
        final double mass = 1000;
        final String partName = "satellite";
        final MassProvider massModel = new SimpleMassModel(mass, partName);
        /*
         * Spacecraft state
         */
        // frame and date
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate d0 = new AbsoluteDate();

        // orbit
        final double muValue = Constants.GRIM5C1_EARTH_MU;
        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 400e3;
        final double e = .0001;
        final double i = 60 * 3.14 / 180;
        final double raan = 0;
        final double pa = 270 * 3.14 / 180;
        final double w = 0;

        // state
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, d0, muValue);
        final SpacecraftState spc = new SpacecraftState(orbit, massModel);

        // numerical propagator
        final FirstOrderIntegrator dop = new ClassicalRungeKuttaIntegrator(60);

        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(spc);
        propagator.setMassProviderEquation(massModel);
        propagator.addForceModel(new AblationForce(massModel, partName));

        // The ablation force model as a partial derivative of -1 at each step
        final SpacecraftState finalState = propagator.propagate(d0.shiftedBy(mass));
        // Propagation should end without error
        Assert.assertEquals(mass, finalState.getDate().durationFrom(d0), Utils.epsilonTest);

    }

    /**
     * Ablation force: only decreases mass from provided part.
     * 
     * @author Sophie Laurens
     * 
     * @version $Id: AblationTest.java 18094 2017-10-02 17:18:57Z bignon $
     * 
     * @since 2.3
     * 
     */
    private class AblationForce extends Parameterizable implements ForceModel {

        /** Default serial version. */
        private static final long serialVersionUID = 1L;

        /** boolean for removal of mass dv. */
        private boolean removeMassDv;

        /** Mass model. */
        private final MassProvider massModel;

        /** Name of the part ablated. */
        private final String partName;

        /**
         * Constructor.
         * 
         * @param massProvider2
         *        massProvider
         * @param partName2
         *        String
         */
        public AblationForce(final MassProvider massProvider2, final String partName2) {
            this.massModel = massProvider2;
            this.partName = partName2;
        }

        /** {@inheritDoc} */
        @Override
        public void
                addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                              throws PatriusException {

            // compute thrust acceleration in inertial frame
            adder.addAcceleration(this.computeAcceleration(s), s.getFrame());

            // add flow rate to mass variation
            if (!this.removeMassDv) {
                // add flow rate to mass variation
                this.massModel.addMassDerivative(this.partName, -1.0);
                this.removeMassDv = true;
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @throws PatriusException
         *         if the mass becomes negative
         */
        @Override
        public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
            return Vector3D.ZERO;
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector[] getEventsDetectors() {
            return new EventDetector[] { new NullMassPartDetector(this.massModel, this.partName) };
        }
        
        /** {@inheritDoc} */
        @Override
        public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
            // Nothing to do
        }
    }

    /**
     * Launched before every tests.
     */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
