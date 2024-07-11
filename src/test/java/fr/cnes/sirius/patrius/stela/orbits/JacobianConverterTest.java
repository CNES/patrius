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
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.orbits;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit tests for {@link JacobianConverter}
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class JacobianConverterTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle STELA equinoctial to cartesian Jacobian
         * 
         * @featureDescription STELA equinoctial to cartesian Jacobian computation validation
         * 
         * @coveredRequirements
         */
        STELA_EQUINOCTIAL_TO_CARTESIAN_JACOBIAN,

        /**
         * @featureTitle STELA Jacobian converter
         * 
         * @featureDescription STELA Jacobian computation validation for test coverage purpose
         * 
         * @coveredRequirements
         */
        STELA_JACOBIAN_CONVERTER
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(JacobianConverterTest.class.getSimpleName(), "STELA Jacobian converter");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_TO_CARTESIAN_JACOBIAN}
     * 
     * @testedMethod {@link JacobianConverter#computeEquinoctialToCartesianJacobian(StelaEquinoctialOrbit)}
     * 
     * @description test the Jacobian (equinoctial to cartesian) matrix computation.
     * 
     * @input a StelaEquinoctialOrbit
     * 
     * @output a Jacobian
     * 
     * @testPassCriteria the jacobian is equal to a Stela reference result ("MyStela (Scilab), 30/11/2012")
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     */
    @Test
    public void testJacobianEqToCart() {

        Report.printMethodHeader("testJacobianEqToCart", "Equinoctial to cartesian conversion", "STELA", 1E-13,
            ComparisonType.RELATIVE);

        final double mu = 398600441449820.000;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(2.500000000000000000E+07,
            6.321985447626403687E-01, -3.649999999999997691E-01, 1.511422733185859013E-02,
            8.726203218641754092E-03, 5.613949185772245443E+00, FramesFactory.getEME2000(), new AbsoluteDate(), mu);

        final double[][] actJacobian = JacobianConverter.computeEquinoctialToCartesianJacobian(orbit);

        /** Expected Jacobian matrix. */
        final double[][] expectedJac = {
            { -2.738348436194636484E-02, 4.982027589277029037E+07, -6.160488012984003127E+07,
                -2.923809991283258051E+07, -1.544841376380647998E+05, -2.437568627085372864E+05 },
            { -3.539149716881464514E-01, 2.031777031201953813E+07, -2.429831707100416068E+06,
                -3.181056230099475756E+07, 5.232399943831562414E+05, -2.062657100470823934E+04 },
            { -1.022506719673767911E-02, -2.554256185561373131E+05, 1.002161274246427231E+06,
                -4.515152515649317065E+05, -1.769691585878017172E+07, 1.366735715508053312E+06 },
            { -1.591457328913081293E-04, 2.441530734334201952E+03, 7.663454770716509302E+03,
                -1.341450877049729206E+04, 5.662493274403552590E+01, -1.796827687976954735E+02 },
            { -6.490302169307821329E-05, 3.155530791101306022E+04, -2.993593762311186947E+04,
                -2.004993465058466973E+04, -5.727450633291243776E+01, 2.405472415171835223E+02 },
            { 8.159307939567458353E-07, 9.116741862171501225E+02, -1.039137681843054452E+03,
                -3.721331230413869662E+02, 6.488697244543346642E+03, -1.591250531296543886E+04 } };

        // Test
        final double tol = 1.E-13;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(0., (expectedJac[i][j] - actJacobian[i][j]) / expectedJac[i][j], tol);
            }
        }

        Report.printToReport("Jacobian matrix", expectedJac, actJacobian);
    }
}
