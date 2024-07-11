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

/**
 * 
 * Date, altitude and longitude input in the reference executable to compute the referenced geomagnetic element output
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id: GeoMagRefInput.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */

public class GeoMagRefInput {

    private final double decimalYear;
    private final double altitude;
    private final double latitude;
    private final double longitude;

    public GeoMagRefInput(final double decimalYear, final double altitude, final double latitude, final double longitude) {
        this.decimalYear = decimalYear;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return the decimalYear
     */
    public double getDecimalYear() {
        return this.decimalYear;
    }

    /**
     * @return the altitude
     */
    public double getAltitude() {
        return this.altitude;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return this.longitude;
    }

}
