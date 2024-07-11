/**
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
 * @history creation 16/03/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:1798:10/12/2018:Add getAlternateEquinoctialParameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * This class handles reentry parameters.
 * <p>
 * The parameters used internally are the following elements:
 * 
 * <pre>
 *     altitude (m)
 *     latitude (m)
 *     longitude (m)
 *     velocity (m/s)
 *     slope of velocity (rad)
 *     azimuth of velocity (rad)
 * </pre>
 * 
 * </p>
 * <p>
 * 2 more parameters defining the central body are added:
 * 
 * <pre>
 *      equatorial radius (m)
 *      flattening
 * </pre>
 * 
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: ReentryParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class ReentryParameters extends AbstractOrbitalParameters {

    /** Epsilon for specific cases. */
    public static final double EPS = 1E-15;

    /** Root int for hash code. */
    private static final int ROOTINT = 356;

    /** Serializable UID. */
    private static final long serialVersionUID = -3761689716075493731L;

    /** Altitude (m). */
    private final double altitude;

    /** Latitude (m). */
    private final double latitude;

    /** Longitude (m). */
    private final double longitude;

    /** Velocity (m/s). */
    private final double velocity;

    /** Slope of velocity (rad). */
    private final double slope;

    /** Azimuth of velocity (rad). */
    private final double azimuth;

    /** Equatorial radius (m). */
    private final double ae;

    /** Flattening. */
    private final double f;

    /**
     * Constructor.
     * 
     * @param altitudeIn
     *        altitude (m)
     * @param latitudeIn
     *        latitude (rad)
     * @param longitudeIn
     *        longitude (rad)
     * @param velocityIn
     *        velocity (m/s)
     * @param slopeIn
     *        slope of velocity (rad)
     * @param azimuthIn
     *        azimuth of velocity (rad)
     * @param aeIn
     *        equatorial radius (m)
     * @param fIn
     *        flattening (f = (a-b)/a)
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public ReentryParameters(final double altitudeIn, final double latitudeIn,
                             final double longitudeIn, final double velocityIn, final double slopeIn,
                             final double azimuthIn, final double aeIn, final double fIn, final double mu) {
        super(mu);
        this.altitude = altitudeIn;
        this.latitude = latitudeIn;
        this.longitude = longitudeIn;
        this.velocity = velocityIn;
        this.slope = slopeIn;
        this.azimuth = azimuthIn;
        this.ae = aeIn;
        this.f = fIn;
    }

    /**
     * Getter for altitude.
     * 
     * @return altitude (m)
     */
    public double getAltitude() {
        return this.altitude;
    }

    /**
     * Getter for latitude.
     * 
     * @return latitude (rad)
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * Getter for longitude.
     * 
     * @return longitude (rad)
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * Getter for velocity.
     * 
     * @return velocity (m/s)
     */
    public double getVelocity() {
        return this.velocity;
    }

    /**
     * Getter for slope of velocity.
     * 
     * @return slope of velocity (rad)
     */
    public double getSlope() {
        return this.slope;
    }

    /**
     * Getter for azimuth of velocity.
     * 
     * @return azimuth of velocity (rad)
     */
    public double getAzimuth() {
        return this.azimuth;
    }

    /**
     * Getter for equatorial radius.
     * 
     * @return equatorial radius (m)
     */
    public double getAe() {
        return this.ae;
    }

    /**
     * Getter for flattening.
     * 
     * @return flattening
     */
    public double getF() {
        return this.f;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {

        // Build ellipsoid
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(this.ae, this.f, null);

        // Compute position
        final GeodeticPoint geodeticPoint = new GeodeticPoint(this.latitude, this.longitude, this.altitude);
        final Vector3D pos = ellipsoid.transform(geodeticPoint);

        // Compute local horizontal plane (located in position and normal = zenith)
        final Vector3D zenith = geodeticPoint.getZenith();
        final Plane horizontalPlane = new Plane(pos, zenith);
        final Vector3D u = horizontalPlane.getU();
        final Vector3D v = horizontalPlane.getV();

        // Compute local north
        final Vector3D northProj = new Vector3D(Vector3D.dotProduct(u, Vector3D.PLUS_K), u,
            Vector3D.dotProduct(v, Vector3D.PLUS_K), v);

        // Compute velocity (ROL = North, West, Zenith)
        // Azimuth is positive toward East
        final double[] sincosSlope = MathLib.sinAndCos(slope);
        final double sinSlope = sincosSlope[0];
        final double cosSlope = sincosSlope[1];
        final double[] sincosAzimut = MathLib.sinAndCos(2. * FastMath.PI - this.azimuth);
        final double sinAzimut = sincosAzimut[0];
        final double cosAzimut = sincosAzimut[1];
        final Vector3D velInlocalFrame = new Vector3D(this.velocity * cosSlope
                * cosAzimut, this.velocity * cosSlope
                * sinAzimut, this.velocity * sinSlope);

        final Vector3D vel;
        if (northProj.getNorm() < EPS) {
            // Specific case: local north is undefined
            vel = velInlocalFrame;
        } else {
            // Normal case (rotation transforming +K in zenith and +I in local north)
            final Rotation rotation = new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I, zenith,
                northProj);
            vel = rotation.applyTo(velInlocalFrame);
        }

        // acceleration
        final double r2 = pos.getNormSq();
        final Vector3D acceleration = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), pos);

        // Return result
        return new CartesianParameters(pos, vel, acceleration, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        return this.getCartesianParameters().getKeplerianParameters();
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        return this.getCartesianParameters().getCircularParameters();
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        return this.getCartesianParameters().getEquatorialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        return this.getCartesianParameters().getEquinoctialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public ApsisAltitudeParameters getApsisAltitudeParameters(final double req) {
        return this.getKeplerianParameters().getApsisAltitudeParameters(req);
    }

    /** {@inheritDoc} */
    @Override
    public ApsisRadiusParameters getApsisRadiusParameters() {
        return this.getKeplerianParameters().getApsisRadiusParameters();
    }

    /** {@inheritDoc} */
    @Override
    public ReentryParameters getReentryParameters(final double req, final double flat) {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public StelaEquinoctialParameters getStelaEquinoctialParameters() {
        return this.getCartesianParameters().getStelaEquinoctialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.getEquinoctialParameters().getAlternateEquinoctialParameters();
    }

    /**
     * Returns a string representation of the reentry parameters.
     * 
     * @return a string representation of this reentry parameters
     */
    @Override
    public String toString() {
        return new StringBuffer().append("reentry parameters: ").append('{')
            .append("altitude(m): ").append(this.altitude).append("; latitude(deg): ")
            .append(MathLib.toDegrees(this.latitude)).append("; longitude(deg): ")
            .append(MathLib.toDegrees(this.longitude)).append("; velocity(m/s): ").append(this.velocity)
            .append("; slope(deg): ").append(MathLib.toDegrees(this.slope))
            .append("; azimuth(deg): ").append(MathLib.toDegrees(this.azimuth)).append("; ae(m): ")
            .append(this.ae).append("; f: ").append(this.f).append(";}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : date, frame, type, mu,
        // a, ex, ey, hx, hy, lv
        boolean isEqual = true;

        if (object == this) {
            // first fast check
            isEqual = true;
        } else if (object instanceof ReentryParameters) {
            // cast object to compare parameters
            final ReentryParameters other = (ReentryParameters) object;

            isEqual &= (this.getMu() == other.getMu());

            // Reentry parameters
            isEqual &= (this.getAltitude() == other.getAltitude());
            isEqual &= (this.getLatitude() == other.getLatitude());
            isEqual &= (this.getLongitude() == other.getLongitude());
            isEqual &= (this.getVelocity() == other.getVelocity());
            isEqual &= (this.getSlope() == other.getSlope());
            isEqual &= (this.getAzimuth() == other.getAzimuth());
            isEqual &= (this.getAe() == other.getAe());
            isEqual &= (this.getF() == other.getF());

        } else {
            isEqual = false;
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + MathUtils.hash(this.getMu());
        result = effMult * result + MathUtils.hash(this.getAltitude());
        result = effMult * result + MathUtils.hash(this.getLatitude());
        result = effMult * result + MathUtils.hash(this.getLongitude());
        result = effMult * result + MathUtils.hash(this.getVelocity());
        result = effMult * result + MathUtils.hash(this.getSlope());
        result = effMult * result + MathUtils.hash(this.getAzimuth());
        result = effMult * result + MathUtils.hash(this.getAe());
        result = effMult * result + MathUtils.hash(this.getF());

        return result;
    }
}
