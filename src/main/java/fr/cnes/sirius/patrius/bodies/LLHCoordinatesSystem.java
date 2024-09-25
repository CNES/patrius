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
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
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

            // Implementation note: this is an analytical method to compute the jacobian. It only supports
            // OneAxisEllipsoid body shape. Otherwise the generic method with finite difference is used.
            final BodyShape bodyShape = point.getBodyShape();
            if (!(bodyShape instanceof OneAxisEllipsoid)) {
                // If the body shape isn't a OneAxisEllipsoid, this method is not supported, call the generic method
                // with finite difference
                return super.jacobianToCartesian(point);
            }
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

            // Compute the components of the jacobian matrix
            final double[][] jacobian = new double[3][3];
            jacobian[0][0] = -(k * r + alt) * sinLat * cosLon;
            jacobian[0][1] = -(r + alt) * cosLat * sinLon;
            jacobian[0][2] = cosLat * cosLon;
            jacobian[1][0] = -(k * r + alt) * sinLat * sinLon;
            jacobian[1][1] = (r + alt) * cosLat * cosLon;
            jacobian[1][2] = cosLat * sinLon;
            jacobian[2][0] = (k * r + alt) * cosLat;
            jacobian[2][1] = 0.0;
            jacobian[2][2] = sinLat;

            return jacobian;
        }

        /** {@inheritDoc} */
        @Override
        public double[][] jacobianFromCartesian(final BodyPoint point) throws PatriusException {

            // Implementation note: this is an analytical method to compute the jacobian. It only supports
            // OneAxisEllipsoid body shape. Otherwise the generic method with finite difference is used.
            final BodyShape bodyShape = point.getBodyShape();
            if (!(bodyShape instanceof OneAxisEllipsoid)) {
                // If the body shape isn't a OneAxisEllipsoid, this method is not supported, call the generic method
                // with finite difference
                return super.jacobianFromCartesian(point);
            }
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
            final double[][] jacobian = new double[3][3];
            jacobian[0][0] = -sinLat * cosLon / (k * r + altitude);
            jacobian[0][1] = -sinLat * sinLon / (k * r + altitude);
            jacobian[0][2] = cosLat / (k * r + altitude);
            jacobian[1][0] = -sinLon / ((r + altitude) * cosLat);
            jacobian[1][1] = cosLon / ((r + altitude) * cosLat);
            jacobian[1][2] = 0.;
            jacobian[2][0] = cosLat * cosLon;
            jacobian[2][1] = cosLat * sinLon;
            jacobian[2][2] = sinLat;

            return jacobian;
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

    // CHECKSTYLE: resume MultipleStringLiterals check
}
