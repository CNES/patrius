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
 * VERSION::DM:316:26/02/2015:take into account Sun-Satellite direction in PRS computation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;

import fr.cnes.sirius.patrius.stela.propagation.GenericTransitionMatrixTest.ComparisonTypes;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Transition matrix test, taken from STELA
 * 
 * @author Rami Houdroge
 * @version $Id$
 * @since 2.1
 */
public class TransitionMatrix05Test {

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
    final int jjdeb = ValidationValues.LBN_TRANSITION_5_IN_jjdeb;
    /** In parameters */
    final int jjfin = ValidationValues.LBN_TRANSITION_5_IN_jjfin;
    /** In parameters */
    final double a = ValidationValues.LBN_TRANSITION_5_IN_a;
    /** In parameters */
    final double eccentricity = ValidationValues.LBN_TRANSITION_5_IN_eccentricity;
    /** In parameters */
    final double i = ValidationValues.LBN_TRANSITION_5_IN_i;
    /** In parameters */
    final double gom = ValidationValues.LBN_TRANSITION_5_IN_gom;
    /** In parameters */
    final double pom = ValidationValues.LBN_TRANSITION_5_IN_pom;
    /** In parameters */
    final double m = ValidationValues.LBN_TRANSITION_5_IN_M;
    /** In parameters */
    final double cx = ValidationValues.LBN_TRANSITION_5_IN_Cx;
    /** In parameters */
    final double refCoef = ValidationValues.LBN_TRANSITION_5_IN_refCoef;
    /** In parameters */
    final double mass = ValidationValues.LBN_TRANSITION_5_IN_mass;
    /** In parameters */
    final double dragArea = ValidationValues.LBN_TRANSITION_5_IN_dragArea;
    /** In parameters */
    final double refArea = ValidationValues.LBN_TRANSITION_5_IN_refArea;
    /** In parameters */
    final double reentryAltitude = 80000;
    /** In parameters */
    final boolean isOsculating = false;

    /** Force model flags */
    final boolean drag = false;
    /** Force model flags */
    final boolean tess = false;

    /** Delta-vector used to compute transition matrix with finite differences. */
    private final double[] delta = { 1.0, 1E-5, 1E-5, 1E-5, 1E-5, 1E-5, 1E-2, 1E-2 };

    /**
     * Tolerance for each cell of transition matrix.
     * Models are not the same, tolerance are therefore high on some components.
     * // da/dE
     * // dksi/dE
     * // dex/dE
     * // dey/dE
     * // dix/dE
     * // diy/dE
     * */
    // private final double[][] tolerance_old = {
    // { 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4 },
    // { 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4 },
    // { 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4 },
    // { 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4 },
    // { 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4 },
    // { 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4, 1E-4 }
    // };

    private final double[][] tolerance = {
        { 1E-7, 1E1, 1E1, 1E1, 1E1, 1E1, 1E-12, 1E1 },
        { 1E-8, 1E-6, 1E-0, 1E-2, 1E-5, 1E-6, 1E-12, 1E-0 },
        { 1E-2, 1E1, 1E-6, 1E-7, 1E-6, 1E-6, 1E-12, 1E-3 },
        { 1E-2, 1E1, 1E-6, 1E-6, 1E-6, 1E-6, 1E-12, 1E-3 },
        { 1E-5, 1E1, 1E-3, 1E-5, 1E-7, 1E-7, 1E-12, 1E-0 },
        { 1E-5, 1E1, 1E-3, 1E-5, 1E-7, 1E-7, 1E-12, 1E-0 },
    };

    /** Comparison type for each cell of transition matrix. */
    private final ComparisonTypes[][] comparisonType = {
        { ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE },
        { ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE },
        { ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE },
        { ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE },
        { ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE },
        { ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE, ComparisonTypes.RELATIVE,
            ComparisonTypes.RELATIVE },
    };

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
        new GenericTransitionMatrixTest("Transition Matrix test 05", this.delta, this.jjfin, this.jjdeb, this.a,
            this.eccentricity,
            this.i, this.pom, this.gom, this.m, this.mu, this.tolerance,
            this.dragArea, this.refArea, this.isOsculating, this.refCoef, this.mass, this.cx, this.dt,
            this.reentryAltitude, this.drag, this.tess, this.comparisonType)
            .run();
    }
}
