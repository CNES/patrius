/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * 
 * The geomagnetic element extract from reference executable output file
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id: GeoMagRefOutput.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */

public class GeoMagRefOutput {

    // Magnetic field vector
    private final Vector3D b;

    // Magnetic field inclination
    private final double i;

    // Magnetic field declination
    private final double d;

    // Magnetic field total intensity
    private final double F;

    // Magnetic field horizontal intensity
    private final double H;

    public GeoMagRefOutput(final double X, final double Y, final double Z, final double i_deg, final double i_min,
        final double d_deg, final double d_min,
        final double F, final double H) {

        final int signI = (i_deg < 0) ? -1 : 1;
        final int signD = (d_deg < 0) ? -1 : 1;

        this.b = new Vector3D(X, Y, Z);
        this.i = i_deg + i_min * signI / 60.0;
        this.d = d_deg + d_min * signD / 60.0;
        this.F = F;
        this.H = H;

    }

    public GeoMagRefOutput(final double X, final double Y, final double Z, final double H, final double F,
        final double I, final double D) {

        this.b = new Vector3D(X, Y, Z);
        this.i = I;
        this.d = D;
        this.F = F;
        this.H = H;

    }

    /**
     * @return the magnetic field element
     */
    public Vector3D getB() {
        return this.b;
    }

    /**
     * @return the inclination
     */
    public double getI() {
        return this.i;
    }

    /**
     * @return the declination
     */
    public double getD() {
        return this.d;
    }

    /**
     * @return the total intensity
     */
    public double getF() {
        return this.F;
    }

    /**
     * @return the horizontal intensity
     */
    public double getH() {
        return this.H;
    }

}
