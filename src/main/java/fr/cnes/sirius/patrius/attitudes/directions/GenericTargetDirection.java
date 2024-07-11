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
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagation;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel.FixedDate;
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
public final class GenericTargetDirection implements ITargetDirection {

    /** Serial UID. */
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
                              final AbsoluteDate date, final FixedDate fixedDateType, final Frame frame,
                              final double epsilon) throws PatriusException {

        // PV provider of the emitter and receiver
        final PVCoordinatesProvider emitter;
        final PVCoordinatesProvider receiver;
        final double direction;
        switch (signalDirection) {
            case FROM_TARGET :
                emitter = this.target;
                receiver = origin;
                direction = -1;
                break;
            case TOWARD_TARGET :
                emitter = origin;
                receiver = this.target;
                direction = 1;
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // freeze the input frame at the origin date with respect to itself
        final Frame frozenFrame = frame.getFrozenFrame(FramesFactory.getICRF(), date, INERTIAL_FROZEN);

        // Signal propagation model taking into account the light speed
        final SignalPropagationModel model = new SignalPropagationModel(frozenFrame, epsilon);
        
        // result of signal propagation
        final SignalPropagation signal = model.computeSignalPropagation(emitter, receiver, date, fixedDateType);

        return signal.getVector(frame).scalarMultiply(direction);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
                        final AbsoluteDate date, final FixedDate fixedDateType, final Frame frame,
                        final double epsilon) throws PatriusException {

        // PV provider of the emitter and receiver
        final PVCoordinatesProvider emitter;
        final PVCoordinatesProvider receiver;
        switch (signalDirection) {
            case FROM_TARGET :
                emitter = this.target;
                receiver = origin;
                break;
            case TOWARD_TARGET :
                emitter = origin;
                receiver = this.target;
                break;                
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // freeze the input frame at the origin date with respect to the ICRF frame.
        final Frame frozenFrame = frame.getFrozenFrame(FramesFactory.getICRF(), date, INERTIAL_FROZEN);

        // signal propagation model taking into account the light speed
        final SignalPropagationModel model = new SignalPropagationModel(frozenFrame, epsilon);
        
        // the result of signal propagation
        final SignalPropagation signal = model.computeSignalPropagation(emitter, receiver, date,
            fixedDateType);

        // the two points to define the output line
        final Vector3D lineOriginPoint;
        final Vector3D lineOtherPoint;
        switch (fixedDateType) {
            case RECEPTION:
                lineOriginPoint = receiver.getPVCoordinates(date, frozenFrame).getPosition();
                lineOtherPoint = emitter.getPVCoordinates(signal.getStartDate(), frozenFrame).getPosition();
                break;
            case EMISSION:
                lineOriginPoint = emitter.getPVCoordinates(date, frozenFrame).getPosition();
                lineOtherPoint = receiver.getPVCoordinates(signal.getEndDate(), frozenFrame).getPosition();
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        // transformation between the frozen frame and the input frame
        final Transform transform = frozenFrame.getTransformTo(frame, date);

        // apply the frame transformation
        final Vector3D lineOriginPointFrame = transform.transformPosition(lineOriginPoint);
        final Vector3D lineOtherPointFrame = transform.transformPosition(lineOtherPoint);
        
        // creation of the line
        Line line = null;
        try {
            line = new Line(lineOriginPointFrame, lineOtherPointFrame, lineOriginPointFrame);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
        
        return line;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getTargetPvProvider() {
        
        return new PVCoordinatesProvider(){
            
            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return getTargetPVCoordinates(date, frame);
            }
            
            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return target.getNativeFrame(date, frame);
            }
        };
    }


}
