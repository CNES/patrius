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
 * @history created 16/11/17
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1305:16/11/2017: Serializable interface implementation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;


/**
 * The kinds of solutions that a {@link BracketedUnivariateSolver
 * (bracketed univariate real) root-finding algorithm} may accept as solutions.
 * This basically controls whether or not under-approximations and
 * over-approximations are allowed.
 * 
 * <p>
 * If all solutions are accepted ({@link #ANY_SIDE}), then the solution that the root-finding algorithm returns for a
 * given root may be equal to the actual root, but it may also be an approximation that is slightly smaller or slightly
 * larger than the actual root. Root-finding algorithms generally only guarantee that the returned solution is within
 * the requested tolerances. In certain cases however, in particular for
 * {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler state events} of
 * {@link fr.cnes.sirius.patrius.math.ode.ODEIntegrator ODE solvers}, it may be necessary to guarantee that a solution
 * is returned that lies on a specific side the solution.
 * </p>
 * 
 * @see BracketedUnivariateSolver
 * @since 3.0
 * @version $Id: AllowedSolution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public enum AllowedSolution {
    /**
     * There are no additional side restriction on the solutions for
     * root-finding. That is, both under-approximations and over-approximations
     * are allowed. So, if a function f(x) has a root at x = x0, then the
     * root-finding result s may be smaller than x0, equal to x0, or greater
     * than x0.
     */
    ANY_SIDE,

    /**
     * Only solutions that are less than or equal to the actual root are
     * acceptable as solutions for root-finding. In other words,
     * over-approximations are not allowed. So, if a function f(x) has a root
     * at x = x0, then the root-finding result s must satisfy s &lt;= x0.
     */
    LEFT_SIDE,

    /**
     * Only solutions that are greater than or equal to the actual root are
     * acceptable as solutions for root-finding. In other words,
     * under-approximations are not allowed. So, if a function f(x) has a root
     * at x = x0, then the root-finding result s must satisfy s &gt;= x0.
     */
    RIGHT_SIDE,

    /**
     * Only solutions for which values are less than or equal to zero are
     * acceptable as solutions for root-finding. So, if a function f(x) has
     * a root at x = x0, then the root-finding result s must satisfy f(s) &lt;= 0.
     */
    BELOW_SIDE,

    /**
     * Only solutions for which values are greater than or equal to zero are
     * acceptable as solutions for root-finding. So, if a function f(x) has
     * a root at x = x0, then the root-finding result s must satisfy f(s) &gt;= 0.
     */
    ABOVE_SIDE;

}
