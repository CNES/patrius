/**
 *
 * Copyright 2011-2017 CNES
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
 * @history Created 02/03/2015
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:02/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.noninertial;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.stela.propagation.ForcesStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for {@link NonInertialContribution}.
 * 
 * @author Emmanuel Bignon
 * @version $Id$
 * @since 3.0
 */
public class NonInertialContributionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle STELA frame conversion test
         * 
         * @featureDescription test frame conversion using STELA specific frame configuration
         * 
         * @coveredRequirements TODO
         */
        STELA_FRAME_CONVERSION,
        /**
         * @featureTitle STELA non inertial contribution test
         * 
         * @featureDescription test non inertial contribution
         * 
         * @coveredRequirements TODO
         */
        STELA_NON_INERTIAL_CONTRIBUTION
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(NonInertialContributionTest.class.getSimpleName(), "STELA non inertial force");
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_FRAME_CONVERSION}
     * @testedMethod {@link NonInertialContribution#computeOmega(AbsoluteDate, org.orekit.frames.Frame, org.orekit.frames.Frame)}
     *               and
     *               {@link NonInertialContribution#computeOmegaDerivative(AbsoluteDate, org.orekit.frames.Frame, org.orekit.frames.Frame)}
     * @description tests computation of rotation vector and its time derivative
     * @input date
     * @output rotation vector and its time derivative
     * @testPassCriteria rotation vector and its time derivative are the same as STELA-LOS reference at 1E-11 and 1E-9
     *                   (relative tol).
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testOmegaICRFToCIRF() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Input data
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 36525 + 35);
        final NonInertialContribution force = new NonInertialContribution(11, FramesFactory.getICRF());

        // Get rotation and rotation rate
        final double[] omega = force.computeOmega(date, FramesFactory.getGCRF(), FramesFactory.getCIRF()).toArray();
        final double[] omegaDot = force.computeOmegaDerivative(date, FramesFactory.getGCRF(), FramesFactory.getCIRF(),
            86400.).toArray();

        // Check rotation and rotation rate
        final double[] expOmega = { 3.7722814102527E-14, 3.9430855317564E-12, -1.3354427617204E-16 };
        final double[] expOmegaDot = { -4.4869080781616E-19, -1.6273634904708E-19, 8.7487660054003E-24 };
        checkDoubleRel(expOmega, omega, 1E-11);
        checkDoubleRel(expOmegaDot, omegaDot, 1E-9);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#STELA_FRAME_CONVERSION}
     * @testedMethod {@link NonInertialContribution#computeOmega(AbsoluteDate, org.orekit.frames.Frame, org.orekit.frames.Frame)}
     *               and
     *               {@link NonInertialContribution#computeOmegaDerivative(AbsoluteDate, org.orekit.frames.Frame, org.orekit.frames.Frame)}
     * @description tests exceptions thrown by non-inertial contribution
     * @input date
     * @testPassCriteria exceptions properly caught
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testExceptions() {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Input data
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 36525 + 35);
        final NonInertialContribution force = new NonInertialContribution(11, FramesFactory.getGCRF());

        // Compute omega unknown frames
        try {
            force.computeOmega(date, FramesFactory.getTIRF(), FramesFactory.getCIRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            force.computeOmega(date, FramesFactory.getGCRF(), FramesFactory.getEME2000());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Using force step handlers (exception thrown because of non-even quadrature points number)
        final NonInertialContribution force2 = new NonInertialContribution(10, FramesFactory.getGCRF());
        final ForcesStepHandler handler = new ForcesStepHandler(null, force2);
        final SpacecraftState state = new SpacecraftState(new StelaEquinoctialOrbit(7E7, 0, 0, 0, 0, 0,
            FramesFactory.getGCRF(), date, Constants.CNES_STELA_MU));

        try {
            handler.init(state, date);
            Assert.fail();
        } catch (final PatriusRuntimeException e) {
            Assert.assertTrue(true);
        }

        try {
            final PatriusStepInterpolator interpolator = new PatriusStepInterpolator(){
                @Override
                public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {
                }

                @Override
                public boolean isForward() {
                    return true;
                }

                @Override
                public AbsoluteDate getPreviousDate() {
                    return null;
                }

                @Override
                public SpacecraftState getInterpolatedState() throws PatriusException {
                    return state;
                }

                @Override
                public AbsoluteDate getInterpolatedDate() {
                    return null;
                }

                @Override
                public AbsoluteDate getCurrentDate() {
                    return null;
                }
            };
            handler.handleStep(interpolator, false);
            Assert.fail();
        } catch (final PropagationException e) {
            Assert.assertTrue(true);
        }

        MeeusMoonStela.resetTransform();
        MeeusSun.resetTransform();
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_NON_INERTIAL_CONTRIBUTION}
     * @testedMethod {@link NonInertialContribution#computePerturbation(fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit, fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter)}
     * @description tests computation non-inertial contribution (GCRF reference system, CIRF integration frame)
     * @input orbit
     * @output non-inertial contribution (CIRF/GCRF)
     * @testPassCriteria non-inertial contribution to dE/dt are the same as STELA-LOS reference at 1E-9 (same relative
     *                   tol).
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputePerturbationCIRF() throws PatriusException {

        Report.printMethodHeader("testComputePerturbationCIRF", "Non-inertial contribution computation (CIRF)",
            "STELA 2.6", 1E-9, ComparisonType.RELATIVE);

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Input data
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 36525 + 35);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(7078136.29999999981, 0.01412801276516814, 0.,
            0.75470958022277190, 0., 0.,
            FramesFactory.getMOD(false), date, Constants.CNES_STELA_MU, false);
        final NonInertialContribution force = new NonInertialContribution(7, FramesFactory.getGCRF());

        // Get actual and expected
        final double[] actual = force.computePerturbation(orbit, null);
        final double[] expected = { -2.1515385250526E-09, 1.6249287561580E-11, 5.3706327264998E-18,
            6.4089997998365E-14,
            -1.2370679886175E-14, 4.1849494191669E-13 };

        // Check
        checkDoubleRel(expected, actual, 1E-9);

        Report.printToReport("Perturbation", expected, actual);

        // Other checks (coverage)
        final double[] zero = new double[6];
        final double[][] partials = force.computePartialDerivatives(orbit);
        checkDoubleAbs(zero, force.computeShortPeriods(orbit), 0);
        checkDoubleAbs(zero, partials[0], 0);
        checkDoubleAbs(zero, partials[1], 0);
        checkDoubleAbs(zero, partials[2], 0);
        checkDoubleAbs(zero, partials[3], 0);
        checkDoubleAbs(zero, partials[4], 0);
        checkDoubleAbs(zero, partials[5], 0);
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_NON_INERTIAL_CONTRIBUTION}
     * @testedMethod {@link NonInertialContribution#computePerturbation(fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit, fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter)}
     * @description tests computation non-inertial contribution (MOD reference system, CIRF integration frame)
     * @input orbit
     * @output non-inertial contribution (CIRF/MOD)
     * @testPassCriteria non-inertial contribution to dE/dt are the same as STELA-LOS reference at 1E-9 (same relative
     *                   tol).
     * @referenceVersion 3.0
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputePerturbationMOD() throws PatriusException {

        Report.printMethodHeader("testComputePerturbationMOD", "Non-inertial contribution computation (MOD)",
            "STELA 2.6", 1E-9, ComparisonType.RELATIVE);

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Input data
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 36525 + 35);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(7078136.29999999981, 0.01412801276516814, 0.,
            0.75470958022277190, 0., 0.,
            FramesFactory.getMOD(false), date, Constants.CNES_STELA_MU, false);
        final NonInertialContribution force = new NonInertialContribution(7, FramesFactory.getMOD(false));

        // Get actual and expected
        final double[] actual = force.computePerturbation(orbit, null);
        final double[] expected = { -2.151528939737266082E-09, -5.644439198237828294E-13, 5.370616811734228803E-18,
            -8.607998267265514415E-14, -1.011758238563792557E-15, -5.257550254556734523E-12 };

        // Check
        checkDoubleRel(expected, actual, 1E-9);

        Report.printToReport("Perturbation", expected, actual);

        // Other checks (coverage)
        final double[] zero = new double[6];
        final double[][] partials = force.computePartialDerivatives(orbit);
        checkDoubleAbs(zero, force.computeShortPeriods(orbit), 0);
        checkDoubleAbs(zero, partials[0], 0);
        checkDoubleAbs(zero, partials[1], 0);
        checkDoubleAbs(zero, partials[2], 0);
        checkDoubleAbs(zero, partials[3], 0);
        checkDoubleAbs(zero, partials[4], 0);
        checkDoubleAbs(zero, partials[5], 0);
    }

    /**
     * Relative check for double[] array.
     */
    public static void checkDoubleRel(final double[] dExpected, final double[] dActual, final double tol) {
        Assert.assertEquals(dExpected.length, dActual.length);
        for (int i = 0; i < dExpected.length; i++) {
            Assert.assertEquals(0, MathLib.abs((dExpected[i] - dActual[i]) / dExpected[i]), tol);
        }
    }

    /**
     * absolute check for double[] array.
     */
    public static void checkDoubleAbs(final double[] dExpected, final double[] dActual, final double tol) {
        Assert.assertEquals(dExpected.length, dActual.length);
        for (int i = 0; i < dExpected.length; i++) {
            Assert.assertEquals(0, MathLib.abs(dExpected[i] - dActual[i]), tol);
        }
    }
}
