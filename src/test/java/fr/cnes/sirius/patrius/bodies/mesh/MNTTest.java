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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation tests for {@link FacetCelestialBody} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class MNTTest {

    /**
     * Build a circular ephemeris around Phobos.
     * @param statesNumber number of states in ephemeris
     * @return an ephemeris
     */
    public List<SpacecraftState> buildEphemeris(final int statesNumber) throws PatriusException {
        final List<SpacecraftState> statesList = new ArrayList<>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < statesNumber; i++) {
            final Orbit orbit = new KeplerianOrbit(20000, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 1E5).shiftedBy(i * 100);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }
        return statesList;
    }
}
