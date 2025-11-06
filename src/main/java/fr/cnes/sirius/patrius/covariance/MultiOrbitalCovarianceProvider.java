/**
 *
 * Copyright 2011-2022 CNES
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
 * @history Created 20/02/2025
 *
 * HISTORY
 * VERSION:4.16:OPENFD-379:25/04/2025:[PATRIUS] Ajout d'une implementation basique de OrbitalCovarianceProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for {@link MultiOrbitalCovariance} providers.
 *
 * <p>
 * This interface can be used by any class used for multi orbital covariance computation.
 * </p>
 *
 * @author TSN
 */
public interface MultiOrbitalCovarianceProvider extends Serializable {

    /**
     * Getter for the {@link MultiOrbitalCovariance} at the provided date.
     *
     * @param date
     *        The date at which the multi orbital covariance is wanted
     * @return the multi orbital covariance at the provided date
     * @throws PatriusException
     *         if multi orbital covariance cannot be computed at the given date
     */
    MultiOrbitalCovariance getMultiOrbitalCovariance(final AbsoluteDate date) throws PatriusException;

    /**
     * Getter for an orbital covariance provider extracting information from this multi orbital covariance.
     *
     * @param index
     *        The index of the spacecraft to be extracted
     * @return the orbital covariance provider of the required spacecraft
     */
    OrbitalCovarianceProvider getOrbitalCovarianceProvider(final int index);
}