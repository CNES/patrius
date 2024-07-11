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
 * @history Created 09/12/2014
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:275:09/12/2014: (creation) public visibility to OceanTidesCoefficientsSet class
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests the class OceanTidesCoefficientsSet.
 * 
 * @author Sophie LAURENS
 * @since 2.3.1
 * @version $Id: OceanTidesCoefficientsSetTest.java 17911 2017-09-11 12:02:31Z bignon $
 */
public class OceanTidesCoefficientsSetTest {

    /**
     * Tests the construction of instance from class,
     * and its getters.
     */
    @Test
    public void testConstructor() {
        // creates a silly object with totally meaningless values
        final double doodson = 14.7;
        final int l = 23;
        final int m = 2;
        final double[] coeffs = { 0, 1, 2, 3, 4, 5, 6, 7 };

        final OceanTidesCoefficientsSet c = new OceanTidesCoefficientsSet(doodson, l, m, coeffs[0], coeffs[1],
            coeffs[2], coeffs[3], coeffs[4],
            coeffs[5], coeffs[6], coeffs[7]);

        // tests the getters for each attributes
        final double doodson2 = c.getDoodson();
        Assert.assertEquals(doodson, doodson2);
        final int l2 = c.getDegree();
        Assert.assertEquals(l, l2);
        final int m2 = c.getOrder();
        Assert.assertEquals(m, m2);
        final double csp = c.getCsp();
        Assert.assertEquals(coeffs[0], csp);
        final double ccp = c.getCcp();
        Assert.assertEquals(coeffs[1], ccp);
        final double csm = c.getCsm();
        Assert.assertEquals(coeffs[2], csm);
        final double ccm = c.getCcm();
        Assert.assertEquals(coeffs[3], ccm);
        final double cp = c.getCp();
        Assert.assertEquals(coeffs[4], cp);
        final double ep = c.getEp();
        Assert.assertEquals(coeffs[5], ep);
        final double cm = c.getCm();
        Assert.assertEquals(coeffs[6], cm);
        final double em = c.getEm();
        Assert.assertEquals(coeffs[7], em);

        // tests the call to methods computeCode (static) and code
        final double result = OceanTidesCoefficientsSet.computeCode(doodson, l, m);
        final double result2 = c.code();
        Assert.assertEquals(result, result2);
    }

}
