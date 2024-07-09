/**
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
 * 
 * @history created 18/06/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis;

import java.io.Serializable;

/**
 * <p>
 * Generic interface to describe a T-dependent variable.
 * </p>
 * The generic parameter T represents the nature of the independent variable.
 * 
 * @param <T>
 *        the nature of the independent variable.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: IDependentVariable.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface IDependentVariable<T> extends Serializable {

    /**
     * Compute the value of the T-dependent variable.
     * 
     * @param x
     *        value of T for which the variable should be computed.
     * @return the value of the dependent variable.
     */
    double value(T x);

}
