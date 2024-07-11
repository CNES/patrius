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
 * @history creation 06/12/2011
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3134:10/05/2022:[PATRIUS] ameliorations mineures de Vector2D 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
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
 * Direction described by a celestial body :
 * the celestial body's center is the target point.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of a celestial body linked to the tree of frames
 *                      makes this class thread-safe only if the underlying CelestialBody is too.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ToCelestialBodyCenterDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class ToCelestialBodyCenterDirection implements ITargetDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 7407313120126398629L;
    
    /** String identifying an inertial frozen frame. */
    private static final String INERTIAL_FROZEN = "inertialFrozen";
    
    /** central celestial body */
    private final CelestialBody body;


    /**
     * Build a direction described by a celestial body :
     * the celestial body's center is the target point.
     * 
     * @param inBody
     *        the celestial body
     * */
    public ToCelestialBodyCenterDirection(final CelestialBody inBody) {

        // Initialisations
        this.body = inBody;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin,
                              final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originInFrame = originPV.getPosition();

        // computation of the center of the celestial body point in the output frame
        final Frame bodyCenteredFrame = this.body.getTrueEquatorFrame();
        final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);
        final Vector3D bodyCenterInFrame = bodyFrametoOutputFrame.transformPosition(Vector3D.ZERO);

        // the returned vector is from the origin center to the center of the celestial body to
        return bodyCenterInFrame.subtract(originInFrame);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // computation of the center of the celestial body point in the output frame
        final Frame bodyCenteredFrame = this.body.getTrueEquatorFrame();
        final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);

        // null coordinates : the center of the celestial body in its own frame
        final PVCoordinates zeroCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        // computation of the celestial body center PV coordinates
        // in the output frame at the date
        return bodyFrametoOutputFrame.transformPVCoordinates(zeroCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {

        Line line = null;

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPointInFrame = originPV.getPosition();

        // computation of the center of the celestial body point in the output frame
        final Frame bodyCenteredFrame = this.body.getTrueEquatorFrame();
        final Transform bodyFrametoOutputFrame = bodyCenteredFrame.getTransformTo(frame, date);
        final Vector3D bodyCenterInFrame = bodyFrametoOutputFrame.transformPosition(Vector3D.ZERO);

        // creation of the line
        try {
            line = new Line(originPointInFrame, bodyCenterInFrame, originPointInFrame);
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
                emitter = this.getTargetPvProvider();
                receiver = origin;
                direction = -1.;
                break;
            case TOWARD_TARGET :
                emitter = origin;
                receiver = this.getTargetPvProvider();
                direction = +1.;
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        
        // frame used for signal propagation
        final Frame frozenFrame = this.body.getTrueEquatorFrame().getFrozenFrame(FramesFactory.getICRF(), date,
            INERTIAL_FROZEN);
        // signal propagation model taking into account the light speed
        final SignalPropagationModel model = new SignalPropagationModel(frozenFrame, epsilon);

        // result of signal propagation
        final SignalPropagation signal = model.computeSignalPropagation(emitter, receiver, date, fixedDateType);

        return signal.getVector(frame).scalarMultiply(direction);
    }
    
    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin,
                        final SignalDirection signalDirection, final AbsoluteDate date,
                        final FixedDate fixedDateType, final Frame frame,
                        final double epsilon) throws PatriusException {

        // PV provider of the emitter and receiver
        final PVCoordinatesProvider emitter;
        final PVCoordinatesProvider receiver;
        switch (signalDirection) {
            case FROM_TARGET:
                emitter = this.getTargetPvProvider();
                receiver = origin;
                break;
            case TOWARD_TARGET:
                emitter = origin;
                receiver = this.getTargetPvProvider();
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        
        // frame used for signal propagation
        final Frame frozenFrame = this.body.getTrueEquatorFrame().getFrozenFrame(FramesFactory.getICRF(), date,
            INERTIAL_FROZEN);
        // signal propagation model taking into account the light speed
        final SignalPropagationModel model = new SignalPropagationModel(frozenFrame, epsilon);

        // the result of signal propagation
        final SignalPropagation signal = model.computeSignalPropagation(emitter, receiver, date, fixedDateType);

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
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR,
                    null);
        }
        
        // frame transformation 
        final Transform transform = frozenFrame.getTransformTo(frame, date);

        // perform the frame transformation
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
                return body.getNativeFrame(date, frame);
            }
        };
    }
}
