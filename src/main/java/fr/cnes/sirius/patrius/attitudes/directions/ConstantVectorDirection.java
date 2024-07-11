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
 * @history creation 30/11/2011
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:FA:FA-2588:27/01/2021:[PATRIUS] erreur de calcul dans ConstantVectorDirection.getLine(…) 
 * VERSION:4.6:DM:DM-2586:27/01/2021:[PATRIUS] intersection entre un objet de type «ExtendedOneAxisEllipsoid»
 * et une droite. 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Direction described only by a vector constant in a frame
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame linked to the tree of frames
 *                      makes this class not thread-safe.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ConstantVectorDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class ConstantVectorDirection implements IDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 6293715479652235385L;

    /** Constant vector in the frame. */
    private final Vector3D vector;

    /** The frame in which the vector is the constant given one. */
    private final Frame frameIn;

    /**
     * Build a direction from a frame and a vector constant in this frame
     * 
     * @param inVector
     *        the constant vector in the input frame
     * @param inFrame
     *        the frame in which the vector is the constant given one
     * */
    public ConstantVectorDirection(final Vector3D inVector, final Frame inFrame) {

        // the direction must not be null
        if (inVector.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_VECTOR);
        }

        // Initialisations
        this.vector = new Vector3D(1.0, inVector);
        this.frameIn = inFrame;
    }

    /**
     * @return the frame
     */
    public Frame getFrame() {
        return this.frameIn;
    }

    /**
     * Provides the direction vector at a given date in a given frame.
     * This Direction has no origin, so the pv coordinates are unused.
     * 
     * @param pvCoord
     *        UNUSED
     * @param date
     *        the date
     * @param frame
     *        the frame to project the vector's coordinates
     * @return the direction vector at the given date in the given frame
     * @exception PatriusException
     *            if some frame specific errors occur
     */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        // creation of the transformation to the output frame
        final Transform transfom = this.frameIn.getTransformTo(frame, date);

        // return the transformed vector in this frame
        return transfom.transformVector(this.vector);
    }

    /**
     * Provides the line containing the given origin point and directed by the direction vector
     * 
     * @param pvCoord
     *        the origin of the line
     * @param date
     *        the current date
     * @param frame
     *        the expression frame of the line
     * @return the Line of space containing the origin (pvCoord) and direction vector
     * @throws PatriusException
     *         if some frame specific errors occur
     */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {
        // position of origin in that frame
        final Vector3D originInFrame = (pvCoord == null) ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
                .getPosition();

        // line creation
        return new Line(originInFrame, originInFrame.add(this.getVector(pvCoord, date, frame)), originInFrame);
    }
}
