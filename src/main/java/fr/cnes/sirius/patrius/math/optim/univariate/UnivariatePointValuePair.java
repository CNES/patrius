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
package fr.cnes.sirius.patrius.math.optim.univariate;

import java.io.Serializable;

/**
 * This class holds a point and the value of an objective function at this
 * point.
 * This is a simple immutable container.
 * 
 * @version $Id: UnivariatePointValuePair.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class UnivariatePointValuePair implements Serializable {
     /** Serializable UID. */
    private static final long serialVersionUID = 1003888396256744753L;
    /** Point. */
    private final double point;
    /** Value of the objective function at the point. */
    private final double value;

    /**
     * Build a point/objective function value pair.
     * 
     * @param pointIn
     *        Point.
     * @param valueIn
     *        Value of an objective function at the point
     */
    public UnivariatePointValuePair(final double pointIn,
        final double valueIn) {
        this.point = pointIn;
        this.value = valueIn;
    }

    /**
     * Get the point.
     * 
     * @return the point.
     */
    public double getPoint() {
        return this.point;
    }

    /**
     * Get the value of the objective function.
     * 
     * @return the stored value of the objective function.
     */
    public double getValue() {
        return this.value;
    }
}
