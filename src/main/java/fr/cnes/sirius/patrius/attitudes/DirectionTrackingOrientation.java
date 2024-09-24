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
 * @history creation 03/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description One direction orientation law. This law has to be used within a composed attitude law as a law modifier.
 * 
 * @concurrency conditionally thread safe
 * 
 * @concurrency.comment the IDirection object has to be thread-safe itself
 * 
 * @author Julie Anton
 * 
 * @version $Id: DirectionTrackingOrientation.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class DirectionTrackingOrientation implements IOrientationLaw {

     /** Serializable UID. */
    private static final long serialVersionUID = 7047076996431138599L;

    /** Target which determinates the direction. */
    private final IDirection dir;
    /** Satellite axis aligned with the direction. */
    private final Vector3D axis;
    /** Fixed satellite axis. */
    private final Vector3D fixedAxis;

    /**
     * Constructor.
     * 
     * @param direction
     *        : the direction that has to be followed
     * @param satelliteAxis
     *        : satellite axis aligned with the first direction
     * @param satelliteFixedAxis
     *        : fixed satellite axis that defines the second direction
     */
    public DirectionTrackingOrientation(final IDirection direction, final Vector3D satelliteAxis,
        final Vector3D satelliteFixedAxis) {
        this.dir = direction;
        this.axis = satelliteAxis;
        this.fixedAxis = satelliteFixedAxis;

    }

    /** {@inheritDoc} */
    @Override
    public Rotation getOrientation(final AbsoluteDate date, final Frame frame) throws PatriusException {
        // the satellite axis has to be best aligned with the given direction
        final Vector3D vd2 = this.dir.getVector(null, date, frame);
        return new Rotation(this.fixedAxis, this.axis, this.fixedAxis, vd2);
    }
}
