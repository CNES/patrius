/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.12:DM:DM-102:17/08/2023:[PATRIUS] Ajout méthode getNature à l'enum PolynomialType
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import org.junit.Assert;
import org.junit.Test;


/**
 * Unit test class for the {@link PolynomialType} class.
 *
 * @author Thibaut BONIT
 *
 * @version $Id$
 *
 * @since 4.12
 */
public final class PolynomialTypeTest {

    /**
     * Evaluate by non regression the enumerates attributes.
     * 
     * @testedMethod {@link PolynomialType#getNature()}
     */
    @Test
    public void testAttributes() {
        // Non regression on the expected nature values
        Assert.assertEquals("CLASSICAL_POLYNOMIAL_FUNCTION", PolynomialType.CLASSICAL.getNature());
        Assert.assertEquals("CHEBYSHEV_POLYNOMIAL_FUNCTION", PolynomialType.CHEBYSHEV.getNature());
    }
}
