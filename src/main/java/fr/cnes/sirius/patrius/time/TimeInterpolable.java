/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.Collection;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents objects that can be interpolated in time.
 * 
 * @param <T>
 *        Type of the object.
 * @author Luc Maisonobe
 */
public interface TimeInterpolable<T extends TimeInterpolable<T>> {

    /**
     * Get an interpolated instance.
     * <p>
     * Note that the state of the current instance may not be used in the interpolation process, only its type and non
     * interpolable fields are used (for example central attraction coefficient or frame when interpolating orbits). The
     * interpolable fields taken into account are taken only from the states of the sample points. So if the state of
     * the instance must be used, the instance should be included in the sample points.
     * </p>
     * 
     * @param date
     *        interpolation date
     * @param sample
     *        sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     * @throws PatriusException
     *         if the sample points are inconsistent
     */
    T interpolate(AbsoluteDate date, Collection<T> sample) throws PatriusException;

}
