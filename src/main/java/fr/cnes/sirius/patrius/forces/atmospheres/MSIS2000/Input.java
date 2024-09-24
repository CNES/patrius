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
 * @history 03/07/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:The log id format has been corrected
 * VERSION::DM:130:08/10/2013:Updated MSIS2000 model
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000;

/**
 * 
 * Class Input NOTES ON INPUT VARIABLES: UT, Local Time, and Longitude are used independently in the model and are not
 * of equal importance for every situation. For the most physically realistic calculation these three variables should
 * be consistent (lst=sec/3600 + g_long/15). The Equation of Time departures from the above formula for apparent local
 * time can be included if available but are of minor importance.
 * 
 * f107 and f107A values used to generate the model correspond to the 10.7 cm radio flux at the actual distance of the
 * Earth from the Sun rather than the radio flux at 1 AU. The following site provides both classes of values:
 * ftp://ftp.ngdc.noaa.gov/STP/SOLAR_DATA/SOLAR_RADIO/FLUX/
 * 
 * f107, f107A, and ap effects are neither large nor well established below 80 km and these parameters should be set to
 * 150., 150., and 4. respectively.
 * 
 * 
 * @concurrency not thread-safe
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Input.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class Input {
    /** day of year */
    private int doy;
    /** seconds in day (UT) */
    private double sec;
    /** altitude in kilometers */
    private double alt;
    /** geodetic latitude */
    private double gLat;
    /** geodetic longitude */
    private double gLong;
    /** local apparent solar time (hours), see note above */
    private double lst;
    /** 81 day average of F10.7 flux (centered on doy) */
    private double f107A;
    /** daily F10.7 flux for previous day */
    private double f107;
    /** magnetic index(daily) */
    private double ap;
    /** magnetic index */
    private ApCoef apA;

    /**
     * Constructor
     */
    public Input() {
        this.doy = 0;
        this.sec = 0.0;
        this.alt = 0.0;
        this.gLat = 0.0;
        this.gLong = 0.0;
        this.lst = 0.0;
        this.f107A = 0.0;
        this.f107 = 0.0;
        this.ap = 0.0;
        this.apA = new ApCoef();
    }

    // ================================= GETTERS =================================

    /**
     * Getter for alt.
     * 
     * @return the alt
     */
    public double getAlt() {
        return this.alt;
    }

    /**
     * Getter for ap.
     * 
     * @return the ap
     */
    public double getAp() {
        return this.ap;
    }

    /**
     * Getter for apA.
     * 
     * @return the apA
     */
    public ApCoef getApA() {
        return this.apA;
    }

    /**
     * Getter for doy.
     * 
     * @return the doy
     */
    public int getDoy() {
        return this.doy;
    }

    /**
     * Getter for f107.
     * 
     * @return the f107
     */
    public double getF107() {
        return this.f107;
    }

    /**
     * Getter for f107A.
     * 
     * @return the f107A
     */
    public double getF107A() {
        return this.f107A;
    }

    /**
     * Getter for gLat.
     * 
     * @return the gLat
     */
    public double getgLat() {
        return this.gLat;
    }

    /**
     * Getter for gLong.
     * 
     * @return the gLong
     */
    public double getgLong() {
        return this.gLong;
    }

    /**
     * Getter for lst.
     * 
     * @return the lst
     */
    public double getLst() {
        return this.lst;
    }

    /**
     * Getter for sec.
     * 
     * @return the sec
     */
    public double getSec() {
        return this.sec;
    }

    // ================================= SETTERS =================================

    /**
     * Setter for alt.
     * 
     * @param alt
     *        the alt to set
     */
    public void setAlt(final double alt) {
        this.alt = alt;
    }

    /**
     * Setter for ap.
     * 
     * @param ap
     *        the ap to set
     */
    public void setAp(final double ap) {
        this.ap = ap;
    }

    /**
     * Setter for apA.
     * 
     * @param apA
     *        the apA to set
     */
    public void setApA(final ApCoef apA) {
        this.apA = apA;
    }

    /**
     * Setter for apA.
     * 
     * @param apA
     *        geomagnetic coefficients.
     */
    public void setApA(final double[] apA) {
        this.apA.setAp(apA);
    }

    /**
     * Setter for doy.
     * 
     * @param doy
     *        the doy to set
     */
    public void setDoy(final int doy) {
        this.doy = doy;
    }

    /**
     * Setter for f107.
     * 
     * @param f107
     *        the f107 to set
     */
    public void setF107(final double f107) {
        this.f107 = f107;
    }

    /**
     * Setter for f107A.
     * 
     * @param f107a
     *        the f107A to set
     */
    public void setF107A(final double f107a) {
        this.f107A = f107a;
    }

    /**
     * Setter for gLat.
     * 
     * @param gLat
     *        the gLat to set
     */
    public void setgLat(final double gLat) {
        this.gLat = gLat;
    }

    /**
     * Setter for gLong.
     * 
     * @param gLong
     *        the gLong to set
     */
    public void setgLong(final double gLong) {
        this.gLong = gLong;
    }

    /**
     * Setter for lst.
     * 
     * @param lst
     *        the lst to set
     */
    public void setLst(final double lst) {
        this.lst = lst;
    }

    /**
     * Setter for doy.
     * 
     * @param sec
     *        the sec to set
     */
    public void setSec(final double sec) {
        this.sec = sec;
    }

}
