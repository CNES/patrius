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
package fr.cnes.sirius.patrius.math.optim;

/**
 * This interface specifies how to check if an optimization algorithm has
 * converged. <br/>
 * Deciding if convergence has been reached is a problem-dependent issue. The
 * user should provide a class implementing this interface to allow the
 * optimization algorithm to stop its search according to the problem at hand. <br/>
 * For convenience, three implementations that fit simple needs are already
 * provided: {@link SimpleValueChecker}, {@link SimpleVectorValueChecker} and {@link SimplePointChecker}. The first two
 * consider that convergence is
 * reached when the objective function value does not change much anymore, it
 * does not use the point set at all.
 * The third one considers that convergence is reached when the input point
 * set does not change much anymore, it does not use objective function value
 * at all.
 * 
 * @param <PAIR>
 *        Type of the (point, objective value) pair.
 * 
 * @see fr.cnes.sirius.patrius.math.optim.SimplePointChecker
 * @see fr.cnes.sirius.patrius.math.optim.SimpleValueChecker
 * @see fr.cnes.sirius.patrius.math.optim.SimpleVectorValueChecker
 * 
 * @version $Id: ConvergenceChecker.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface ConvergenceChecker<PAIR> {
    /**
     * Check if the optimization algorithm has converged.
     * 
     * @param iteration
     *        Current iteration.
     * @param previous
     *        Best point in the previous iteration.
     * @param current
     *        Best point in the current iteration.
     * @return {@code true} if the algorithm is considered to have converged.
     */
    boolean converged(int iteration, PAIR previous, PAIR current);
}
