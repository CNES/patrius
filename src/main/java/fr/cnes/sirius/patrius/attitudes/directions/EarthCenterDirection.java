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
 * @history creation 22/10/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans l'interface ITargetDirection
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3134:10/05/2022:[PATRIUS] ameliorations mineures de Vector2D 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:419:22/10/2015: Creation direction to central body center
 * VERSION::DM:557:15/02/2016: class rename
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
 * Direction to Earth body center : the central body's center is the target point.
 * This direction is directed toward GCRF frame origin (i.e. Earth center).
 * 
 * @concurrency immutable
 * 
 * @author maggioranic
 * 
 * @version $Id: EarthCenterDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 */
public class EarthCenterDirection implements ITargetDirection {

     /** Serializable UID. */
    private static final long serialVersionUID = -6945414164388862443L;
    
    /** String identifying an inertial frozen frame. */
    private static final String INERTIAL_FROZEN = "inertialFrozen";
    
    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {

        final Transform t = frame.getTransformTo(FramesFactory.getGCRF(), date);

        // computation of the origin's position in the output frame at the date
        final Vector3D posInFrame = (pvCoord == null) ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
            .getPosition();

        // computation of the origin's position in the GCRF frame at the date
        final Vector3D posInGCRF = t.transformPosition(posInFrame);

        // computation of the negated origin's position in the GCRF frame at the date
        final Vector3D vectorInGCRF = posInGCRF.negate();

        // return the negated origin's position in the output frame
        return t.getInverse().transformVector(vectorInGCRF);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {

        // computation of the origin's PV in the output frame at the date
        final Vector3D originPosition = (pvCoord == null) ? Vector3D.ZERO : pvCoord.getPVCoordinates(date, frame)
            .getPosition();

        // computation of the target's position in the output frame at the date
        final Vector3D targetPosition = this.getTargetPVCoordinates(date, frame).getPosition();

        // creation of the line
        try {
            return new Line(originPosition, targetPosition, originPosition);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return FramesFactory.getGCRF().getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
                              final AberrationCorrection correction, final AbsoluteDate date,
                              final FixedDate fixedDateType, final Frame frame, final double epsilon)
        throws PatriusException {
        return getVector(origin, getTargetPvProvider(), signalDirection, correction, date, fixedDateType, frame,
                epsilon, FramesFactory.getGCRF());
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
            case FROM_TARGET:
                emitter = this.getTargetPvProvider();
                receiver = origin;
                dirFactorSignalDirection = -1.;
                break;
            case TOWARD_TARGET:
                emitter = origin;
                receiver = this.getTargetPvProvider();
                dirFactorSignalDirection = 1.;
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        // The inertial body frame is frozen wrt ICRF frame
        final Frame frozenFrame = FramesFactory.getGCRF()
            .getFrozenFrame(FramesFactory.getICRF(), date, INERTIAL_FROZEN);
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
        return FramesFactory.getGCRF();
    }
}
