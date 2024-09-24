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
 * @history creation 30/09/2016
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans ITargetDirection
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3134:10/05/2022:[PATRIUS] ameliorations mineures de Vector2D 
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
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
 * 
 * Direction from Earth center to celestial body center : the central body's center is the target
 * point.
 * 
 * @concurrency immutable
 * 
 * @author rodriguest
 * 
 * @version $Id: EarthToCelestialBodyCenterDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.3
 * 
 */
public class EarthToCelestialBodyCenterDirection implements ITargetDirection {

    /** Serializable UID. */
    private static final long serialVersionUID = 2212101322761333705L;

    /** Earth center frame: GCRF. */
    private static final Frame EARTH_FRAME = FramesFactory.getGCRF();

    /** String identifying an inertial frozen frame. */
    private static final String INERTIAL_FROZEN = "inertialFrozen";

    /** Celestial body. */
    private final CelestialBody bodyTo;

    /**
     * Constructor for celestial body center direction from Earth center :
     * the celestial body's center is the target point.
     * 
     * @param body
     *        the celestial body
     */
    public EarthToCelestialBodyCenterDirection(final CelestialBody body) {
        this.bodyTo = body;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date, final Frame frame)
            throws PatriusException {

        // Transform to output frame
        final Transform t = EARTH_FRAME.getTransformTo(frame, date);

        // Expressed body center direction in output frame
        // Return Earth => celestial body vector in frame
        return t.transformVector(this.bodyTo.getPVCoordinates(date, EARTH_FRAME).getPosition());
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date, final Frame frame)
            throws PatriusException {

        // Transform to output frame
        final Transform t = EARTH_FRAME.getTransformTo(frame, date);

        // Earth center in frame
        final Vector3D earthPosInFrame = t.transformVector(Vector3D.ZERO);

        // Body center in frame
        final Vector3D bodyPosInFrame = t
                .transformVector(this.bodyTo.getPVCoordinates(date, EARTH_FRAME).getPosition());

        // creation of the line
        try {
            return new Line(bodyPosInFrame, earthPosInFrame, bodyPosInFrame);

        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return new ToCelestialBodyCenterDirection(this.bodyTo).getTargetPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
            final AberrationCorrection correction, final AbsoluteDate date, final FixedDate fixedDateType,
            final Frame frame, final double epsilon) throws PatriusException {
        final EarthCenterDirection earthCenter = new EarthCenterDirection();
        final PVCoordinatesProvider earthProvider = earthCenter.getTargetPvProvider();
        return getVector(earthProvider, getTargetPvProvider(), signalDirection, correction, date, fixedDateType, frame,
                epsilon, this.bodyTo.getInertialFrame(IAUPoleModelType.TRUE));
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin, final SignalDirection signalDirection,
            final AberrationCorrection correction, final AbsoluteDate date, final FixedDate fixedDateType,
            final Frame frame, final double epsilon) throws PatriusException {
        // PV provider of the earth center
        final EarthCenterDirection earthCenter = new EarthCenterDirection();
        final PVCoordinatesProvider earthProvider = earthCenter.getTargetPvProvider();
        // PV provider of the emitter and receiver
        final PVCoordinatesProvider emitter;
        final PVCoordinatesProvider receiver;
        // Factor related to the signal direction and useful to determine the direction of the line
        final double dirFactorSignalDirection;
        switch (signalDirection) {
            case FROM_TARGET:
                emitter = this.getTargetPvProvider();
                receiver = earthProvider;
                dirFactorSignalDirection = -1.;
                break;
            case TOWARD_TARGET:
                emitter = earthProvider;
                receiver = this.getTargetPvProvider();
                dirFactorSignalDirection = 1.;
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        // Freeze the body center frame at the given date wrt to ICRF frame
        final Frame frozenFrame = this.bodyTo.getInertialFrame(IAUPoleModelType.TRUE).getFrozenFrame(
                FramesFactory.getICRF(), date, INERTIAL_FROZEN);
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
        // Frame transformation between the frozen frame and the input frame
        final Transform frozenToFrame = frozenFrame.getTransformTo(frame, date);
        // Perform the frame transformation
        final Vector3D lineOriginPointFrame = frozenToFrame.transformPosition(lineOriginPoint);
        // Compute the direction of the line
        final Vector3D dirVector = this.getVector(origin, signalDirection, correction, date, fixedDateType, frame,
                epsilon);
        // Direction factor (1 if signal direction is FROM_TARGET and fixed date type is RECEPTION
        // or if signal
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
        return new TargetPVCoordinatesProvider();
    }

    /**
     * Target PV coordinates provider.
     *
     * @since 4.10
     */
    private class TargetPVCoordinatesProvider implements PVCoordinatesProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 2356715705757435381L;

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return getTargetPVCoordinates(date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return EarthToCelestialBodyCenterDirection.this.bodyTo.getNativeFrame(date, frame);
        }
    }
}
