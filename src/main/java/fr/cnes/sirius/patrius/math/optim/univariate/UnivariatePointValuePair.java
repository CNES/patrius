/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
    /** Serializable version identifier. */
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
