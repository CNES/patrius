/**
 *
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history creation 18/10/2012
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Test class for {@link PrecessionNutationConvention}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: IERSPNConventionTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class IERSPNConventionTest {

    /** File names. */
    private static final String TA = "/tab5.2a.txt";
    /** File names. */
    private static final String TB = "/tab5.2b.txt";
    /** File names. */
    private static final String TC = "/tab5.2c.txt";
    /** File names. */
    private static final String TD = "/tab5.2d.txt";
    /** Convetion data dir. */
    private final String s03 = "/META-INF/IERS-conventions-2003";
    /** Convetion data dir. */
    private final String s10 = "/META-INF/IERS-conventions-2010";

    /** Test return data location. */
    @Test
    public void testGetDataLocation() {

        Assert.assertEquals(this.s03 + TA, PrecessionNutationConvention.IERS2003.getDataLocation()[0]);
        Assert.assertEquals(this.s03 + TB, PrecessionNutationConvention.IERS2003.getDataLocation()[1]);
        Assert.assertEquals(this.s03 + TC, PrecessionNutationConvention.IERS2003.getDataLocation()[2]);

        Assert.assertEquals(this.s10 + TA, PrecessionNutationConvention.IERS2010.getDataLocation()[0]);
        Assert.assertEquals(this.s10 + TB, PrecessionNutationConvention.IERS2010.getDataLocation()[1]);
        Assert.assertEquals(this.s10 + TD, PrecessionNutationConvention.IERS2010.getDataLocation()[2]);
    }

}
