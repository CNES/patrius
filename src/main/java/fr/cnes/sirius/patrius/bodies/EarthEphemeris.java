/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11:FA:FA-3278:22/05/2023:[PATRIUS] Doublon de classes pour le corps celeste Earth
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Earth ephemeris.
 * 
 * @since 4.11
 */
public class EarthEphemeris implements CelestialBodyEphemeris {
    // Earth case
    // Not defined in JPL ephemeris data

    /** Serializable UID. */
    private static final long serialVersionUID = 800054277277715849L;

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                          final Frame frame) throws PatriusException {
        // Specific implementation for Earth:
        // The Earth is always exactly at the origin of its own inertial frame
        return FramesFactory.getGCRF().getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) {
        return FramesFactory.getGCRF();
    }
}
