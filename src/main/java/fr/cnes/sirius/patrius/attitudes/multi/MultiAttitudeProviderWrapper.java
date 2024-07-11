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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1872:10/10/2016:add Multi-attitude provider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Wrapper of attitude provider to make it compatible with {@link MultiAttitudeProvider}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.2
 */
public class MultiAttitudeProviderWrapper implements MultiAttitudeProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 1312446998581390686L;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Satellite ID. */
    private final String satId;

    /**
     * Constructor.
     * 
     * @param attitudeProviderIn attitude provider
     * @param satIdIn satellite ID
     */
    public MultiAttitudeProviderWrapper(final AttitudeProvider attitudeProviderIn,
        final String satIdIn) {
        this.attitudeProvider = attitudeProviderIn;
        this.satId = satIdIn;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final Map<String, PVCoordinatesProvider> pvProvs,
                                final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.attitudeProvider.getAttitude(pvProvs.get(this.satId), date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final Map<String, Orbit> orbits) throws PatriusException {
        return this.attitudeProvider.getAttitude(orbits.get(this.satId));
    }

    /**
     * Returns the AttitudeProvider.
     * 
     * @return the AttitudeProvider
     */
    public AttitudeProvider getAttitudeProvider() {
        return this.attitudeProvider;
    }

    /**
     * Returns the ID of the spacecraft associated with the AttitudeProvider.
     * 
     * @return the ID of the spacecraft associated with the AttitudeProvider
     */
    public String getID() {
        return this.satId;
    }
}
