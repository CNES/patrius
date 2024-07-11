/**
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
 * @history creation 04/01/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description This class is only used in the tests of the package attitudes. This is a basic BasicAttitudeProvider :
 *              the attributes are an Attitude object that can be returned.
 * 
 * @concurrency not thread-safe
 * 
 * @author Johann Tournebize
 * 
 * @version $Id: BasicAttitudeProvider.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class BasicAttitudeProvider extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -1766417997969694524L;

    /** the constant attitude */
    private final Attitude attitude;

    /**
     * Default builder for the BasicAttitudeProvider
     * 
     * @param inAttitude
     *        the attitude
     * */

    public BasicAttitudeProvider(final Attitude inAttitude) {
        this.attitude = inAttitude;
    }

    @Override
    public
            Attitude
            getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
                                                                                                       throws PatriusException {
        return this.attitude;
    }
}
