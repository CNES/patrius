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
 * @history Created on 09/11/2015
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:09/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import java.io.Serializable;

/**
 * <p>
 * This class is a simple container for generic ephemeris description parameters of GNSS satellites
 * (almanac or broadcast model ephemeris).
 * </p>
 *
 * @author fteilhard
 *
 *
 */
public class GNSSParameters implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 1741060082354960426L;

    /** GNSS Type : GPS, Galileo or BeiDou. */
    private final GNSSType gnssType;

    /** Time of applicability as a duration from the reference week (s). */
    private final double tRef;

    /** Initial Mean anomaly (rad) */
    private final double meanAnomaly0;

    /** Eccentricity */
    private final double eccentricity;

    /** Semi major axis square root (m^(1/2)) */
    private final double sqrtA;

    /** Initial right ascension (rad/s) */
    private final double rightAscension0;

    /** Orbital inclination (rad) */
    private final double i;

    /** Argument of perigee (rad) */
    private final double w;

    /** Rate of right ascension (rad/s) */
    private final double omegaRate;

    /** Clock correcting parameter af0 (s) */
    private final double af0;

    /** Clock correcting parameter af1 (s/s) */
    private final double af1;

    /** Clock correcting parameter af2 (s/s^2) */
    private final double af2;

    /** Mean motion difference from computed value (rad/s) */
    private final double deltaN;

    /** Rate of inclination angle (rad/s) */
    private final double iRate;

    /** Amplitude of the cosine harmonic correction term to the argument of latitude (rad) */
    private final double cuc;

    /** Amplitude of the sine harmonic correction term to the argument of latitude (rad) */
    private final double cus;

    /** Amplitude of the cosine harmonic correction term to the orbit radius (m) */
    private final double crc;

    /** Amplitude of the sine harmonic correction term to the orbit radius (m) */
    private final double crs;

    /** Amplitude of the cosine harmonic correction term to the angle of inclination (rad) */
    private final double cic;

    /** Amplitude of the sine harmonic correction term to the angle of inclination (rad) */
    private final double cis;

    /** Change rate in semi-major axis (m/s) */
    private final double aRate;

    /** Rate of mean motion difference from computed value (rad/s^2) */
    private final double deltaNRate;

    /**
     * Creates an instance of a generic GNSS Parameter (for GPS or BeiDou only)
     * @param gnssType
     *        the type of satellite (GPS, Galileo or BeiDou)
     * @param refDuration
     *        time applicability of the model
     * @param m0
     *        Initial Mean anomaly
     * @param e
     *        Eccentricity
     * @param squareRootA
     *        Semi major axis square root
     * @param omega0
     *        Initial Right ascencion
     * @param i0
     *        Orbital inclination
     * @param w0
     *        Argument of perigee
     * @param rateRa
     *        Rate of right ascension
     * @param af0
     *        Clock correcting parameter af0 (s)
     * @param af1
     *        Clock correcting parameter af1 (s/s)
     * @param af2
     *        Clock correcting parameter af2 (s/s2)
     * @param deltaN
     *        Mean motion difference from computed value (rad/s)
     * @param iRate
     *        Rate of inclination angle (rad/s)
     * @param cuc
     *        Amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     * @param cus
     *        Amplitude of the sine harmonic correction term to the argument of latitude (rad)
     * @param crc
     *        Amplitude of the cosine harmonic correction term to the orbit radius (m)
     * @param crs
     *        Amplitude of the sine harmonic correction term to the orbit radius (m)
     * @param cic
     *        Amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     * @param cis
     *        Amplitude of the sine harmonic correction term to the angle of inclination (rad)
     * @param aRate
     *        Change rate in semi-major axis (m/s)
     * @param deltaNRate
     *        Rate of mean motion difference from computed value (rad/s^2)
     */
    protected GNSSParameters(final GNSSType gnssType, final double refDuration, final double m0,
            final double e, final double squareRootA, final double omega0, final double i0, final double w0,
            final double rateRa, final double af0, final double af1, final double af2, final double deltaN,
            final double iRate, final double cuc, final double cus, final double crc, final double crs,
            final double cic, final double cis, final double aRate, final double deltaNRate) {
        this.gnssType = gnssType;
        this.tRef = refDuration;
        this.meanAnomaly0 = m0;
        this.eccentricity = e;
        this.sqrtA = squareRootA;
        this.rightAscension0 = omega0;
        this.i = i0;
        this.w = w0;
        this.omegaRate = rateRa;
        this.af0 = af0;
        this.af1 = af1;
        this.af2 = af2;
        this.deltaN = deltaN;
        this.iRate = iRate;
        this.cuc = cuc;
        this.cus = cus;
        this.crc = crc;
        this.crs = crs;
        this.cic = cic;
        this.cis = cis;
        this.aRate = aRate;
        this.deltaNRate = deltaNRate;

    }

    /**
     * @return the gnssType
     */
    public GNSSType getGnssType() {
        return gnssType;
    }

    /**
     * @return the initial mean anomaly
     */
    public double getMeanAnomalyInit() {
        return this.meanAnomaly0;
    }

    /**
     * @return the eccentricity
     */
    public double getEccentricity() {
        return this.eccentricity;
    }

    /**
     * @return the square root of the semi-major axis
     */
    public double getSqrtA() {
        return this.sqrtA;
    }

    /**
     * @return the initial right ascension of ascending node
     */
    public double getOmegaInit() {
        return this.rightAscension0;
    }

    /**
     * @return the orbital inclination
     */
    public double getI() {
        return this.i;
    }

    /**
     * @return the Argument of perigee
     */
    public double getW() {
        return this.w;
    }

    /**
     * @return the Rate of right ascension
     */
    public double getOmegaRate() {
        return this.omegaRate;
    }

    /**
     * @return the number of seconds in the week
     */
    public double gettRef() {
        return this.tRef;
    }

    /**
     * @return the Clock correcting parameter af0 (s)
     */
    public double getAf0() {
        return af0;
    }

    /**
     * @return the Clock correcting parameter af1 (s/s)
     */
    public double getAf1() {
        return af1;
    }

    /**
     * @return the Clock correcting parameter af2 (s/s^2)
     */
    public double getAf2() {
        return af2;
    }

    /**
     * @return the Mean motion difference from computed value (rad/s)
     */
    public double getDeltaN() {
        return deltaN;
    }

    /**
     * @return the Rate of inclination angle (rad/s)
     */
    public double getiRate() {
        return iRate;
    }

    /**
     * @return the Amplitude of the cosine harmonic correction term to the argument of latitude
     *         (rad)
     */
    public double getCuc() {
        return cuc;
    }

    /**
     * @return the Amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public double getCus() {
        return cus;
    }

    /**
     * @return the Amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public double getCrc() {
        return crc;
    }

    /**
     * @return the Amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public double getCrs() {
        return crs;
    }

    /**
     * @return the Amplitude of the cosine harmonic correction term to the angle of inclination
     *         (rad)
     */
    public double getCic() {
        return cic;
    }

    /**
     * @return the Amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public double getCis() {
        return cis;
    }

    /**
     * @return the Change rate in semi-major axis (m/s)
     */
    public double getaRate() {
        return aRate;
    }

    /**
     * @return the Rate of mean motion difference from computed value (rad/s^2)
     */
    public double getDeltaNRate() {
        return deltaNRate;
    }

}
