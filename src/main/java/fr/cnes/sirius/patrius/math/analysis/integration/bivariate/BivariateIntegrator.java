/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.bivariate;

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;

/**
 * Interface for bivariate real integration algorithms.
 *
 * @author GMV
 * 
 * @since 4.1
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface BivariateIntegrator {

    /**
     * Integrates the function on the specified domain.
     *
     * @param maxEval
     *        the maximum number of evaluations
     * @param f
     *        the integrand function
     * @param xmin
     *        the lower bound of the interval for the 1st axis
     * @param xmax
     *        the upper bound of the interval for the 1st axis
     * @param ymin
     *        the lower bound of the interval for the 2nd axis
     * @param ymax
     *        the upper bound of the interval for the 2nd axis
     *
     * @return the value of integral
     *
     * @throws TooManyEvaluationsException
     *         if the maximum number of function evaluations is exceeded
     * @throws MaxCountExceededException
     *         if the maximum iteration count is exceeded or a convergence
     *         problem is detected
     * @throws MathIllegalArgumentException
     *         if min > max or the endpoints do not satisfy the requirements
     *         specified by the integrator
     * @throws NullArgumentException
     *         if {@code f} is {@code null}.
     */
    double integrate(int maxEval,
                     BivariateFunction f,
                     double xmin,
                     double xmax,
                     double ymin,
                     double ymax);

    /**
     * Returns the number of function evaluations made during the last run of
     * the integrator.
     *
     * @return the number of function evaluations
     */
    int getEvaluations();

    /**
     * Returns the maximal number of function evaluations authorized during the
     * last run of the integrator.
     *
     * @return the maximal number of function evaluations
     */
    int getMaxEvaluations();

    /**
     * Returns the function used during the last run of the integrator.
     *
     * @return the last function integrated
     */
    BivariateFunction getFunction();

    /**
     * Returns the lower bounds on x used during the last call to this
     * integrator.
     *
     * @return the dimension of the integration domain
     */
    double getXMin();

    /**
     * Returns the upper bounds on x used during the last call to this
     * integrator.
     *
     * @return the dimension of the integration domain
     */
    double getXMax();

    /**
     * Returns the lower bounds on y used during the last call to this
     * integrator.
     *
     * @return the dimension of the integration domain
     */
    double getYMin();

    /**
     * Returns the upper bounds on y used during the last call to this
     * integrator.
     *
     * @return the dimension of the integration domain
     */
    double getYMax();
}
