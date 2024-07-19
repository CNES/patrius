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
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:DM:DM-3268:22/05/2023:[PATRIUS] Creation d'une classe GeodeticTargetDirection
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans ITargetDirection
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Direction described by a target PVCoordinatesProvider.
 * The vector is at any date computed from the given PVCoordinate origin
 * to the target.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment Not thread-safe by default.
 *                      No use case for sharing an instance between threads found.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: GenericTargetDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class GenericTargetDirection implements ITargetDirection {

     /** Serializable UID. */
    private static final long serialVersionUID = 3773927800981656340L;
    
    /** String identifying an inertial frozen frame. */
    private static final String INERTIAL_FROZEN = "inertialFrozen";
    
    /** Target point. */
    private final PVCoordinatesProvider target;

    
    /**
     * Build a direction from a target described by its
     * PVCoordinatesProvider
     * 
     * @param inTarget
     *        the PVCoordinatesProvider describing the target point
     * */
    public GenericTargetDirection(final PVCoordinatesProvider inTarget) {

        // Initialisation
        this.target = inTarget;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPosition = originPV.getPosition();

        // computation of the target's position in the output frame at the date
        final PVCoordinates targetPV = this.target.getPVCoordinates(date, frame);
        final Vector3D targetPosition = targetPV.getPosition();

        // the return is the vector from the origin to the target
        return targetPosition.subtract(originPosition);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the target's PV coordinates
        // in the output frame at the date
        return this.target.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {

        Line line = null;

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPosition = originPV.getPosition();

        // computation of the target's position in the output frame at the date
        final PVCoordinates targetPV = this.target.getPVCoordinates(date, frame);
        final Vector3D targetPosition = targetPV.getPosition();

        // creation of the line
        try {
            line = new Line(originPosition, targetPosition, originPosition);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
        return line;
    }
    
    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
                              final AberrationCorrection correction, final AbsoluteDate date,
                              final FixedDate fixedDateType, final Frame frame, final double epsilon)
        throws PatriusException {
        return getVector(origin, target, signalDirection, correction, date, fixedDateType, frame, epsilon, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
                        final AberrationCorrection correction, final AbsoluteDate date, final FixedDate fixedDateType,
                        final Frame frame, final double epsilon) throws PatriusException {
        // PV provider of the emitter and receiver
        final PVCoordinatesProvider emitter;
        final PVCoordinatesProvider receiver;
        // Factor related to the signal direction and useful to determine the direction of the line
        final double dirFactorSignalDirection;
        switch (signalDirection) {
            case FROM_TARGET :
                emitter = this.target;
                receiver = origin;
                dirFactorSignalDirection = -1.;
                break;
            case TOWARD_TARGET :
                emitter = origin;
                receiver = this.target;
                dirFactorSignalDirection = 1.;
                break;                
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        // freeze the input frame at the origin date with respect to the ICRF frame.
        final Frame frozenFrame = frame.getFrozenFrame(FramesFactory.getICRF(), date, INERTIAL_FROZEN);
        // The two points to define the output line computed in the frozen frame
        final Vector3D lineOriginPoint;
        // Factor related to the fixed date type and useful to determine the direction of the line
        final double dirFactorFixedDateType;
        switch (fixedDateType) {
            case RECEPTION:
                lineOriginPoint = receiver.getPVCoordinates(date, frozenFrame).getPosition();
                dirFactorFixedDateType = -1.;
                break;
            case EMISSION:
                lineOriginPoint = emitter.getPVCoordinates(date, frozenFrame).getPosition();
                dirFactorFixedDateType = 1.;
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        // Transformation between the frozen frame and the input frame
        final Transform transform = frozenFrame.getTransformTo(frame, date);
        // Apply the frame transformation
        final Vector3D lineOriginPointFrame = transform.transformPosition(lineOriginPoint);
        // Compute the direction of the line
        final Vector3D dirVector = this.getVector(origin, signalDirection, correction, date, fixedDateType,
            frame, epsilon);
        // Direction factor (1 if signal direction is FROM_TARGET and fixed date type is RECEPTION or if signal
        // direction is TOWARD_TARGET and fixed date type is EMISSION; -1 otherwise)
        final double dirFactor = dirFactorSignalDirection * dirFactorFixedDateType;
        // Creation of the line
        Line line = null;
        try {
            line = Line.createLine(lineOriginPointFrame, dirVector.scalarMultiply(dirFactor), lineOriginPointFrame);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }

        return line;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getTargetPvProvider() {
        
        return this.target;
    }
}
