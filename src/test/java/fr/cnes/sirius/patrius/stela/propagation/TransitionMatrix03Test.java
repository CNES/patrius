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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: new test class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;

import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Transition matrix test, taken from STELA
 * 
 * @author Rami Houdroge
 * @version $Id$
 * @since 2.1
 */
public class TransitionMatrix03Test {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela GTO partial derivatives computation
         * 
         * @featureDescription test the Stela GTO partial derivatives computation
         * 
         * @coveredRequirements
         */
        STELA_GTO_PARTIAL_DERIVATIVES
    }

    /** STELA Value */
    final double mu = 398600441449820.0;
    /** STELA Value */
    final double dt = Constants.JULIAN_DAY;

    /** In parameters */
    final int jjdeb = ValidationValues.LBN_TRANSITION_1_IN_jjdeb;
    /** In parameters */
    final int jjfin = ValidationValues.LBN_TRANSITION_1_IN_jjfin;
    /** In parameters */
    final double a = ValidationValues.LBN_TRANSITION_1_IN_a;
    /** In parameters */
    final double eccentricity = ValidationValues.LBN_TRANSITION_1_IN_eccentricity;
    /** In parameters */
    final double i = ValidationValues.LBN_TRANSITION_1_IN_i;
    /** In parameters */
    final double gom = ValidationValues.LBN_TRANSITION_1_IN_gom;
    /** In parameters */
    final double pom = ValidationValues.LBN_TRANSITION_1_IN_pom;
    /** In parameters */
    final double m = ValidationValues.LBN_TRANSITION_1_IN_M;
    /** In parameters */
    final double cx = ValidationValues.LBN_TRANSITION_1_IN_Cx;
    /** In parameters */
    final double refCoef = ValidationValues.LBN_TRANSITION_1_IN_refCoef;
    /** In parameters */
    final double mass = ValidationValues.LBN_TRANSITION_1_IN_mass;
    /** In parameters */
    final double dragArea = ValidationValues.LBN_TRANSITION_1_IN_dragArea;
    /** In parameters */
    final double refArea = ValidationValues.LBN_TRANSITION_1_IN_refArea;
    /** In parameters */
    final double reentryAltitude = 80000;
    /** In parameters */
    final boolean isOsculating = false;

    /** Force model flags */
    final boolean drag = true;

    /** Tolerance for the test. */
    private final double tolerance = 3E-2;

    /** Expected transition matrix. */

    private final double[][] expected = {
        { 1.0282967729688E+00, 0.0000000000000E+00, -2.3901634290203E+06, 1.3832279223338E+06,
            -7.6082450728637E+03, -7.6773830872808E+03, -6.3825795615177E+03, 1.6789411610710E+01 },
        { -3.0789125647807E-04, 1.0000000000000E+00, 4.3021288932892E+02, -2.4862301149905E+02,
            6.2794762206213E-01, 9.5709129689293E-01, 1.1144105898753E+00, 8.2500728764144E-04 },
        { 2.3386877469758E-07, 0.0000000000000E+00, -9.9950683412535E+00, 4.6230555999662E+00, 3.3606863083868E-01,
            2.5233974545565E-01, -4.1537765398268E-04, 7.2284908141347E-05 },
        { 9.1974564938389E-08, 0.0000000000000E+00, -3.0041970335757E+00, 1.2932906599671E+00, 1.3165828276762E-01,
            1.0056361021696E-01, -2.3697558291276E-04, -5.1992673048438E-06 },
        { 5.9603359382025E-09, 0.0000000000000E+00, -2.1182158995815E-01, 1.1957599295560E-01,
            -7.9227744994969E-01, 6.0017087051198E-01, -1.2342811104918E-05, 8.7390928359160E-06 },
        { -1.0844429699522E-09, 0.0000000000000E+00, 5.7517942364637E-02, -3.3331295356551E-02,
            -6.0406880944192E-01, -8.0936787768566E-01, 4.5917531133311E-07, -6.4450554661049E-06 } };

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PARTIAL_DERIVATIVES}
     * 
     * @testedMethod {@link StelaPartialDerivativesEquations#StelaPartialDerivativesEquations(java.util.List, java.util.List, int)}
     * @testedMethod {@link StelaGTOPropagator#addAdditionalEquations(StelaAdditionalEquations)}
     * 
     * @description test the computation of the partial derivatives
     * 
     * @input a Stela GTO propagator
     * 
     * @output the computed transition matrix
     * 
     * @testPassCriteria the output derivatives are the expected one
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     * @throws IOException
     *         if an input output error occurs
     * @throws ParseException
     *         if a parsing error occurs
     */
    @Test
    public void test() throws PatriusException, IOException, ParseException {
        new GenericTransitionMatrixTest("Transition_matrix_test_03", null, this.jjfin, this.jjdeb, this.a,
            this.eccentricity, this.i, this.pom, this.gom,
            this.m, this.mu, this.tolerance, this.dragArea, this.refArea, this.isOsculating, this.refCoef, this.mass,
            this.cx, this.dt, this.reentryAltitude, this.drag,
            this.expected).run();
    }
}
