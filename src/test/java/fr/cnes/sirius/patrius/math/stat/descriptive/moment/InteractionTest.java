/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import org.junit.Assert;
import org.junit.Test;

/**
 * @version $Id: InteractionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class InteractionTest {

    protected double mean = 12.40454545454550;
    protected double var = 10.00235930735930;
    protected double skew = 1.437423729196190;
    protected double kurt = 2.377191264804700;

    protected double tolerance = 10E-12;

    protected double[] testArray =
    {
        12.5,
        12,
        11.8,
        14.2,
        14.9,
        14.5,
        21,
        8.2,
        10.3,
        11.3,
        14.1,
        9.9,
        12.2,
        12,
        12.1,
        11,
        19.8,
        11,
        10,
        8.8,
        9,
        12.3 };

    @Test
    public void testInteraction() {

        final FourthMoment m4 = new FourthMoment();
        final Mean m = new Mean(m4);
        final Variance v = new Variance(m4);
        final Skewness s = new Skewness(m4);
        final Kurtosis k = new Kurtosis(m4);

        for (final double element : this.testArray) {
            m4.increment(element);
        }

        Assert.assertEquals(this.mean, m.getResult(), this.tolerance);
        Assert.assertEquals(this.var, v.getResult(), this.tolerance);
        Assert.assertEquals(this.skew, s.getResult(), this.tolerance);
        Assert.assertEquals(this.kurt, k.getResult(), this.tolerance);

    }

}
