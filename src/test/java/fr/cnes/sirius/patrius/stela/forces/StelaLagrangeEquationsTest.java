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
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test Class computing Lagrange equations
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
public class StelaLagrangeEquationsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Lagrange Equations
         * 
         * @featureDescription Computation of Lagrange Equations and its derivatives.
         * 
         * @coveredRequirements
         */
        LAGRANGE_EQ
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#LAGRANGE_EQ}
     * 
     * @testedMethod {@link StelaLagrangeEquations#computeLagrangeEquations(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of Lagrange Equations
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output Matrix of Lagrange Equations
     * 
     * @testPassCriteria Matrix is very close to a reference one (SatLight)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testLagrangeEquations() throws PatriusException {

        /** Epsilon */
        final double EPS = 1e-12;

        // Context
        /** Semi-major axis (Type 8 param). */
        final double aIn = 2.43505E+7;
        /** Eccentricity (Type 8 param). */
        final double exIn = 0.3644381018870251;
        /** Eccentricity (Type 8 param). */
        final double eyIn = 0.6312253086822904;
        /** Inclination (Type 8 param). */
        final double ixIn = 0.09052430460833645;
        /** Inclination (Type 8 param). */
        final double iyIn = 0.05226423163382672;
        /** LambdaEq. */
        final double lMIn = 0.8922879297441284;

        // expected results

        final double[][] expected = {
            { 0., 0.0004943278166200146, 0., 0., 0., 0. },
            { -0.0004943278166200146, 0., 1.5033433599602868E-12, 2.6038670806725233E-12, 6.710372139100649E-13,
                3.874235160872324E-13 },
            { 0., -1.5033433599602868E-12, 0., -6.949330034237786E-12, -4.2357567248768484E-13,
                -2.445515285329416E-13 },
            { 0., -2.6038670806725233E-12, 6.949330034237786E-12, 0., 2.445515285329417E-13, 1.411918908292283E-13 },
            { 0., -6.710372139100649E-13, 4.2357567248768484E-13, -2.445515285329417E-13, 0.,
                -3.706392536310456E-12 },
            { 0., -3.874235160872324E-13, 2.445515285329416E-13, -1.411918908292283E-13, 3.706392536310456E-12, 0. } };

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, lMIn, frame, date,
            mu);

        final StelaLagrangeEquations lagObj = new StelaLagrangeEquations();

        final double[][] eqLagrange = lagObj.computeLagrangeEquations(orbit);
        for (int i = 0; i < eqLagrange.length; i++) {
            for (int j = 0; j < eqLagrange[0].length; j++) {

                if (MathLib.abs(expected[i][j]) < EPS) {

                    Assert.assertEquals(expected[i][j], eqLagrange[j][i], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expected[i][j] - eqLagrange[j][i])) / expected[i][j], 0, EPS);

                }

            }

        }

    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#LAGRANGE_EQ}
     * 
     * @testedMethod {@link StelaLagrangeEquations#computeLagrangeDerivativeEquations(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of Lagrange derivatives Equations
     * 
     * @input SpacecraftState
     * 
     * @output Matrix of Lagrange derivatives Equations
     * 
     * @testPassCriteria Matrix is very close to the reference one (used in Stela)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testLagrangeDerivativesEquations() throws PatriusException {

        /** Epsilon */
        final double EPS = 1e-12;

        // Context
        /** Semi-major axis (Type 8 param). */
        final double aIn = 2.4228E+7;
        /** Eccentricity (Type 8 param). */
        final double exIn = 0.4592777886100000151;
        /** Eccentricity (Type 8 param). */
        final double eyIn = -0.566545395240000027;
        /** Inclination (Type 8 param). */
        final double ixIn = 0.05490687833999999962;
        /** Inclination (Type 8 param). */
        final double iyIn = -0.06770144794999999327;
        /** LambdaEq. */
        final double lMIn = 0.0;

        // Expected results
        /** Expected Poisson brackets derivatives, first term. */
        final double[][] expLagrangeDerEqTerm1 = {
            { 0.000000000000000000E+00, 1.017588827002183602E-11, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { -1.017588827002183602E-11, 0.000000000000000000E+00, -3.918129296361489680E-20,
                4.833236367747591456E-20,
                -8.426684602766535548E-21, 1.039029655797519620E-20 },
            { 0.000000000000000000E+00, 3.918129296361489680E-20, 0.000000000000000000E+00,
                1.436777626566953817E-19,
                -4.774099358837185516E-21, 5.886574670098869009E-21 },
            { 0.000000000000000000E+00, -4.833236367747591456E-20, -1.436777626566953817E-19,
                0.000000000000000000E+00,
                -3.870189069672548665E-21, 4.772032426148942193E-21 },
            { 0.000000000000000000E+00, 8.426684602766535548E-21, 4.774099358837185516E-21,
                3.870189069672548665E-21, 0.000000000000000000E+00,
                7.673614725085940730E-20 },
            { 0.000000000000000000E+00, -1.039029655797519620E-20, -5.886574670098869009E-21,
                -4.772032426148942193E-21,
                -7.673614725085940730E-20, 0.000000000000000000E+00 } };
        /** Expected Poisson brackets derivatives, second term. */
        final double[][] expLagrangeDerEqTerm2 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 } };
        /** Expected Poisson brackets derivatives, third term. */
        final double[][] expLagrangeDerEqTerm3 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 3.027735005546443637E-12,
                1.364411048165448949E-12, 4.006361815168997041E-13,
                -4.939936563483957074E-13 },
            { 0.000000000000000000E+00, -3.027735005546443637E-12, 0.000000000000000000E+00,
                6.830979550459469530E-12,
                2.269785838049363426E-13, -2.798698312819546489E-13 },
            { 0.000000000000000000E+00, -1.364411048165448949E-12, -6.830979550459469530E-12,
                0.000000000000000000E+00,
                5.923267285958912347E-13, -7.303525240883052543E-13 },
            { 0.000000000000000000E+00, -4.006361815168997041E-13, -2.269785838049363426E-13,
                -5.923267285958912347E-13,
                0.000000000000000000E+00, -3.648324159279637976E-12 },
            { 0.000000000000000000E+00, 4.939936563483957074E-13, 2.798698312819546489E-13,
                7.303525240883052543E-13, 3.648324159279637976E-12,
                0.000000000000000000E+00 } };
        /** Expected Poisson brackets derivatives, forth term. */
        final double[][] expLagrangeDerEqTerm4 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 1.364411048165448949E-12,
                2.450734529746642039E-12,
                -4.942076221275252678E-13, 6.093694017491637918E-13 },
            { 0.000000000000000000E+00, -1.364411048165448949E-12, 0.000000000000000000E+00,
                -8.426403595532278139E-12,
                -6.883144817205142914E-13, 8.487076385743882388E-13 },
            { 0.000000000000000000E+00, -2.450734529746642039E-12, 8.426403595532278139E-12,
                0.000000000000000000E+00,
                -2.269785838049363426E-13, 2.798698312819546489E-13 },
            { 0.000000000000000000E+00, 4.942076221275252678E-13, 6.883144817205142914E-13,
                2.269785838049363426E-13, 0.000000000000000000E+00,
                4.500416314575764323E-12 },
            { 0.000000000000000000E+00, -6.093694017491637918E-13, -8.487076385743882388E-13,
                -2.798698312819546489E-13,
                -4.500416314575764323E-12, 0.000000000000000000E+00 } };
        /** Expected Poisson brackets derivatives, fifth term. */
        final double[][] expLagrangeDerEqTerm5 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 7.436653502375290552E-12,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 4.213201797766139070E-12,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 3.415489775229734765E-12,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, -7.436653502375290552E-12, -4.213201797766139070E-12,
                -3.415489775229734765E-12,
                0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 } };
        /** Expected Poisson brackets derivatives, sixth term. */
        final double[][] expLagrangeDerEqTerm6 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                7.436653502375290552E-12 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                4.213201797766139070E-12 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                3.415489775229734765E-12 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { 0.000000000000000000E+00, -7.436653502375290552E-12, -4.213201797766139070E-12,
                -3.415489775229734765E-12,
                0.000000000000000000E+00, 0.000000000000000000E+00 } };
        /** Expected Poisson brackets derivatives (6x6x6 matrix). Updated in create3DPoissBrackMatrix. */
        final double[][][] expLagrangeDerEq = new double[6][6][6];

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                // Results and references data are inverted.
                // Difference between reference data formalism and computePoissBrackDeriv() formalism.
                expLagrangeDerEq[0][j][i] = expLagrangeDerEqTerm1[i][j];
                expLagrangeDerEq[1][j][i] = expLagrangeDerEqTerm2[i][j];
                expLagrangeDerEq[2][j][i] = expLagrangeDerEqTerm3[i][j];
                expLagrangeDerEq[3][j][i] = expLagrangeDerEqTerm4[i][j];
                expLagrangeDerEq[4][j][i] = expLagrangeDerEqTerm5[i][j];
                expLagrangeDerEq[5][j][i] = expLagrangeDerEqTerm6[i][j];
            }
        }

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, lMIn, frame, date,
            mu);

        final StelaLagrangeEquations lagObj = new StelaLagrangeEquations();

        final double[][][] eqDerLagrange = lagObj.computeLagrangeDerivativeEquations(orbit);
        for (int i = 0; i < eqDerLagrange.length; i++) {
            for (int j = 0; j < eqDerLagrange[0].length; j++) {
                for (int j2 = 0; j2 < eqDerLagrange[0][0].length; j2++) {

                    if (MathLib.abs(expLagrangeDerEq[i][j][j2]) < EPS) {

                        Assert.assertEquals(expLagrangeDerEq[i][j][j2], eqDerLagrange[i][j][j2], EPS);

                    } else {

                        Assert.assertEquals((expLagrangeDerEq[i][j][j2] - eqDerLagrange[i][j][j2])
                            / expLagrangeDerEq[i][j][j2], 0, EPS);

                    }

                }

            }

        }
    }

}
