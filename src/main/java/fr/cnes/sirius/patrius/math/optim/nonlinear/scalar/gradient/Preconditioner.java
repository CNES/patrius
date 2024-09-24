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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.gradient;

/**
 * This interface represents a preconditioner for differentiable scalar
 * objective function optimizers.
 * 
 * @version $Id: Preconditioner.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public interface Preconditioner {
    /**
     * Precondition a search direction.
     * <p>
     * The returned preconditioned search direction must be computed fast or the algorithm performances will drop
     * drastically. A classical approach is to compute only the diagonal elements of the hessian and to divide the raw
     * search direction by these elements if they are all positive. If at least one of them is negative, it is safer to
     * return a clone of the raw search direction as if the hessian was the identity matrix. The rationale for this
     * simplified choice is that a negative diagonal element means the current point is far from the optimum and
     * preconditioning will not be efficient anyway in this case.
     * </p>
     * 
     * @param point
     *        current point at which the search direction was computed
     * @param r
     *        raw search direction (i.e. opposite of the gradient)
     * @return approximation of H<sup>-1</sup>r where H is the objective function hessian
     */
    double[] precondition(double[] point, double[] r);
}
