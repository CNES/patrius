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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Ajout des courtes periodes dues a la traînee atmospherique et a la pression de radiation solaire dans STELA
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.stela.StelaSpacecraftFactory;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
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
                StelaSpacecraftFactory.createStelaCompatibleSpacecraft("main2", MASS, 0, 0, SURF, CR)), this.sun,
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
     * @testedFeature {@link features#SRP_SHORT}
     * 
     * @testedMethod {@link StelaSRPSquaring#computeShortPeriods(StelaEquinoctialOrbit, OrbitNatureConverter)}
     * 
     * @description
     *              Test method SRP Short Periods LEO.
     *              Test from STELA 3.4 (SRPAccSPTest.computeShortPeriodSrpLEO with UT1 - UTC = 0).
     *              Difference is due to different models from frames CIRF and MOD.
     * 
     * @testPassCriteria relative threshold is 1E-11. Difference is due to different models (between STELA and PATRIUS) from frames CIRF and MOD.
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testShortPeriodSRPLEO() throws PatriusException {

        Utils.clear();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader() {

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input,
                    final String name) throws IOException, ParseException, PatriusException {
                // nothing to do
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<>();
                for (int i = 1969; i < 2010; i++) {
                    // constant value:
                    map.put(new DateComponents(i, 11, 13), 36);
                }
                return map;
            }

            @Override
            public String getSupportedNames() {
                return "No name";
            }
        });
        final FramesConfigurationBuilder config = new FramesConfigurationBuilder(
                FramesConfigurationFactory.getStelaConfiguration());
        FramesFactory.setConfiguration(config.getConfiguration());

        // Bulletin (mean)
        final AbsoluteDate date = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TAI, 24472 * Constants.JULIAN_DAY + 36);

        final KeplerianOrbit meanOrbit = new KeplerianOrbit(7000000, 0.001, 55 * MathLib.PI / 180, 0, 0, 0,
                PositionAngle.MEAN, FramesFactory.getCIRF(), date, Constants.CNES_STELA_MU);

        // Sun
        final CelestialPoint sun = new MeeusSun(MODEL.STELA);

        // Force
        final StelaSRPSquaring srp = new StelaSRPSquaring(50., 10., 0.8, 33, sun, Constants.CNES_STELA_AE,
                Constants.CNES_STELA_UA, Constants.CONST_SOL_STELA, 10);

        // Conversion
        final double[] sp = srp.computeShortPeriods(new StelaEquinoctialOrbit(meanOrbit), null);

        // Check
        final double tol = 2E-11;
        Assert.assertEquals(0., (-0.10653429342900843 - sp[0]) / sp[0], tol);
        Assert.assertEquals(0., (1.0970332116899562E-7 - sp[1]) / sp[1], tol);
        Assert.assertEquals(0., (4.1878702893708E-8 - sp[2]) / sp[2], tol);
        Assert.assertEquals(0., (-2.7150230667316326E-9 - sp[3]) / sp[3], tol);
        Assert.assertEquals(0., (-7.041627603850544E-10 - sp[4]) / sp[4], tol);
        Assert.assertEquals(0., (1.6218596396438172E-8 - sp[5]) / sp[5], tol);
    }

    /**
     * @testedFeature {@link features#SRP_SHORT}
     * 
     * @testedMethod {@link StelaSRPSquaring#computeShortPeriods(StelaEquinoctialOrbit, OrbitNatureConverter)}
     * 
     * @description
     *              Test method SRP Short Periods LEO.
     *              Test from STELA 3.4 (SRPAccSPTest.computeShortPeriodSrpLEO with UT1 - UTC = 0).
     *              Difference is due to different models from frames CIRF and MOD.
     * 
     * @testPassCriteria relative threshold is 1E-11. Difference is due to different models (between STELA and PATRIUS) from frames CIRF and MOD.
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testShortPeriodSRPGEO() throws PatriusException {

        Utils.clear();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader() {

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input,
                    final String name) throws IOException, ParseException, PatriusException {
                // nothing to do
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<>();
                for (int i = 1969; i < 2010; i++) {
                    // constant value:
                    map.put(new DateComponents(i, 11, 13), 36);
                }
                return map;
            }

            @Override
            public String getSupportedNames() {
                return "No name";
            }
        });
        final FramesConfigurationBuilder config = new FramesConfigurationBuilder(
                FramesConfigurationFactory.getStelaConfiguration());
        FramesFactory.setConfiguration(config.getConfiguration());

        // Bulletin (mean)
        final AbsoluteDate date = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TAI, 24472 * Constants.JULIAN_DAY + 36);

        final KeplerianOrbit meanOrbit = new KeplerianOrbit(42164200, 0, 0, 0, 0, 0, PositionAngle.MEAN,
                FramesFactory.getCIRF(), date, Constants.CNES_STELA_MU);

        // Sun
        final CelestialPoint sun = new MeeusSun(MODEL.STELA);

        // Force
        final StelaSRPSquaring srp = new StelaSRPSquaring(50., 10., 0.8, 33, sun, Constants.CNES_STELA_AE,
                Constants.CNES_STELA_UA, Constants.CONST_SOL_STELA, 10);

        // Conversion
        final double[] sp = srp.computeShortPeriods(new StelaEquinoctialOrbit(meanOrbit), null);

        // Check
        final double tol = 5E-12;
        Assert.assertEquals(0., (-51.982747174847965 - sp[0]) / sp[0], tol);
        Assert.assertEquals(0., (6.072787636457518E-6 - sp[1]) / sp[1], tol);
        Assert.assertEquals(0., (-1.4864313477645554E-7 - sp[2]) / sp[2], tol);
        Assert.assertEquals(0., (-7.837347719179616E-7 - sp[3]) / sp[3], tol);
        Assert.assertEquals(0., (1.3054420913511904E-10 - sp[4]) / sp[4], tol);
        Assert.assertEquals(0., (-6.575579040308528E-7 - sp[5]) / sp[5], tol);
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
    }

    void assertEquals(final double[] exp,
            final double[] act,
            final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            Assert.assertEquals(exp[i], act[i], eps);
        }
    }

    void assertEquals(final double[][] exp,
            final double[][] act,
            final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            this.assertEquals(exp[i], act[i], eps);
        }
    }
}
