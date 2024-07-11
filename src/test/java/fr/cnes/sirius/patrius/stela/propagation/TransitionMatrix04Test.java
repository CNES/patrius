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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: new test class
 * VERSION::DM:316:26/02/2015:take into account Sun-Satellite direction in PRS computation
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
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
public class TransitionMatrix04Test {

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
    final boolean drag = false;

    /** Delta-vector used to compute transition matrix with finite differences. */
    private final double[] delta = { 1.0, 1E-5, 1E-5, 1E-5, 1E-5, 1E-5, 1E-5, 1E-5 };

    /** Tolerance for each cell of transition matrix. */
    private final double tolerance = 1e-2;

    /** Expected transition matrix. */
    private final double[][] expected = {
        { 1.0000000000000E+00, 0.0000000000000E+00, 0.0000000000000E+00, 0.0000000000000E+00, 0.0000000000000E+00,
            0.0000000000000E+00, 0.0000000000000E+00, 0.0000000000000E+00 },
        { -3.0279615855980E-04, 1.0000000000000E+00, 2.0277321866642E+01, -1.1527965313076E+01,
            -7.5892425142280E-01, -6.5997800616331E-01, 0.0000000000000E+00, -2.0906706347615E-04 },
        { 2.3201386261020E-07, 0.0000000000000E+00, -9.8430892147569E+00, 4.5347755752356E+00, 3.3664336131141E-01,
            2.5297835655752E-01, 0.0000000000000E+00, 7.2771300588333E-05 },
        { 9.0740839923098E-08, 0.0000000000000E+00, -2.9099107720102E+00, 1.2394705510758E+00, 1.3172090175917E-01,
            1.0070737799655E-01, 0.0000000000000E+00, -5.2417125102108E-06 },
        { 5.9043237755837E-09, 0.0000000000000E+00, -2.0726838480758E-01, 1.1694435595158E-01,
            -7.9187661237756E-01, 6.0069747662151E-01, 0.0000000000000E+00, 8.7523628234635E-06 },
        { -1.0790629458259E-09, 0.0000000000000E+00, 5.7250641798228E-02, -3.3175258713514E-02,
            -6.0458430638133E-01, -8.0899319100638E-01, 0.0000000000000E+00, -6.4488424935085E-06 } };

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
        new GenericTransitionMatrixTest("Transition_matrix_test_04", this.delta, this.jjfin, this.jjdeb, this.a,
            this.eccentricity, this.i, this.pom, this.gom,
            this.m, this.mu, this.tolerance, this.dragArea, this.refArea, this.isOsculating, this.refCoef, this.mass,
            this.cx, this.dt, this.reentryAltitude, this.drag,
            this.expected).run();
    }
}
