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
 * VERSION::FA:306:17/11/2014: (creation) coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Test class for JacobiPreconditioner
 * 
 * @since 2.4
 * @version $Id: JacobiPreconditionerTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class JacobiPreconditionerTest {

    /**
     * For coverage purpose, tests the if (a.getRowDimension() != n) in
     * method create.
     */
    @Test(expected = NonSquareOperatorException.class)
    public void testNSOExceptionCreate() {
        final double[][] d = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(d);
        JacobiPreconditioner.create(matrix);
    }

    /**
     * For coverage purpose, tests the method getRowDimension and
     * getColumnDimension of RealLinearOperator defined by method sqrt
     */
    @Test
    public void testGetDimension() {
        final double[] diag = { 1, 2, 3, 4 };
        final int dim = 4;
        final boolean deep = true;
        final JacobiPreconditioner jacobi = new JacobiPreconditioner(diag, deep);
        final RealLinearOperator op = jacobi.sqrt();
        final int ncolumn = op.getColumnDimension();
        final int nrow = op.getRowDimension();
        Assert.assertEquals(ncolumn, dim);
        Assert.assertEquals(nrow, dim);

    }
}
