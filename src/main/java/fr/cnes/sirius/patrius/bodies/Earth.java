/**
 * Copyright 2023-2023 CNES
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
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Earth body.
 * Earth body is specific since it allows to match body frames with standard Earth boy frames (MOD, TOD, GCRF,
 * etc.).
 * 
 * @author Emmanuel Bignon
 * @since 4.11.1
 */
public class Earth implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = 800054277277715849L;

    /** Earth's equatorial radius constant (2009 IAU report). */
    private static final double EARTH_EQUATORIAL_RADIUS = 6378136.6;
    /** Earth's polar radius constant (2009 IAU report). */
    private static final double EARTH_POLAR_RADIUS = 6356751.9;

    /** Default shape of Earth. */
    private BodyShape shape;

    /** Default gravitational attraction model. */
    private GravityModel gravityModel;

    /** Name. */
    private final String name;

    /**
     * Constructor.
     * @param name name
     * @param gm gravitational parameter
     * @throws PatriusException thrown if failed
     */
    public Earth(final String name, final double gm) throws PatriusException {
        this.name = name;
        final double flatness = MathLib.divide(EARTH_EQUATORIAL_RADIUS - EARTH_POLAR_RADIUS, EARTH_EQUATORIAL_RADIUS);
        this.shape = new OneAxisEllipsoid(EARTH_EQUATORIAL_RADIUS, flatness, FramesFactory.getITRF(), name);
        this.gravityModel = new NewtonianGravityModel(gm);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        // Specific implementation for Earth:
        // The Earth is always exactly at the origin of its own inertial frame
        return getInertialFrame(IAUPoleModelType.CONSTANT).getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyEphemeris getEphemeris() {
        return new EarthEphemeris();
    }

    /** {@inheritDoc} */
    @Override
    public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
        // Nothing to do, Earth ephemeris cannot be set
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return getInertialFrame(IAUPoleModelType.CONSTANT);
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getICRF() {
        return FramesFactory.getGCRF();
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getEME2000() {
        return FramesFactory.getEME2000();
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
        final CelestialBodyFrame frame;
        switch (iauPole) {
            case CONSTANT:
                // Get an inertially oriented, body centered frame taking into account only
                // constant part of IAU pole data with respect to ICRF frame. The frame is
                // always bound to the body center, and its axes have a fixed orientation with
                // respect to other inertial frames.
                frame = FramesFactory.getGCRF();
                break;
            case MEAN:
                // Get an inertially oriented, body centered frame taking into account only
                // constant and secular part of IAU pole data with respect to ICRF frame.
                frame = FramesFactory.getMOD(true);
                break;
            case TRUE:
                // Get an inertially oriented, body centered frame taking into account constant,
                // secular and harmonics part of IAU pole data with respect to ICRF frame.
                frame = FramesFactory.getTOD(true);
                break;
            default:
                // The iauPole given as input is not implemented in this method.
                throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
        }
        return frame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
        final CelestialBodyFrame frame;
        switch (iauPole) {
            case TRUE:
                // Get a body oriented, body centered frame taking into account constant, secular
                // and harmonics part of IAU pole data with respect to true equator frame. The frame
                // is always bound to the body center, and its axes have a fixed orientation with
                // respect to the celestial body.
                frame = FramesFactory.getITRF();
                break;
            case CONSTANT:
                // Get a body oriented, body centered frame taking into account only constant part
                // of IAU pole data with respect to inertially-oriented frame. The frame is always
                // bound to the body center, and its axes have a fixed orientation with respect to
                // the celestial body.
                throw new PatriusException(PatriusMessages.UNDEFINED_FRAME, "Earth constant rotating frame");
            case MEAN:
                // Get a body oriented, body centered frame taking into account constant and secular
                // part of IAU pole data with respect to mean equator frame. The frame is always
                // bound to the body center, and its axes have a fixed orientation with respect to
                // the celestial body.
                throw new PatriusException(PatriusMessages.UNDEFINED_FRAME, "Earth mean rotating frame");
            default:
                // The iauPole given as input is not implemented in this method.
                throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public BodyShape getShape() {
        return shape;
    }

    /** {@inheritDoc} */
    @Override
    public void setShape(final BodyShape shapeIn) {
        shape = shapeIn;
    }

    /** {@inheritDoc} */
    @Override
    public GravityModel getGravityModel() {
        return gravityModel;
    }

    /** {@inheritDoc} */
    @Override
    public void setGravityModel(final GravityModel modelIn) {
        gravityModel = modelIn;
    }

    /** {@inheritDoc} */
    @Override
    public double getGM() {
        return getGravityModel().getMu();
    }

    /** {@inheritDoc} */
    @Override
    public void setGM(final double gmIn) {
        getGravityModel().setMu(gmIn);
    }

    /** {@inheritDoc} */
    @Override
    public IAUPole getIAUPole() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setIAUPole(final IAUPole iauPoleIn) {
        // Nothing to do, this method is not supposed to be called
    }
}
