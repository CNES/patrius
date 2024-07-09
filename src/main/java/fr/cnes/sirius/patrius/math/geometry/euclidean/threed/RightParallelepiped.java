/**
 *
 * Copyright 2011-2017 CNES
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
 * @history creation 21/05/2018
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.1:DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION:4.1.1:DM:1796:10/09/2018:remove Parallelepiped extension
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Right parallelepiped shape.
 *
 * @author Emmanuel Bignon
 *
 * @version $Id: NthOccurrenceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 4.1
 */
public class RightParallelepiped implements CrossSectionProvider, Serializable {

    /** UID */
    private static final long serialVersionUID = -7958050349239839773L;

    /** Hash code constant. */
    private static final int HASH_CODE = 41;

    /** Surface perpendicular to X axis. */
    private final double surfX;

    /** Surface perpendicular to Y axis. */
    private final double surfY;

    /** Surface perpendicular to Z axis. */
    private final double surfZ;

    /**
     * Creates a new instance of a parallelepiped.
     * 
     * @param surfXIn surface perpendicular to X axis (m2)
     * @param surfYIn surface perpendicular to Y axis (m2)
     * @param surfZIn surface perpendicular to Z axis (m2)
     */
    public RightParallelepiped(final double surfXIn, final double surfYIn, final double surfZIn) {
        this.surfX = surfXIn;
        this.surfY = surfYIn;
        this.surfZ = surfZIn;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ArithmeticException if in direction, x, y or z are NaN or if they are all 0
     */
    @Override
    public double getCrossSection(final Vector3D direction) throws ArithmeticException {

        final Vector3D dirnorm = direction.normalize();

        // Absolute value of the cosinus of the angle with each axis
        final double cosAngleX = MathLib.abs(Vector3D.dotProduct(dirnorm, Vector3D.PLUS_I));
        final double cosAngleY = MathLib.abs(Vector3D.dotProduct(dirnorm, Vector3D.PLUS_J));
        final double cosAngleZ = MathLib.abs(Vector3D.dotProduct(dirnorm, Vector3D.PLUS_K));

        // Return cross section
        return cosAngleX * this.getSurfX() + cosAngleY * this.getSurfY() + cosAngleZ * this.getSurfZ();
    }

    /**
     * Get surface perpendicular to X axis.
     * 
     * @return the surface perpendicular to X axis
     */
    public double getSurfX() {
        return this.surfX;
    }

    /**
     * Get surface perpendicular to Y axis.
     * 
     * @return the surface perpendicular to Y axis
     */
    public double getSurfY() {
        return this.surfY;
    }

    /**
     * Get surface perpendicular to Z axis.
     * 
     * @return the surface perpendicular to Z axis
     */
    public double getSurfZ() {
        return this.surfZ;
    }

    /**
     * Get x direction dimension (m).
     *
     * @return the length (x direction dimension)
     * @throws IllegalArgumentException
     *         if surfX is negative or zero (undetermined Length)
     */
    public double getLength() throws IllegalArgumentException {
        return getLengthFromSurfs(this.getSurfX(), this.getSurfY(), this.getSurfZ());
    }

    /**
     * Get y direction dimension (m).
     *
     * @return the width (y direction dimension)
     * @throws IllegalArgumentException
     *         if surfZ is negative or zero (undetermined Width)
     */
    public double getWidth() throws IllegalArgumentException {
        return getWidthFromSurfs(this.getSurfX(), this.getSurfY(), this.getSurfZ());
    }

    /**
     * Get z direction dimension (m).
     *
     * @return the height (z direction dimension)
     * @throws IllegalArgumentException
     *         if surfZ is negative or zero (undetermined height)
     */
    public double getHeight() throws IllegalArgumentException {
        return getHeightFromSurfs(this.getSurfX(), this.getSurfY(), this.getSurfZ());
    }

    /**
     * Get parallelepiped length (X direction dimension).
     * 
     * @param surfX surface perpendicular to X axis
     * @param surfY surface perpendicular to Y axis
     * @param surfZ surface perpendicular to Z axis
     * @return the parallelepiped length (X direction dimension)
     * @throws IllegalArgumentException if surfX is negative or zero (undetermined Length)
     */
    public static double getLengthFromSurfs(final double surfX, final double surfY, final double surfZ)
            throws IllegalArgumentException {
        return getDimensionFromSurfs(surfX, surfY, surfZ);
    }

    /**
     * Get parallelepiped width (Y direction dimension).
     * 
     * @param surfX surface perpendicular to X axis
     * @param surfY surface perpendicular to Y axis
     * @param surfZ surface perpendicular to Z axis
     * @return the parallelepiped width (y direction dimension)
     * @throws IllegalArgumentException if Z surface is negative or zero (undetermined Width)
     */
    public static double getWidthFromSurfs(final double surfX, final double surfY, final double surfZ)
            throws IllegalArgumentException {
        return getDimensionFromSurfs(surfY, surfX, surfZ);
    }

    /**
     * Get parallelepiped height (Z direction dimension).
     * 
     * @param surfX surface perpendicular to X axis
     * @param surfY surface perpendicular to Y axis
     * @param surfZ surface perpendicular to Z axis
     * @return the parallelepiped height (Z direction dimension)
     * @throws IllegalArgumentException if Z surface is negative or zero (undetermined height)
     */
    public static double getHeightFromSurfs(final double surfX, final double surfY, final double surfZ)
            throws IllegalArgumentException {
        return getDimensionFromSurfs(surfZ, surfX, surfY);
    }

    /**
     * Get parallelepiped dimension normal to 1st surface.
     * 
     * @param surf1 surface normal to 1st axis
     * @param surf2 surface normal to 2nd axis
     * @param surf3 surface normal to 3rd axis
     * @return the parallelepiped dimension normal to 1st surface
     * @throws IllegalArgumentException if some surface is negative or zero (undetermined Length)
     */
    private static double getDimensionFromSurfs(final double surf1, final double surf2,
                                                final double surf3) throws IllegalArgumentException {

        // Check surfaces are positive
        if (surf1 <= 0 || surf2 <= 0 || surf3 <= 0) {
            throw new IllegalArgumentException("Parallelepiped surfaces must be positive");
        }

        // Computation
        return MathLib.sqrt(surf2 * surf3 / surf1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return HASH_CODE
            * (HASH_CODE * (HASH_CODE + new Double(this.surfX).hashCode()) + new Double(this.surfY).hashCode())
            + new Double(this.surfZ).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = false;
        if (other instanceof RightParallelepiped) {
            final RightParallelepiped that = (RightParallelepiped) other;
            // REMPLACER PAR SURFACE
            result = this.getSurfX() == that.getSurfX() && this.getSurfY() == that.getSurfY()
                && this.getSurfZ() == that.getSurfZ();
        }
        return result;
    }
}
