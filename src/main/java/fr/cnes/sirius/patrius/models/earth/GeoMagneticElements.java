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
 */
/* Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:205:14/03/2014:Corrected jdoc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.Serializable;
import java.text.NumberFormat;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Contains the elements to represent a magnetic field at a single point.
 * 
 * @author Thomas Neidhart
 */
public class GeoMagneticElements implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 1881493738280586855L;

    /** The magnetic field vector (North=X, East=Y, Nadir=Z). */
    private final Vector3D b;

    /** The magnetic inclination in deg. down is positive, up is negative */
    private final double inclination;

    /** The magnetic declination in deg. east is positive, west is negative */
    private final double declination;

    /** The magnetic total intensity, in nano Teslas. */
    private final double totalIntensity;

    /** The magnetic horizontal intensity, in nano Teslas. */
    private final double horizontalIntensity;

    /**
     * Construct a new element with the given field vector. The other elements
     * of the magnetic field are calculated from the field vector.
     * 
     * @param bIn
     *        the magnetic field vector in the topocentric frame (North=X, East=Y, Nadir=Z)
     */
    public GeoMagneticElements(final Vector3D bIn) {
        this.b = bIn;

        this.horizontalIntensity = MathLib.hypot(bIn.getX(), bIn.getY());
        this.totalIntensity = bIn.getNorm();
        this.declination = MathLib.toDegrees(MathLib.atan2(bIn.getY(), bIn.getX()));
        this.inclination = MathLib.toDegrees(MathLib.atan2(bIn.getZ(), this.horizontalIntensity));
    }

    /**
     * Returns the magnetic field vector in the topocentric frame (North=X, East=Y, Nadir=Z) in nTesla.
     * 
     * @return the magnetic field vector in nTesla
     */
    public Vector3D getFieldVector() {
        return this.b;
    }

    /**
     * Returns the inclination of the magnetic field in degrees.
     * 
     * @return the inclination (dip) in degrees
     */
    public double getInclination() {
        return this.inclination;
    }

    /**
     * Returns the declination of the magnetic field in degrees.
     * 
     * @return the declination (dec) in degrees
     */
    public double getDeclination() {
        return this.declination;
    }

    /**
     * Returns the total intensity of the magnetic field (= norm of the field vector).
     * 
     * @return the total intensity in nTesla
     */
    public double getTotalIntensity() {
        return this.totalIntensity;
    }

    /**
     * Returns the horizontal intensity of the magnetic field (= norm of the
     * vector in the plane spanned by the x/y components of the field vector).
     * 
     * @return the horizontal intensity in nTesla
     */
    public double getHorizontalIntensity() {
        return this.horizontalIntensity;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // Initilization
        final NumberFormat f = NumberFormat.getInstance();
        final StringBuilder sb = new StringBuilder();

        // Add elements
        //
        sb.append("MagneticField[");
        sb.append("B=");
        sb.append(this.b.toString(f));
        sb.append(",H=");
        sb.append(f.format(this.getHorizontalIntensity()));
        sb.append(",F=");
        sb.append(f.format(this.getTotalIntensity()));
        sb.append(",I=");
        sb.append(f.format(this.getInclination()));
        sb.append(",D=");
        sb.append(f.format(this.getDeclination()));
        sb.append("]");

        // Return result
        return sb.toString();
    }
}
