/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.15:OPENFD-351:21/11/2024:[PATRIUS] Calcul de la dérivée des coordonnées LLH
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This enumerate define the handled LLH (Latitude/Longitude/Height) coordinates systems.<br>
 * Each coordinates system is defined by:
 * <ul>
 * <li>A lat/long coordinates system</li>
 * <li>An height coordinate system</li>
 * </ul>
 *
 * @author Alice Latourte
 */
public enum LLHCoordinatesSystem {

    // CHECKSTYLE: stop MultipleStringLiterals check
    // Reason: cannot define String constant in enum class

    /** Ellipsodetic latitude/longitude and normal height: applicable to ellipsoid shapes only. */
    ELLIPSODETIC("surface ellipsodetic coord", "normal height") {

        /** {@inheritDoc} */
        @Override
        public double[][] jacobianToCartesian(final BodyPoint point) {

            // Initialise the components of the jacobian matrix
            final double[][] jacobian;

            // Implementation note: this is an analytical method to compute the jacobian. It only supports
            // OneAxisEllipsoid body shape. Otherwise the generic method with finite difference is used.
            final BodyShape bodyShape = point.getBodyShape();
            if (bodyShape instanceof OneAxisEllipsoid) {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) bodyShape;

                // Temporary variables
                final double lat = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
                final double[] sincosLat = MathLib.sinAndCos(lat);
                final double sinLat = sincosLat[0];
                final double cosLat = sincosLat[1];

                final double lon = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
                final double[] sincosLon = MathLib.sinAndCos(lon);
                final double sinLon = sincosLon[0];
                final double cosLon = sincosLon[1];

                final double alt = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();

                // Local radius
                final double g2 = ellipsoid.getG2();
                final double r = ellipsoid.getEquatorialRadius()
                        / MathLib.sqrt(MathLib.max(0.0, 1 - (1 - g2) * sinLat * sinLat));
                final double k = g2 / (1 - (1 - g2) * sinLat * sinLat);

                jacobian = new double[3][3];
                // Compute the components of the jacobian matrix
                jacobian[0][0] = -(k * r + alt) * sinLat * cosLon;
                jacobian[0][1] = -(r + alt) * cosLat * sinLon;
                jacobian[0][2] = cosLat * cosLon;
                jacobian[1][0] = -(k * r + alt) * sinLat * sinLon;
                jacobian[1][1] = (r + alt) * cosLat * cosLon;
                jacobian[1][2] = cosLat * sinLon;
                jacobian[2][0] = (k * r + alt) * cosLat;
                jacobian[2][1] = 0.0;
                jacobian[2][2] = sinLat;
            } else {
                // If the body shape isn't a OneAxisEllipsoid, this method is not supported, call the generic method
                // with finite difference
                jacobian = super.jacobianToCartesian(point);
            }

            return jacobian;
        }

        /** {@inheritDoc} */
        @Override
        public double[][] jacobianFromCartesian(final BodyPoint point) throws PatriusException {

            // Initialise the components of the jacobian matrix
            final double[][] jacobian;

            // Implementation note: this is an analytical method to compute the jacobian. It only supports
            // OneAxisEllipsoid body shape. Otherwise the generic method with finite difference is used.
            final BodyShape bodyShape = point.getBodyShape();
            if (bodyShape instanceof OneAxisEllipsoid) {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) bodyShape;

                // Cartesian coordinates
                final Vector3D cartesianPosition = point.getPosition();
                final double x = cartesianPosition.getX();
                final double y = cartesianPosition.getY();
                final double dist = MathLib.sqrt(x * x + y * y);

                final double eqRadius = ellipsoid.getEquatorialRadius();

                // Check the case if the point is close to the poles
                if (dist < AbstractEllipsoidBodyShape.CLOSE_APPROACH_THRESHOLD * eqRadius) {
                    // The point is close to one of the poles, the jacobian matrix cannot be computed
                    throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
                }

                final double g2 = ellipsoid.getG2();

                // Sinus and cosinus for latitude and longitude
                final LLHCoordinates ellipsodeticCoordinates = point.getLLHCoordinates(this);
                final double[] sincosLat = MathLib.sinAndCos(ellipsodeticCoordinates.getLatitude());
                final double sinLat = sincosLat[0];

                final double altitude = ellipsodeticCoordinates.getHeight();

                final double r = eqRadius / MathLib.sqrt(1 - (1 - g2) * sinLat * sinLat);
                final double k = g2 / (1 - (1 - g2) * sinLat * sinLat);

                // The altitude of the point is "too negative"
                if (k * r + altitude < AbstractEllipsoidBodyShape.CLOSE_APPROACH_THRESHOLD * eqRadius) {
                    throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
                }

                final double cosLat = sincosLat[1];

                final double[] sincosLon = MathLib.sinAndCos(ellipsodeticCoordinates.getLongitude());
                final double sinLon = sincosLon[0];
                final double cosLon = sincosLon[1];

                // Compute the components of the jacobian matrix
                jacobian = new double[3][3];
                jacobian[0][0] = -sinLat * cosLon / (k * r + altitude);
                jacobian[0][1] = -sinLat * sinLon / (k * r + altitude);
                jacobian[0][2] = cosLat / (k * r + altitude);
                jacobian[1][0] = -sinLon / ((r + altitude) * cosLat);
                jacobian[1][1] = cosLon / ((r + altitude) * cosLat);
                jacobian[1][2] = 0.;
                jacobian[2][0] = cosLat * cosLon;
                jacobian[2][1] = cosLat * sinLon;
                jacobian[2][2] = sinLat;
            } else {
                // If the body shape isn't a OneAxisEllipsoid, this method is not supported, call the generic method
                // with finite difference
                jacobian = super.jacobianFromCartesian(point);
            }
            
            return jacobian;
        }
        
        /**
         * <p>Computes the rates in LLH coordinates (longitude, latitude, height) from the provided
         * PVCoordinates and date using either :
         * <ul>
         *      <li>an analytical method if the ellipsoid is of type {@link OneAxisEllipsoid}, or</li>
         *      <li>a finite-difference method if the ellipsoid is of another type, such as {@link ThreeAxisEllipsoid}.</li>
         * </p>
         * <p>This method is therefore valid for all types of ellipsoids.</p>
         * 
         * @param bodyShape Input body shape.
         * @param pv Input position and velocity of spacecraft.
         * @param frame Frame in which the input PV is expressed.
         * @param date Date at which the frame conversion must be computed.
         * @return A vector of double precision numbers containing the time derivatives for longitude,
         *         latitude, and height, in rad/s, rad/s, and m/s respectively.
         * @throws PatriusException
         */
        @Override
        public double[] computeLLHRates(final BodyShape bodyShape,
                final PVCoordinates pv, final Frame frame, final AbsoluteDate date)
                throws PatriusException {
            
            double[] rates = new double[3];
            
            // Implementation note: this is an analytical method to compute the rates. It only supports
            // OneAxisEllipsoid body shape. Otherwise the generic method with finite difference is used.
            
            // Generic case using finite differences
            if (!(bodyShape instanceof OneAxisEllipsoid) || forceFiniteDifference) {
                // If the body shape isn't a OneAxisEllipsoid, this method is not supported, call the generic method
                // with finite difference
                rates = super.computeLLHRates(bodyShape, pv, frame, date);
            } else {
                // Special case using analytical formulas
                final OneAxisEllipsoid ellipsoidBodyShape = (OneAxisEllipsoid) bodyShape;

                // Convert given PV to ellipsoid coordinates, by applying the transform between the input
                // frame (given by the user) and the rotating body frame
                final Transform trans = frame.getTransformTo(ellipsoidBodyShape.getBodyFrame(), date);
                final PVCoordinates pvBody = trans.transformPVCoordinates(pv);
                final EllipsoidPoint point =
                        ellipsoidBodyShape.buildPoint(pv.getPosition(), frame, date, "");
                final LLHCoordinates coord = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC);
    
                // Convert rates
                final double transverseRadius = ellipsoidBodyShape.getEquatorialRadius();
                final double flatness = 1. - ellipsoidBodyShape.getPolarRadius() / transverseRadius;
                final double modFlatness = flatness * (2. - flatness);
                final double normAltitude = coord.getHeight() / transverseRadius;
                final double sLat = MathLib.sin(coord.getLatitude());
                final double cLat = MathLib.cos(coord.getLatitude());
                final double nVar = 1. / MathLib.sqrt(1.0 - modFlatness * sLat * sLat);
                final double xVar = (nVar + normAltitude) * cLat;
                final double radiusXY = transverseRadius * xVar;
                final double nVarSinLat = nVar * sLat;
                final double det =
                        nVar * modFlatness * cLat * cLat * (1. + modFlatness * nVarSinLat * nVarSinLat)
                                - (nVar + normAltitude);
                final double sLon = MathLib.sin(coord.getLongitude());
                final double cLon = MathLib.cos(coord.getLongitude());
                final fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D vel =
                        pvBody.getVelocity();
    
                // longitude rate
                rates[0] = (vel.getY() * cLon - vel.getX() * sLon) / radiusXY;
                final double xDot = (vel.getX() * cLon + vel.getY() * sLon) / transverseRadius;
                final double zDot = vel.getZ() / transverseRadius;
    
                // ellipsoid latitude rate
                rates[1] = (sLat * xDot - cLat * zDot) / det;
                final double nVarModFlat = nVar * (1. - modFlatness);
                final double nToSc = modFlatness * nVar * nVar * sLat * cLat;
    
                // ellipsoid altitude rate
                rates[2] = ((nVar * nToSc * cLat - (nVar + normAltitude) * sLat) * zDot
                        - (nVarModFlat * nToSc * sLat + (nVarModFlat + normAltitude) * cLat) * xDot)
                        * (transverseRadius / det);
            }
            return rates;
        }
    },

    /** Bodycentric latitude/longitude, and radial height. */
    BODYCENTRIC_RADIAL("surface bodycentric coord", "radial height"),

    /** Bodycentric latitude/longitude, and normal height. */
    BODYCENTRIC_NORMAL("surface bodycentric coord", "normal height");

    /** Label for the managed lat/long system. */
    private final String latLongSystemLabel;

    /** Label for the managed height system. */
    private final String heightSystemLabel;
    
    /**
     * <p>Boolean that forces the computation of the LLH derivatives using finite-differences, even in
     * the case of a OneAxisEllipsoid.
     * This is only meant to be used in unit tests to compare the analytical solution and the
     * finite-differences implementation for cross-validation purposes. </p>
     * 
     * <p>This boolean should never be changed by the user and its value should remain <i>false</i>.</p>
     */
    boolean forceFiniteDifference = false;

    /**
     * Private constructor.
     *
     * @param latLongSystemLabel
     *        label for the managed lat/long coordinates system
     * @param heightSystemLabel
     *        label for the managed height coordinate system
     */
    private LLHCoordinatesSystem(final String latLongSystemLabel, final String heightSystemLabel) {
        this.latLongSystemLabel = latLongSystemLabel;
        this.heightSystemLabel = heightSystemLabel;
    }

    /**
     * Getter for the label for the managed lat/long coordinates system.
     * 
     * @return the label for the managed lat/long coordinates system
     */
    public final String getLatLongSystemLabel() {
        return this.latLongSystemLabel;
    }

    /**
     * Getter for the label for the managed height coordinate system.
     * 
     * @return the label for the managed height coordinate system
     */
    public final String getHeightSystemLabel() {
        return this.heightSystemLabel;
    }

    /**
     * Compute the jacobian from the LLHCoordinate system to the cartesian system.
     * 
     * @param point
     *        The pivot point for the jacobian computation
     * @return the jacobian with the following columns: latitude, longitude, height
     */
    public double[][] jacobianToCartesian(final BodyPoint point) {
        final BodyShape bodyShape = point.getBodyShape();

        // Extract the central coordinates
        final LLHCoordinates centralLLHCoordinates = point.getLLHCoordinates(this);
        final double centralLat = centralLLHCoordinates.getLatitude();
        final double centralLon = centralLLHCoordinates.getLongitude();
        final double centralHeight = centralLLHCoordinates.getHeight();

        // Finite difference deltas
        final double deltaLat = 0.01;
        final double deltaLon = 0.01;
        final double deltaHeight = 1;

        // Compute the finites difference values
        final Vector3D posPlusLat = bodyShape.buildPoint(this, centralLat + deltaLat, centralLon, centralHeight,
            "PlusLat").getPosition();
        final Vector3D posMinusLat = bodyShape.buildPoint(this, centralLat - deltaLat, centralLon, centralHeight,
            "MinusLat").getPosition();

        final Vector3D posPlusLon = bodyShape.buildPoint(this, centralLat, centralLon + deltaLon, centralHeight,
            "PlusLon").getPosition();
        final Vector3D posMinusLon = bodyShape.buildPoint(this, centralLat, centralLon - deltaLon, centralHeight,
            "MinusLon").getPosition();

        final Vector3D posPlusHeight = bodyShape.buildPoint(this, centralLat, centralLon, centralHeight + deltaHeight,
            "PlusHeight").getPosition();
        final Vector3D posMinusHeight = bodyShape.buildPoint(this, centralLat, centralLon, centralHeight - deltaHeight,
            "MinusHeight").getPosition();

        // Factors 2 * delta
        final double factorLat = 2 * deltaLat;
        final double factorLon = 2 * deltaLon;
        final double factorHeight = 2 * deltaHeight;

        // Compute the jacobian by finite difference
        return new double[][] {
            { (posPlusLat.getX() - posMinusLat.getX()) / factorLat,
                (posPlusLon.getX() - posMinusLon.getX()) / factorLon,
                (posPlusHeight.getX() - posMinusHeight.getX()) / factorHeight },
            { (posPlusLat.getY() - posMinusLat.getY()) / factorLat,
                (posPlusLon.getY() - posMinusLon.getY()) / factorLon,
                (posPlusHeight.getY() - posMinusHeight.getY()) / factorHeight },
            { (posPlusLat.getZ() - posMinusLat.getZ()) / factorLat,
                (posPlusLon.getZ() - posMinusLon.getZ()) / factorLon,
                (posPlusHeight.getZ() - posMinusHeight.getZ()) / factorHeight }
        };
    }

    /**
     * Compute the jacobian from the cartesian system to the LLHCoordinate system.
     * 
     * @param point
     *        The pivot point for the jacobian computation
     * @return the jacobian with the following rows: latitude, longitude, height
     * @throws PatriusException
     *         if point cannot be converted to body frame<br>
     *         if the point is close to one of the poles or if the altitude of the point is "too negative"
     */
    // PatriusException exception needed for the ELLIPSODETIC overridden methods
    @SuppressWarnings("unused")
    public double[][] jacobianFromCartesian(final BodyPoint point) throws PatriusException {
        final BodyShape bodyShape = point.getBodyShape();

        // Extract the central cartesian position
        final Vector3D centralPosition = point.getPosition();

        // Finite difference delta
        final double delta = 0.1;

        // Compute the finites difference values
        final LLHCoordinates llhPlusX = bodyShape.buildPoint(centralPosition.add(delta, Vector3D.PLUS_I), "PlusX")
            .getLLHCoordinates(this);
        final LLHCoordinates llhMinusX = bodyShape.buildPoint(centralPosition.add(delta, Vector3D.MINUS_I), "MinusX")
            .getLLHCoordinates(this);

        final LLHCoordinates llhPlusY = bodyShape.buildPoint(centralPosition.add(delta, Vector3D.PLUS_J), "PlusY")
            .getLLHCoordinates(this);
        final LLHCoordinates llhMinusY = bodyShape.buildPoint(centralPosition.add(delta, Vector3D.MINUS_J), "MinusY")
            .getLLHCoordinates(this);

        final LLHCoordinates llhPlusZ = bodyShape.buildPoint(centralPosition.add(delta, Vector3D.PLUS_K), "PlusZ")
            .getLLHCoordinates(this);
        final LLHCoordinates llhMinusZ = bodyShape.buildPoint(centralPosition.add(delta, Vector3D.MINUS_K), "MinusZ")
            .getLLHCoordinates(this);

        // Factor 2 * delta
        final double factor = 2 * delta;

        // Compute the jacobian by finite difference
        return new double[][] {
            { (llhPlusX.getLatitude() - llhMinusX.getLatitude()) / factor,
                (llhPlusY.getLatitude() - llhMinusY.getLatitude()) / factor,
                (llhPlusZ.getLatitude() - llhMinusZ.getLatitude()) / factor },
            { (llhPlusX.getLongitude() - llhMinusX.getLongitude()) / factor,
                (llhPlusY.getLongitude() - llhMinusY.getLongitude()) / factor,
                (llhPlusZ.getLongitude() - llhMinusZ.getLongitude()) / factor },
            { (llhPlusX.getHeight() - llhMinusX.getHeight()) / factor,
                (llhPlusY.getHeight() - llhMinusY.getHeight()) / factor,
                (llhPlusZ.getHeight() - llhMinusZ.getHeight()) / factor }
        };
    }
            

    /**
     * <p>Computes the rates in LLH coordinates (longitude, latitude, height) from the provided
     * PVCoordinates and date using finite differences. This method is valid for all types of ellipsoids.</p>
     * 
     * <p>The step size for the finite differences is 0.01 s, which minimizes the error for the
     * OneAxisEllipsoid (Earth) case with an orbit at 700 km of altitude.</p>
     * 
     * @param bodyShape Input body shape.
     * @param pv Input position and velocity of spacecraft.
     * @param frame Frame in which the input PV is expressed.
     * @param date Date at which the frame conversion must be computed.
     * @return A vector of double precision numbers containing the time derivatives for longitude,
     *         latitude, and height, in rad/s, rad/s, and m/s respectively.
     * @throws PatriusException
     */
    public double[] computeLLHRates(final BodyShape bodyShape, final PVCoordinates pv, final Frame frame,
            final AbsoluteDate date) throws PatriusException {
        final double[] rates = new double[] { 0.0, 0.0, 0.0 };

        // Finite-differences step size for the computation of the rates in LLR coordinates. The
        // step is expressed in seconds.
        final double step = 0.01;

        // It has been decided to not convert the input PV into an inertial frame

        // Compute position at t+dt by considering a constant velocity over a time dt : p(t+dt) =
        // p(t) + v(t) * dt
        final Vector3D positionTPlusDt = pv.getPosition()
                .add(step, pv.getVelocity());
        final BodyPoint pointTPlusDt = bodyShape.buildPoint(positionTPlusDt,
                frame, date.shiftedBy(step), "");
        final LLHCoordinates coordTPlusDt =
                pointTPlusDt.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC);

        // Compute position coordinates at t-dt by considering a constant velocity over a time dt :
        // p(t-dt) = p(t) - v(t) * dt
        final Vector3D positionTMinusDt = pv.getPosition()
                .add(-step, pv.getVelocity());
        final BodyPoint pointTMinusDt = bodyShape.buildPoint(positionTMinusDt,
                frame, date.shiftedBy(-step), "");
        final LLHCoordinates coordTMinusDt =
                pointTMinusDt.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC);

        // Estimate longitude rate
        rates[0] = (coordTPlusDt.getLongitude() - coordTMinusDt.getLongitude())
                / (2.0 * step);

        // Estimate latitude rate
        rates[1] = (coordTPlusDt.getLatitude() - coordTMinusDt.getLatitude())
                / (2.0 * step);

        // Estimate height rate
        rates[2] = (coordTPlusDt.getHeight() - coordTMinusDt.getHeight())
                / (2.0 * step);

        return rates;
    }
    
    // CHECKSTYLE: resume MultipleStringLiterals check
}
