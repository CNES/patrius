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
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test Class computing Gauss equations
 * </p>
 * 
 * @see STELA
 * 
 * @author Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class AbstractStelaGaussContributionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Gauss Equations
         * 
         * @featureDescription Computation of Gauss Equations and its derivatives.
         * 
         * @coveredRequirements
         */
        GAUSS_EQ
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GAUSS_EQ}
     * 
     * @testedMethod {@link AbstractStelaGaussContribution#computeGaussEquations(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of Gauss Equations
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output Matrix of Gauss Equations
     * 
     * @testPassCriteria Matrix is very close to the reference one (from Satlight)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testGaussEquations() throws PatriusException {

        /** Epsilon */
        final double EPS = 1e-12;
        // Input Parameters
        final double aIn = 2.43505E+7;
        final double lMIn = 0.8922879297441284;
        final double exIn = 0.3644381018870251;
        final double eyIn = 0.6312253086822904;
        final double ixIn = 0.09052430460833645;
        final double iyIn = 0.05226423163382672;

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, lMIn, frame, date,
            mu);

        // expected results
        final double[][] expected = { { 25493.669384831075, -0.00000955819053293242, 0.00031675988649920326,
            0.00011918199939202834, 0., 0. },
            { 0., 0.00024061722253813046, -0.00014219841095626333,
                0.00012729861995641113, 0., 0. },
            { 0., -0.000008311319236007934, 0.000005246315050306167,
                -0.0000030289614065478513, 0.00006518969661360568, -0.000008269273288431915 } };

        final AbstractStelaGaussContribution gaussContrib = new AbstractStelaGaussContribution(){

            @Override
            public
                    double[]
                    computePerturbation(final StelaEquinoctialOrbit orbit, final OrbitNatureConverter converter)
                                                                                                                throws PatriusException {
                return null;
            }

            @Override
            public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) {
                return null;
            }

            @Override
            public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {
                return null;
            }
        };

        final double[][] eqGauss = gaussContrib.computeGaussEquations(orbit);

        for (int i = 0; i < eqGauss.length; i++) {
            for (int j = 0; j < eqGauss[0].length; j++) {

                if (MathLib.abs(expected[j][i]) < EPS) {

                    Assert.assertEquals(expected[j][i], eqGauss[i][j], EPS);

                } else {

                    Assert.assertEquals((expected[j][i] - eqGauss[i][j]) / expected[j][i], 0, EPS);
                }
            }
        }
        Assert.assertEquals("GAUSS", gaussContrib.getType());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GAUSS_EQ}
     * 
     * @testedMethod {@link AbstractStelaGaussContribution#computeGaussDerivativeEquations(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of Gauss derivatives Equations
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output Matrix of Gauss derivatives Equations
     * 
     * @testPassCriteria Matrix is very close to the reference one (used in Stela)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testGaussDerivativesEquations() throws PatriusException {

        /** Epsilon */
        final double EPS = 1e-12;

        // Input Parameters
        // Context
        /** Semi-major axis. */
        final double aIn = 2.500000000000000000E+07;
        /** LambdaEq. */
        final double lMIn = 5.632358385358075203E+00;
        /** Eccentricity. */
        final double exIn = 6.062177826491070842E-01;
        /** Eccentricity. */
        final double eyIn = -3.499999999999997558E-01;
        /** Inclination. */
        final double ixIn = 1.511422733185859013E-02;
        /** Inclination. */
        final double iyIn = 8.726203218641754092E-03;

        // Expected results
        /** Expected Gauss equations derivatives matrix, first term. */
        final double[][] expGaussEqDerivTerm1 = {
            { 1.616969824975754812E-03, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -2.194644219600097048E-13, 4.864872347502599350E-12, -4.046105840097571118E-14 },
            { 3.512832133164909290E-12, 2.446414578698905412E-12, -1.416137044034149071E-14 },
            { -6.232100822968744251E-12, 2.944414202101602972E-12, -2.452821310747551147E-14 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 1.850637979571455480E-13 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.231662359071516272E-12 } };
        /** Expected Gauss equations derivatives matrix, second term. */
        final double[][] expGaussEqDerivTerm2 = {
            { 3.537223917810415878E+04, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -7.290228384290645526E-05, -2.672194805216992716E-04, -1.359918205716792914E-07 },
            { 1.072766193621526318E-03, -2.835243275781824663E-04, -4.759713720008770434E-08 },
            { 6.046833424834671070E-04, 2.493438924078969772E-05, -8.244065992537863502E-08 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 3.286915091920468586E-04 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 1.852713297002971018E-04 } };
        /** Expected Gauss equations derivatives matrix, third term. */
        final double[][] expGaussEqDerivTerm3 = {
            { -3.719179516826740155E+02, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -3.742091583083718992E-05, -7.428992857883809603E-05, 2.771582703580852319E-06 },
            { -1.276082479929615361E-03, 4.290605541265150337E-05, 9.700539462532980575E-07 },
            { -2.312112663164283210E-04, -3.343448006868526313E-05, -3.428701990553846584E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -3.930877484077048675E-04 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.352614446432394012E-04 } };
        /** Expected Gauss equations derivatives matrix, forth term. */
        final double[][] expGaussEqDerivTerm4 = {
            { -5.813433538853173377E+04, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 1.418625564032291323E-04, 4.836891360113987955E-04, -2.687760710298461280E-06 },
            { -5.188437117816823742E-04, 2.000708351851130176E-04, 1.082336671444325886E-06 },
            { -5.742454322161816022E-04, 1.799492044616222692E-04, -1.629368338088522293E-06 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -2.136847539654056075E-04 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -2.122858729412978784E-04 } };
        /** Expected Gauss equations derivatives matrix, fifth term. */
        final double[][] expGaussEqDerivTerm5 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.232037621086628858E-04 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -4.312131673803198430E-05 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -7.468831147954170394E-05 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 3.977865568926481773E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.011757603398385895E-06 } };
        /** Expected Gauss equations derivatives matrix, sixth term. */
        final double[][] expGaussEqDerivTerm6 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.851201830640169784E-05 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -6.479206407240590176E-06 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.122231469006651478E-05 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 1.011603507815724170E-06 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 3.975196555142558786E-07 } };

        final double[][][] expGaussEqDeriv = new double[6][3][6];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                expGaussEqDeriv[i][j][0] = expGaussEqDerivTerm1[i][j];
                expGaussEqDeriv[i][j][1] = expGaussEqDerivTerm2[i][j];
                expGaussEqDeriv[i][j][2] = expGaussEqDerivTerm3[i][j];
                expGaussEqDeriv[i][j][3] = expGaussEqDerivTerm4[i][j];
                expGaussEqDeriv[i][j][4] = expGaussEqDerivTerm5[i][j];
                expGaussEqDeriv[i][j][5] = expGaussEqDerivTerm6[i][j];
            }
        }

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, lMIn, frame, date,
            mu);

        final AbstractStelaGaussContribution gaussContrib = new AbstractStelaGaussContribution(){

            @Override
            public
                    double[]
                    computePerturbation(final StelaEquinoctialOrbit orbit, final OrbitNatureConverter converter)
                                                                                                                throws PatriusException {
                return null;
            }

            @Override
            public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) {
                return null;
            }

            @Override
            public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {
                return null;
            }
        };
        final double[][][] eqDerGauss = gaussContrib.computeGaussDerivativeEquations(orbit);

        for (int i = 0; i < eqDerGauss.length; i++) {
            for (int j = 0; j < eqDerGauss[0].length; j++) {
                for (int j2 = 0; j2 < eqDerGauss[0][0].length; j2++) {

                    if (MathLib.abs(expGaussEqDeriv[i][j][j2]) < EPS) {

                        Assert.assertEquals(expGaussEqDeriv[i][j][j2], eqDerGauss[i][j][j2], EPS);

                    } else {

                        Assert.assertEquals((expGaussEqDeriv[i][j][j2] - eqDerGauss[i][j][j2])
                            / expGaussEqDeriv[i][j][j2], 0, EPS);
                    }
                }
            }
        }
    }
}
