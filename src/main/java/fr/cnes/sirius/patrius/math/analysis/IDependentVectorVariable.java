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
 * @history created 18/06/2012
 */
package fr.cnes.sirius.patrius.math.analysis;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * <p>
 * Generic interface to describe a T-dependent 3D vector.
 * </p>
 * The generic parameter T represents the nature of the independent variable.
 * 
 * @param <T>
 *        the nature of the independent variable.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: IDependentVectorVariable.java 17603 2017-05-18 08:28:32Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface IDependentVectorVariable<T> extends Serializable {

    /**
     * Compute the value of the T-dependent 3D vector.
     * 
     * @param x
     *        value of T for which the variable should be computed.
     * @return the value of the dependent vector.
     */
    Vector3D value(T x);
}