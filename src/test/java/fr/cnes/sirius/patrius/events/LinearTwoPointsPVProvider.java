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
 * @history creation 23/04/2012
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              This class s a PVCoordinatesProvider whose coordinates are linearly interpolated between two date
 *              points.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class LinearTwoPointsPVProvider implements PVCoordinatesProvider {

    /** start point position */
    private final Vector3D startPos;

    /** start point velocity */
    private final Vector3D startVel;

    /** start point date */
    private final AbsoluteDate inStartDate;

    /** position diff */
    private final Vector3D posDiff;

    /** velocity diff */
    private final Vector3D velDiff;

    /** reference frame */
    private final Frame inFrame;

    /** end date - start date */
    private final double duration;

    /**
     * constructor for a Simple linear two points interpolated PV coordinates provider.
     * 
     * @param startPV
     *        start point PV
     * @param startDate
     *        start point date
     * @param endPV
     *        end point PV
     * @param endDate
     *        end point date
     * @param frame
     *        the frame in which those coordinates are expressed
     */
    public LinearTwoPointsPVProvider(final PVCoordinates startPV, final AbsoluteDate startDate,
        final PVCoordinates endPV, final AbsoluteDate endDate, final Frame frame) {

        this.inStartDate = startDate;
        this.startPos = startPV.getPosition();
        this.startVel = startPV.getVelocity();
        this.posDiff = endPV.getPosition().subtract(this.startPos);
        this.velDiff = endPV.getVelocity().subtract(this.startVel);
        this.inFrame = frame;
        this.duration = endDate.durationFrom(startDate);
    }

    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        final double durationFromStart = date.durationFrom(this.inStartDate);
        final double h = durationFromStart / this.duration;

        final Vector3D pos = this.startPos.add(h, this.posDiff);
        final Vector3D vel = this.startVel.add(h, this.velDiff);

        final Transform trans = this.inFrame.getTransformTo(frame, date);

        final PVCoordinates res = new PVCoordinates(pos, vel);

        return trans.transformPVCoordinates(res);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return inFrame;
    }
}
