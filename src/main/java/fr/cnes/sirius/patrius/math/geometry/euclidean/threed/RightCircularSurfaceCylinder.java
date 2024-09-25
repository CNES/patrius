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
 * $Id: CustomCylinder.java 627 2017-10-30 11:21:12Z jjct $
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1796:03/10/2018:Correction vehicle class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Cylinder shape class.
 *
 * @author sumelzi
 *
 */
public class RightCircularSurfaceCylinder implements CrossSectionProvider {

    /** Hash code constant. */
    private static final int HASH_CODE = 41;

    /** Surface perpendicular to X axis. */
    private final double xAxisSurf;

    /** Transversal surface (which contains x axis). */
    private final double transversalSurf;

    /**
     * Creates a new instance of a cylinder.
     *
     * @param xAxisSurfIn
     *        surface perpendicular to X axis (m2)
     * @param transversalSurfIn
     *        transversal surface (m2)
     */
    public RightCircularSurfaceCylinder(final double xAxisSurfIn, final double transversalSurfIn) {
        if (Double.isNaN(xAxisSurfIn) || xAxisSurfIn < 0) {
            throw new IllegalArgumentException("Negative x axis surface or NaN value.");
        }
        if (Double.isNaN(transversalSurfIn) || transversalSurfIn < 0) {
            throw new IllegalArgumentException("Negative transversal surface or NaN value.");
        }
        this.xAxisSurf = xAxisSurfIn;
        this.transversalSurf = transversalSurfIn;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ArithmeticException
     *         if in direction, x, y or z are NaN or if they are all 0
     */
    @Override
    public double getCrossSection(final Vector3D direction) throws ArithmeticException {
        // compute cos alfa x
        final double cosAlfaX = MathLib.abs(direction.normalize().dotProduct(Vector3D.PLUS_I));

        // compute sin alfa x
        double sinAlfaX = 0;
        if (cosAlfaX < 1) {
            // (This is to avoid sqrt(negative)->NaN for the case cosAlfaX=1+eps)
            sinAlfaX = MathLib.sqrt(1 - MathLib.pow(cosAlfaX, 2));
        }
        // reference surface
        return this.xAxisSurf * cosAlfaX + this.transversalSurf * sinAlfaX;
    }

    /**
     * Get surface perpendicular to X axis (m2).
     *
     * @return the surface perpendicular to X axis
     */
    public double getSurfX() {
        return this.xAxisSurf;
    }

    /**
     * Get radius corresponding to perpendicular x axis surface (m).
     *
     * @return the radius
     */
    public double getRadius() {
        return Sphere.getRadiusFromSurface(this.getSurfX());
    }

    /**
     * Get length corresponding to transversal surface (x length) (m).
     *
     * @return the length
     * @throws IllegalArgumentException
     *         if radius is negative or zero (undetermined length)
     */
    public double getLength() throws IllegalArgumentException {
        return getLengthFromTSurfaceAndRadius(this.getTransversalSurf(), this.getRadius());
    }

    /**
     * Get equivalent transversal surface. This surface is used in order to modelize the cylinder as a
     * parallelepiped (m2).
     *
     * @return the transveral surface of the equivalent parallelepiped
     */
    public double getEquivalentTransversalSurf() {
        return (MathLib.sqrt(2) + 2) / 4.0 * this.transversalSurf;
    }

    /**
     * Get transversal surface (m2).
     *
     * @return the transversal surface
     */
    public double getTransversalSurf() {
        return this.transversalSurf;
    }

    /**
     * Get length from the transversal surface and radius.
     *
     * @param tarnsversalSurf
     *        cylinder transversal surface
     * @param radius
     *        cylinder radius
     * @return the cylinder length
     * @throws IllegalArgumentException
     *         if radius is negative or zero (undetermined length)
     */
    public static double getLengthFromTSurfaceAndRadius(final double tarnsversalSurf, final double radius)
            throws IllegalArgumentException {
        if (radius <= 0) {
            throw new IllegalArgumentException("Cylinder radius must be strictly positive");
        }
        return tarnsversalSurf / (2.0 * radius);
    }

    /**
     * Get transversal surface from radius and length.
     *
     * @param radius
     *        cylinder radius
     * @param length
     *        cylinder length
     * @return the surface
     */
    public static double getTSurfaceFromRadiusAndLength(final double radius, final double length) {
        return 2.0 * radius * length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = false;
        if (other instanceof RightCircularSurfaceCylinder) {
            final RightCircularSurfaceCylinder that = (RightCircularSurfaceCylinder) other;
            result = this.xAxisSurf == that.xAxisSurf && this.transversalSurf == that.transversalSurf;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return HASH_CODE * (HASH_CODE + new Double(this.xAxisSurf).hashCode())
            + new Double(this.transversalSurf).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s:[transversalSurface=%s, baseSurface=%s]", this.getClass().getSimpleName(),
            this.getTransversalSurf(), this.getSurfX());
    }
}
