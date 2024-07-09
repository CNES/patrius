/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.data;

import java.io.Serializable;

/**
 * Elements of the bodies having an effect on nutation.
 * <p>
 * This class is a simple placeholder, it does not provide any processing method.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public final class BodiesElements implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 9193325350743225370L;

    /** Mean anomaly of the Moon. */
    private final double l;

    /** Mean anomaly of the Sun. */
    private final double lPrime;

    /** L - &Omega; where L is the mean longitude of the Moon. */
    private final double f;

    /** Mean elongation of the Moon from the Sun. */
    private final double d;

    /** Mean longitude of the ascending node of the Moon. */
    private final double omega;

    /** Mean Mercury longitude. */
    private final double lMe;

    /** Mean Venus longitude. */
    private final double lVe;

    /** Mean Earth longitude. */
    private final double lE;

    /** Mean Mars longitude. */
    private final double lMa;

    /** Mean Jupiter longitude. */
    private final double lJu;

    /** Mean Saturn longitude. */
    private final double lSa;

    /** Mean Uranus longitude. */
    private final double lUr;

    /** Mean Neptune longitude. */
    private final double lNe;

    /** General accumulated precession in longitude. */
    private final double pa;

    /**
     * Simple constructor.
     * 
     * @param lIn
     *        mean anomaly of the Moon
     * @param lPrimeIn
     *        mean anomaly of the Sun
     * @param fIn
     *        L - &Omega; where L is the mean longitude of the Moon
     * @param dIn
     *        mean elongation of the Moon from the Sun
     * @param omegaIn
     *        mean longitude of the ascending node of the Moon
     * @param lMeIn
     *        mean Mercury longitude
     * @param lVeIn
     *        mean Venus longitude
     * @param lEIn
     *        mean Earth longitude
     * @param lMaIn
     *        mean Mars longitude
     * @param lJuIn
     *        mean Jupiter longitude
     * @param lSaIn
     *        mean Saturn longitude
     * @param lUrIn
     *        mean Uranus longitude
     * @param lNeIn
     *        mean Neptune longitude
     * @param paIn
     *        general accumulated precession in longitude
     */
    public BodiesElements(final double lIn, final double lPrimeIn, final double fIn, final double dIn,
        final double omegaIn,
        final double lMeIn, final double lVeIn, final double lEIn, final double lMaIn, final double lJuIn,
        final double lSaIn, final double lUrIn, final double lNeIn, final double paIn) {
        this.l = lIn;
        this.lPrime = lPrimeIn;
        this.f = fIn;
        this.d = dIn;
        this.omega = omegaIn;
        this.lMe = lMeIn;
        this.lVe = lVeIn;
        this.lE = lEIn;
        this.lMa = lMaIn;
        this.lJu = lJuIn;
        this.lSa = lSaIn;
        this.lUr = lUrIn;
        this.lNe = lNeIn;
        this.pa = paIn;
    }

    /**
     * Get the mean anomaly of the Moon.
     * 
     * @return mean anomaly of the Moon
     */
    public double getL() {
        return this.l;
    }

    /**
     * Get the mean anomaly of the Sun.
     * 
     * @return mean anomaly of the Sun.
     */
    public double getLPrime() {
        return this.lPrime;
    }

    /**
     * Get L - &Omega; where L is the mean longitude of the Moon.
     * 
     * @return L - &Omega;
     */
    public double getF() {
        return this.f;
    }

    /**
     * Get the mean elongation of the Moon from the Sun.
     * 
     * @return mean elongation of the Moon from the Sun.
     */
    public double getD() {
        return this.d;
    }

    /**
     * Get the mean longitude of the ascending node of the Moon.
     * 
     * @return mean longitude of the ascending node of the Moon.
     */
    public double getOmega() {
        return this.omega;
    }

    /**
     * Get the mean Mercury longitude.
     * 
     * @return mean Mercury longitude.
     */
    public double getLMe() {
        return this.lMe;
    }

    /**
     * Get the mean Venus longitude.
     * 
     * @return mean Venus longitude.
     */
    public double getLVe() {
        return this.lVe;
    }

    /**
     * Get the mean Earth longitude.
     * 
     * @return mean Earth longitude.
     */
    public double getLE() {
        return this.lE;
    }

    /**
     * Get the mean Mars longitude.
     * 
     * @return mean Mars longitude.
     */
    public double getLMa() {
        return this.lMa;
    }

    /**
     * Get the mean Jupiter longitude.
     * 
     * @return mean Jupiter longitude.
     */
    public double getLJu() {
        return this.lJu;
    }

    /**
     * Get the mean Saturn longitude.
     * 
     * @return mean Saturn longitude.
     */
    public double getLSa() {
        return this.lSa;
    }

    /**
     * Get the mean Uranus longitude.
     * 
     * @return mean Uranus longitude.
     */
    public double getLUr() {
        return this.lUr;
    }

    /**
     * Get the mean Neptune longitude.
     * 
     * @return mean Neptune longitude.
     */
    public double getLNe() {
        return this.lNe;
    }

    /**
     * Get the general accumulated precession in longitude.
     * 
     * @return general accumulated precession in longitude.
     */
    public double getPa() {
        return this.pa;
    }

}
