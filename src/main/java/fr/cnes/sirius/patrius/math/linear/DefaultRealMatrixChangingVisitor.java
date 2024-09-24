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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

/**
 * Default implementation of the {@link RealMatrixChangingVisitor} interface.
 * <p>
 * This class is a convenience to create custom visitors without defining all methods. This class provides default
 * implementations that do nothing.
 * </p>
 * 
 * @version $Id: DefaultRealMatrixChangingVisitor.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class DefaultRealMatrixChangingVisitor implements RealMatrixChangingVisitor {
    /** {@inheritDoc} */
    @Override
    public void start(final int rows, final int columns,
                      final int startRow, final int endRow, final int startColumn, final int endColumn) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double visit(final int row, final int column, final double value) {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public double end() {
        return 0;
    }
}
