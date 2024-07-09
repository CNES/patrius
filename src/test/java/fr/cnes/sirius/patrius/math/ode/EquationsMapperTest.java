/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:20/11/2014: (creation) coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * Test class for EquationMapper.
 * 
 * @version $Id: EquationsMapperTest.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.4
 * 
 */
public class EquationsMapperTest {

    /**
     * For coverage purposes, tests the if (equationData.length != dimension)
     * of method extractEquationData.
     */
    @Test(expected = DimensionMismatchException.class)
    public void testExceptionExtractEquationData() {
        final EquationsMapper mapper = new EquationsMapper(0, 2);
        final double[] complete = new double[5];
        final double[] equationData = new double[3];
        mapper.extractEquationData(complete, equationData);
    }

    /**
     * For coverage purposes, tests the if (equationData.length != dimension)
     * of method insertEquationData.
     */
    @Test(expected = DimensionMismatchException.class)
    public void testExceptionInsertEquationData() {
        final EquationsMapper mapper = new EquationsMapper(0, 2);
        final double[] complete = new double[5];
        final double[] equationData = new double[3];
        mapper.insertEquationData(equationData, complete);
    }

}
