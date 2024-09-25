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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim;

import fr.cnes.sirius.patrius.math.util.Pair;

/**
 * This class holds a point and the value of an objective function at that point.
 *
 * @see PointVectorValuePair
 * @see fr.cnes.sirius.patrius.math.analysis.MultivariateFunction
 * @version $Id: PointValuePair.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class PointValuePair extends Pair<double[], Double> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20120514L;

    /**
     * Builds a point/objective function value pair.
     *
     * @param point
     *        Point coordinates. This instance will store a copy of the array, not the array passed as argument.
     * @param value
     *        Value of the objective function at the point
     */
    public PointValuePair(final double[] point, final double value) {
        this(point, value, true);
    }

    /**
     * Builds a point/objective function value pair.
     *
     * @param point
     *        Point coordinates
     * @param value
     *        Value of the objective function at the point
     * @param copyArray
     *        if {@code true}, the input array will be copied, otherwise it will be referenced
     */
    public PointValuePair(final double[] point, final double value, final boolean copyArray) {
        super(copyArray ? ((point == null) ? null : point.clone()) : point, value);
    }

    /**
     * Getter for the point.
     *
     * @return a copy of the stored point
     */
    public double[] getPoint() {
        final double[] p = getKey();
        return p == null ? null : p.clone();
    }

    /**
     * Getter for the reference to the point.
     *
     * @return the reference to the internal array storing the point
     */
    public double[] getPointRef() {
        return getKey();
    }
}
