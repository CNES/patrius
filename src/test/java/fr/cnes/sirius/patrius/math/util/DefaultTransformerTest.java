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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * @version $Id: DefaultTransformerTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class DefaultTransformerTest {
    /**
     *
     */
    @Test
    public void testTransformDouble() throws Exception {
        final double expected = 1.0;
        final Double input = Double.valueOf(expected);
        final DefaultTransformer t = new DefaultTransformer();
        Assert.assertEquals(expected, t.transform(input), 1.0e-4);
    }

    /**
     *
     */
    @Test
    public void testTransformNull() throws Exception {
        final DefaultTransformer t = new DefaultTransformer();
        try {
            t.transform(null);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException e) {
            // expected
        }
    }

    /**
     *
     */
    @Test
    public void testTransformInteger() throws Exception {
        final double expected = 1.0;
        final Integer input = Integer.valueOf(1);
        final DefaultTransformer t = new DefaultTransformer();
        Assert.assertEquals(expected, t.transform(input), 1.0e-4);
    }

    /**
     *
     */
    @Test
    public void testTransformBigDecimal() throws Exception {
        final double expected = 1.0;
        final BigDecimal input = new BigDecimal("1.0");
        final DefaultTransformer t = new DefaultTransformer();
        Assert.assertEquals(expected, t.transform(input), 1.0e-4);
    }

    /**
     *
     */
    @Test
    public void testTransformString() throws Exception {
        final double expected = 1.0;
        final String input = "1.0";
        final DefaultTransformer t = new DefaultTransformer();
        Assert.assertEquals(expected, t.transform(input), 1.0e-4);
    }

    /**
     *
     */
    @Test(expected = MathIllegalArgumentException.class)
    public void testTransformObject() {
        final Boolean input = Boolean.TRUE;
        final DefaultTransformer t = new DefaultTransformer();
        t.transform(input);
    }

    @Test
    public void testSerial() {
        Assert.assertEquals(new DefaultTransformer(), TestUtils.serializeAndRecover(new DefaultTransformer()));
    }
}
