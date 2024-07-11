/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history Created 25/02/2012
  * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
  * VERSION::DM:596:12/04/2016:Improve test coherence
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.stela.StelaSpacecraftFactory;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link StelaSRPSquaring}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaSRPSquaringTest {

    /** mass */
    private static final double MASS = 1000;
    /** surface */
    private static final double SURF = 5;
    /** coefficient */
    private static final double CR = 2;
    /** threshold */
    private static final double EPS = Precision.EPSILON;

    /** SRP container */
    private StelaSRPSquaring srp;
    /** Orbit */
    private StelaEquinoctialOrbit orbit;
    /** Sun */
    private CelestialBody sun;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle SRP Stela Perturbation
         * 
         * @featureDescription Validation of perturbations, quadratic approximation
         * 
         * @coveredRequirements
         */
        SRP_PERTURBATION,

        /**
         * @featureTitle SRP Stela Perturbation
         * 
         * @featureDescription Validation of short periods, method not implemented
         * 
         * @coveredRequirements
         */
        SRP_SHORT,

        /**
         * @featureTitle SRP Perturbation
         * 
         * @featureDescription Validation of partial derivatives, potential approximation
         * 
         * @coveredRequirements
         */
        SRP_DV

    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_SHORT}
     * 
     * @testedMethod {@link StelaSRPSquaring#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description tests short periods method, not implemented yet, returns zero
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria the computed perturbations are the same as the expected ones (new double[6]), the threshold is
     *                   the largest double-precision floating-point number such that 1 + EPSILON is numerically equal
     *                   to 1. This value is an upper bound on the relative error due to rounding real numbers to double
     *                   precision floating-point numbers.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testShort() throws PatriusException {
        this.assertEquals(new double[6], this.srp.computeShortPeriods(this.orbit), EPS);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_DV}
     * 
     * @testedMethod {@link StelaSRPSquaring#computePartialDerivatives(StelaEquinoctialOrbit)}
     * 
     * @description tests partial derivatives method
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria the computed derivatives are the same as the expected ones from SRPPotential, the threshold is
     *                   the largest double-precision floating-point number such that 1 + EPSILON is numerically equal
     *                   to 1. This value is an upper bound on the relative error due to rounding real numbers to double
     *                   precision floating-point numbers.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testPartialDV() throws PatriusException {
        final SRPPotential sq = new SRPPotential(this.sun, MASS, SURF, CR);
        final double[][] result = this.srp.computePartialDerivatives(this.orbit);
        final double[][] expected = sq.computePartialDerivatives(this.orbit);
        this.assertEquals(expected, result, EPS);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_PERTURBATION}
     * 
     * @testedMethod {@link StelaSRPSquaring#computePerturbation(StelaEquinoctialOrbit, OrbitNatureConverter)}
     * 
     * @description tests perturbation method
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria the computed perturbations are the same as the expected ones from SRPSquaring, the threshold is
     *                   the largest double-precision floating-point number such that 1 + EPSILON is numerically equal
     *                   to 1. This value is an upper bound on the relative error due to rounding real numbers to double
     *                   precision floating-point numbers.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testPerturbations() throws PatriusException {
        final SRPSquaring sq = new SRPSquaring(new DirectRadiativeModel(
            StelaSpacecraftFactory.createStelaCompatibleSpacecraft("main", MASS, 0, 0, SURF, CR)), this.sun,
            Constants.CNES_STELA_AE);
        final double[] result = this.srp.computePerturbation(this.orbit, null);
        final double[] expected = sq.computePerturbation(this.orbit, null);
        this.assertEquals(expected, result, EPS);
        final double[] result22 = this.srp.getdPert();
        this.assertEquals(result, result22, 0);

        final SRPPotential sp = new SRPPotential(this.sun, MASS, SURF, CR);
        final double[] result2 = this.srp.computePotentialPerturbation(this.orbit);
        final double[] expected2 = sp.computePerturbation(this.orbit);
        this.assertEquals(expected2, result2, EPS);

    }

    /**
     * setup
     * 
     * @throws PatriusException
     *         if fails
     */
    @Before
    public void setup() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        this.sun = CelestialBodyFactory.getSun();
        this.srp = new StelaSRPSquaring(MASS, SURF, CR, 11, this.sun);

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2011, 1), TimeComponents.H00,
            TimeScalesFactory.getUT1());

        this.orbit = new StelaEquinoctialOrbit(2.43505E+7, 0.3644381018870251, 0.6312253086822904, 0.09052430460833645,
            0.05226423163382672, 1.919862177193762, FramesFactory.getMOD(false), date, Constants.CNES_STELA_MU);

    }

    /**
     * Tests for other constructor
     * 
     * @throws PatriusException
     *         if fails
     */
    @After
    public void teardown() throws PatriusException {
        this.srp = new StelaSRPSquaring(MASS, SURF, CR, 11, this.sun, Constants.CNES_STELA_AE, Constants.CNES_STELA_UA,
            Constants.CONST_SOL_STELA);
        this.testPartialDV();
        this.testPerturbations();
        this.testShort();
    }

    void assertEquals(final double[] exp, final double[] act, final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            Assert.assertEquals(exp[i], act[i], eps);
        }
    }

    void assertEquals(final double[][] exp, final double[][] act, final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            this.assertEquals(exp[i], act[i], eps);
        }
    }
}
