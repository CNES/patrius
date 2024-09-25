/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2021 CNES
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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for {@link OrbitalCovariance} providers.
 *
 * <p>
 * This interface can be used by any class used for orbital covariance computation
 * </p>
 *
 * @author veuillh
 * 
 * @since 4.13
 */
public interface OrbitalCovarianceProvider extends PVCoordinatesProvider {

    /**
     * Getter for the {@link OrbitalCovariance} at the provided date.
     *
     * @param date
     *        The date at which the orbital covariance is wanted
     * @return the orbital covariance at the provided date
     * @throws PatriusException
     *         if orbital covariance cannot be computed at the given date
     */
    public OrbitalCovariance getOrbitalCovariance(AbsoluteDate date) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    default PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return getOrbitalCovariance(date).getOrbit().getPVCoordinates(frame);
    }
}
