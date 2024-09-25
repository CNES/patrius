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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:DM:DM-139:08/12/2023:[PATRIUS] Suppression de l'argument frame
 * dans PVCoordinatesProvider#getNativeFrame
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans ITargetDirection
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

import fr.cnes.sirius.patrius.bodies.CelestialPoint;
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
 * Direction described by a celestial point: the celestial point is the target point.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of a celestial point linked to the tree of frames
 *                      makes this class thread-safe only if the underlying {@link CelestialPoint} is too.
 * 
 * @author Thomas Trapier
 * 
 * @since 1.1
 */
public final class ToCelestialPointDirection implements ITargetDirection {

    /** Serializable UID. */
    private static final long serialVersionUID = 7407313120126398629L;

    /** String identifying an inertial frozen frame. */
    private static final String INERTIAL_FROZEN = "inertialFrozen";

    /** Central celestial point. */
    private final CelestialPoint celestialPoint;

    /**
     * Build a direction described by a celestial point: the celestial point is the target point.
     * 
     * @param celestialPoint
     *        the celestial point
     * */
    public ToCelestialPointDirection(final CelestialPoint celestialPoint) {
        this.celestialPoint = celestialPoint;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin, final AbsoluteDate date, final Frame frame)
            throws PatriusException {
        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originInFrame = originPV.getPosition();

        // computation of the center of the celestial point point in the output frame
        final Frame celestialPointCenteredFrame = this.celestialPoint.getICRF();
        final Vector3D celestialPointCenterInFrame = celestialPointCenteredFrame.getPVCoordinates(date, frame)
            .getPosition();

        // the returned vector is from the origin center to the center of the celestial point to
        return celestialPointCenterInFrame.subtract(originInFrame);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getTargetPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        // computation of the center of the celestial point point in the output frame
        final Frame celestialPointCenteredFrame = this.celestialPoint.getICRF();

        // computation of the celestial point center PV coordinates in the output frame at the date
        return celestialPointCenteredFrame.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin, final AbsoluteDate date, final Frame frame)
            throws PatriusException {

        Line line = null;

        // computation of the origin's position in the output frame at the date
        final PVCoordinates originPV = (origin == null) ? PVCoordinates.ZERO : origin.getPVCoordinates(date, frame);
        final Vector3D originPointInFrame = originPV.getPosition();

        // computation of the center of the celestial point in the output frame
        final Frame celestialPointCenteredFrame = this.celestialPoint.getICRF();
        final Vector3D celestialPointCenterInFrame = celestialPointCenteredFrame.getPVCoordinates(date, frame)
            .getPosition();

        // creation of the line
        try {
            line = new Line(originPointInFrame, celestialPointCenterInFrame, originPointInFrame);
        } catch (final IllegalArgumentException e) {
            throw new PatriusException(e, PatriusMessages.ILLEGAL_LINE);
        }
        return line;
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
        // frame used for signal propagation
        final Frame frozenFrame = this.celestialPoint.getICRF().getFrozenFrame(
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
        // Transformation between the frozen frame and the input frame
        final Transform transform = frozenFrame.getTransformTo(frame, date);
        // Apply the frame transformation
        final Vector3D lineOriginPointFrame = transform.transformPosition(lineOriginPoint);
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
        private static final long serialVersionUID = -1486924098912919463L;

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return getTargetPVCoordinates(date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
            return ToCelestialPointDirection.this.celestialPoint.getNativeFrame(date);
        }
    }
}
