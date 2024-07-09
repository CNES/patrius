/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history 02/01/2012
 */
package fr.cnes.sirius.patrius.math.analysis;

/**
 * Extension of {@link UnivariateFunction} representing an integrable univariate real function.
 * 
 * @author Rami Houdroge
 * @since 1.1
 * @version $Id: IntegrableUnivariateFunction.java 17603 2017-05-18 08:28:32Z bignon $
 */
public interface IntegrableUnivariateFunction
    extends UnivariateFunction {

    /**
     * Returns the primitive of the function
     * 
     * @return the primitive function
     */
    UnivariateFunction primitive();

}
