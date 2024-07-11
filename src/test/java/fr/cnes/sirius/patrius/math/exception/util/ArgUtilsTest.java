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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.exception.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link ArgUtils}.
 * 
 * @version $Id: ArgUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ArgUtilsTest {
    @Test
    public void testFlatten() {
        final List<Object> orig = new ArrayList<Object>();

        final Object[] struct = new Object[] {
            new Object[] {
                new Object[] {
                    this.create(orig),
                    this.create(orig),
                },
                this.create(orig),
                new Object[] {
                    this.create(orig),
                }
            },
            this.create(orig),
            new Object[] {
                this.create(orig),
                new Object[] {
                    this.create(orig),
                    this.create(orig),
                }
            },
            this.create(orig),
        };

        final Object[] flat = ArgUtils.flatten(struct);
        Assert.assertEquals(flat.length, orig.size());

        for (int i = 0, max = orig.size(); i < max; i++) {
            Assert.assertEquals(orig.get(i), flat[i]);
        }
    }

    /**
     * Create and store an {@code Object}.
     * 
     * @param list
     *        List to store to.
     * @return the stored object.
     */
    private Object create(final List<Object> list) {
        final Object o = new Object();
        list.add(o);
        return o;
    }
}
