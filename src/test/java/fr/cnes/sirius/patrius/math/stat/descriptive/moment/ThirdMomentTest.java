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

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;

/**
 * Test cases for the {@link ThirdMoment} class.
 * 
 * @version $Id: ThirdMomentTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ThirdMomentTest extends StorelessUnivariateStatisticAbstractTest {

    /** descriptive statistic. */
    protected ThirdMoment stat;

    /**
     * @see fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatisticAbstractTest#getUnivariateStatistic()
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new ThirdMoment();
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatisticAbstractTest#expectedValue()
     */
    @Override
    public double expectedValue() {
        return this.thirdMoment;
    }

}
