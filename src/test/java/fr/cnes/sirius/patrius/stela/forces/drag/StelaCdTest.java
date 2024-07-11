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
 * @history Created 21/08/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:21/08/2013:creation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.drag;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link StelaCd}
 * 
 * @author cedric Dental
 * 
 * @version
 * 
 * @since 2.1
 */
public class StelaCdTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela spacecraft factory test
         * 
         * @featureDescription tests StelaCd mechanism
         */
        STELA_CD
    }

    /**
     * 
     * Test the Cd container {@link StelaCd}
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_CD}
     * 
     * @testedMethod {@link StelaCd#StelaCd(double)}
     * 
     * @description tests the instance to make sure the correct Cd
     * 
     * @input a constant value for Cd
     * 
     * @output the Cd value stored
     * 
     * @testPassCriteria the value returned is exactly equal to the expected one (0. ulp difference)
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testConstant() throws PatriusException {

        final double inCd = 2.5;
        final StelaCd cd = new StelaCd(inCd);

        Assert.assertEquals(inCd, cd.getCd(null), 0);

    }

    /**
     * 
     * Test the Cd container {@link StelaCd}
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_CD}
     * 
     * @testedMethod {@link StelaCd#StelaCd(Map, double, double)}
     * 
     * @description tests the instance to make sure the correct Cd is returned at a given altitude
     * 
     * @input a list of Cd with corresponding attitudes
     * 
     * @output a Cd value for a given altitude
     * 
     * @testPassCriteria the values returned are exactly equal to the expected ones (0. ulp difference)
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testVariable() throws PatriusException {

        final Map<Double, Double> inCd = new TreeMap();

        // earth - stela values
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;

        inCd.put(0., 0.5);
        inCd.put(100., 1.5);
        inCd.put(1000., 2.5);
        inCd.put(10000., 3.5);

        final StelaCd cd = new StelaCd(inCd, ae, 1 / f);

        final Vector3D u = new Vector3D(0, 0, 1);

        Assert.assertEquals(inCd.get(0.), cd.getCd(new Vector3D(1 + ae, u)), 0);
        Assert.assertEquals(inCd.get(100.), cd.getCd(new Vector3D(100000 + ae, u)), 0);
        Assert.assertEquals(inCd.get(1000.), cd.getCd(new Vector3D(1000000 + ae, u)), 0);
        Assert.assertEquals(inCd.get(10000.), cd.getCd(new Vector3D(10000000 + ae, u)), 0);

    }
}
