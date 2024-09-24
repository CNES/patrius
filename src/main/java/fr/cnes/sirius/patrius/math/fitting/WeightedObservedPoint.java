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
package fr.cnes.sirius.patrius.math.fitting;

import java.io.Serializable;

/**
 * This class is a simple container for weighted observed point in {@link CurveFitter curve fitting}.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @version $Id: WeightedObservedPoint.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class WeightedObservedPoint implements Serializable {
     /** Serializable UID. */
    private static final long serialVersionUID = 5306874947404636157L;
    /** Weight of the measurement in the fitting process. */
    private final double weight;
    /** Abscissa of the point. */
    private final double x;
    /** Observed value of the function at x. */
    private final double y;

    /**
     * Simple constructor.
     * 
     * @param weightIn
     *        Weight of the measurement in the fitting process.
     * @param xIn
     *        Abscissa of the measurement.
     * @param yIn
     *        Ordinate of the measurement.
     */
    public WeightedObservedPoint(final double weightIn, final double xIn, final double yIn) {
        this.weight = weightIn;
        this.x = xIn;
        this.y = yIn;
    }

    /**
     * Gets the weight of the measurement in the fitting process.
     * 
     * @return the weight of the measurement in the fitting process.
     */
    public double getWeight() {
        return this.weight;
    }

    /**
     * Gets the abscissa of the point.
     * 
     * @return the abscissa of the point.
     */
    public double getX() {
        return this.x;
    }

    /**
     * Gets the observed value of the function at x.
     * 
     * @return the observed value of the function at x.
     */
    public double getY() {
        return this.y;
    }

}
