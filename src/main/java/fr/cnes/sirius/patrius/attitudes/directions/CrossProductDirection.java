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
 * @history creation 09/04/2015
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:404:09/04/2015:creation direction : cross product of two directions
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This direction is the cross product of two directions
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment this class is not thread-safe
 * 
 * @author Delphine Pontisso
 * 
 * @version $Id:
 * 
 * @since 3.0
 * 
 */
public class CrossProductDirection implements IDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 8144782472961714713L;

    /** First direction. */
    private final IDirection d1;

    /** Second direction. */
    private final IDirection d2;

    /**
     * Build a cross product of two directions.
     * 
     * @param direction1
     *        first direction
     * @param direction2
     *        second direction
     * 
     * */
    public CrossProductDirection(final IDirection direction1, final IDirection direction2) {
        this.d1 = direction1;
        this.d2 = direction2;
    }

    /**
     * Provides the cross product of direction1 vector and dirction2 vector.
     * 
     * @param pvCoord
     *        PV coordinates
     * @param date
     *        a date
     * @param frame
     *        the frame to project the vector's coordinates
     * @return the direction1 vector cross product with the direction2 vector
     * @exception PatriusException
     *            if one of directions vectors could not be retrieved
     */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {
        return this.d1.getVector(pvCoord, date, frame).crossProduct(this.d2.getVector(pvCoord, date, frame));
    }

    /**
     * Provides the line containing the origin (given PV coordinates) and directed by the cross product of directions.
     * 
     * @param pvCoord
     *        PV coordinates. If null, {@link PVCoordinates#ZERO} is considered
     * @param date
     *        a date
     * @param frame
     *        the expression frame of the line
     * @return the Line of space containing the origin and direction vector
     * @throws PatriusException
     *         if some frame specific errors occur or if directions vectors could not be retrieved
     */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {
        // PV coordinates
        final PVCoordinates pv = (pvCoord == null) ? PVCoordinates.ZERO : pvCoord.getPVCoordinates(date, frame);
        // Line creation
        return new Line(pv.getPosition(), pv.getPosition().add(this.getVector(pvCoord, date, frame)), pv.getPosition());
    }

}
