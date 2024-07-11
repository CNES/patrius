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
 *
 * @history created 11/03/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.analysis.UnivariateVectorFunction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface is a time-dependent function representing a generic vector 3D.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: Vector3DFunction.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 1.3
 * 
 */
public interface Vector3DFunction extends UnivariateVectorFunction {

    /**
     * Get the vector at a given date. <br>
     * The vector is an instance of the {@link Vector3D} class.
     * 
     * @param date
     *        the date
     * @return the vector at a given date.
     * @throws PatriusException
     *         if vector3D cannot be computed
     */
    Vector3D getVector3D(final AbsoluteDate date) throws PatriusException;

    /**
     * Compute the {@link Vector3DFunction} representing the n-th derivative of the current vector function. The
     * derivation
     * can be analytical or numerical, depending on the current vector function.
     * 
     * @param order
     *        the order n
     * @return the n-th derivative of the current vector function.
     */
    Vector3DFunction nthDerivative(final int order);

    /**
     * Returns the integral of the vector function in the given interval. The integration can be analytical or
     * numerical,
     * depending on the current vector function.
     * 
     * @param x0
     *        the lower bound of the interval.
     * @param xf
     *        the upper bound of the interval.
     * @return the value of the integral
     */
    Vector3D integral(final double x0, final double xf);
}
