/**
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
 */
/* Copyright 2002-2015 CS Systèmes d'Information
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

public class AngularDerivativesFilterTest {

    @Test
    public void testList() {
        Assert.assertEquals(3, AngularDerivativesFilter.values().length);
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(0, AngularDerivativesFilter.USE_R.getMaxOrder(), 0);
        Assert.assertEquals(1, AngularDerivativesFilter.USE_RR.getMaxOrder(), 0);
        Assert.assertEquals(2, AngularDerivativesFilter.USE_RRA.getMaxOrder(), 0);
    }

    @Test
    public void testBuildFromOrder() {
        Assert.assertEquals(AngularDerivativesFilter.USE_R, AngularDerivativesFilter.getFilter(0));
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, AngularDerivativesFilter.getFilter(1));
        Assert.assertEquals(AngularDerivativesFilter.USE_RRA, AngularDerivativesFilter.getFilter(2));
    }

    @Test
    public void testNoNegativeOrder() {
        try {
            AngularDerivativesFilter.getFilter(-1);
            Assert.fail("an exception should have been thrown");
        } catch (final IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testNoOrder3() {
        try {
            AngularDerivativesFilter.getFilter(3);
            Assert.fail("an exception should have been thrown");
        } catch (final IllegalArgumentException iae) {
            // expected
        }
    }

}
