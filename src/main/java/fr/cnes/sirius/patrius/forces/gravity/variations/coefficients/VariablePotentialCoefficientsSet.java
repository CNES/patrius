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
 * @history Created 07/11/2012
 *
 * HISTORY
 * VERSION:4.11:FA:FA-3316:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un potentiel variable
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Represents a variable potential coefficients set for a given degree and order.
 *
 * @concurrency immutable
 *
 * @see {@link http://grgs.obs-mip.fr/grace/variable-models-grace-lageos/formats}
 *
 * @author Rami Houdroge
 *
 * @version $Id: VariablePotentialCoefficientsSet.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 1.3
 */
public class VariablePotentialCoefficientsSet implements Serializable {
   
    /** The three next integer are use to know is a year is a leap year or not. See isLeapYear() method */
    /** One hundred */
    private static final int HUNDRED = 100;

    /** Four */
    private static final int FOUR = 4;

    /** Four hundred */
    private static final int FOUR_HUNDRED = 400;

    /** Number of days in a normal year */
    private static final int NUMBER_OF_DAYS_IN_CLASSICAL_YEAR = 365;

    /** Number of days in a leap year */
    private static final int NUMBER_OF_DAYS_IN_LEAP_YEAR = 366;
    
    /**
     * Method to compute the periodic component of the variable coefficients. </br>
     * The {@code PeriodicComputationMethod#LEAP_YEAR} is coded for cross-validation purposes.
     */
    private static PeriodicComputationMethod periodicComputationMethod = PeriodicComputationMethod.HOMOGENEOUS;

    /** Serializable UID. */
    private static final long serialVersionUID = 3662552678425158131L;

    /** Degree */
    private final int degree;

    /** Order */
    private final int order;

    /** C */
    private final double coefC;

    /** C_DRIFT */
    private final double coefCDrift;

    /** C_S1A */
    private final double coefCSin1A;

    /** C_C1A */
    private final double coefCCos1A;

    /** C_S2A */
    private final double coefCSin2A;

    /** C_C2A */
    private final double coefCCos2A;
    /** S */
    private final double coefS;

    /** S_DRIFT */
    private final double coefSDrift;

    /** S_S1A */
    private final double coefSSin1A;

    /** S_C1A */
    private final double coefSCos1A;

    /** S_S2A */
    private final double coefSSin2A;

    /** S_C2A */
    private final double coefSCos2A;

    /**
     * Create a set for a given order and degree
     *
     * @param degree
     *        degree of set
     * @param order
     *        order of set
     * @param c
     *        C correction parameters
     * @param s
     *        S correction parameters
     * @param cc
     *        c coefficient corrections {DOT, S1A, C1A, S2A, C2A}
     * @param sc
     *        s coefficient corrections {DOT, S1A, C1A, S2A, C2A}
     */
    public VariablePotentialCoefficientsSet(final int degree, final int order, final double c, final double s,
                                            final double[] cc, final double[] sc) {
        this(degree, order,
                c, cc[0], cc[1], cc[2], cc[3], cc[4],
                s, sc[0], sc[1], sc[2], sc[3], sc[4]);
        checksanityCCSCSize(cc, sc);
    }

    /**
     * Create a set for a given order and degree
     *
     * @param degree
     *        degree of set
     * @param order
     *        order of set
     * @param c
     *        normalized c coefficient
     * @param cDrift
     *        C_DRIFT
     * @param cS1A
     *        sinus 1A c coefficient
     * @param cC1A
     *        cosinus 1A c coefficient
     * @param cS2A
     *        sinus 2A c coefficient
     * @param cC2A
     *        cosinus 2A c coefficient
     * @param s
     *        normalized s coefficient
     * @param sDrift
     *        S_DRIFT
     * @param sS1A
     *        sinus 1A s coefficient
     * @param sC1A
     *        cosinus 1A s coefficient
     * @param sS2A
     *        sinus 2A s coefficient
     * @param sC2A
     *        cosinus 2A s coefficient
     *
     */
    public VariablePotentialCoefficientsSet(final int degree, final int order,
                                            final double c, final double cDrift, final double cS1A, final double cC1A,
                                            final double cS2A, final double cC2A,
                                            final double s, final double sDrift, final double sS1A, final double sC1A,
                                            final double sS2A, final double sC2A) {

        checkSanityOrdDeg(degree, order);

        this.degree = degree;
        this.order = order;
        this.coefC = c;
        this.coefCDrift = cDrift;
        this.coefCSin1A = cS1A;
        this.coefCCos1A = cC1A;
        this.coefCSin2A = cS2A;
        this.coefCCos2A = cC2A;

        this.coefS = s;
        this.coefSDrift = sDrift;
        this.coefSSin1A = sS1A;
        this.coefSCos1A = sC1A;
        this.coefSSin2A = sS2A;
        this.coefSCos2A = sC2A;
    }

    /**
     * Check sanity of Order and Degree input arguments
     * 
     * @param d
     *        degree
     * @param o
     *        order
     */
    private static void checkSanityOrdDeg(final int d, final int o) {
        if (d < 0 || o < 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Check sanity of Order and Degree input arguments
     * 
     * @param cc
     *        c corrections
     * @param sc
     *        s corrections
     */
    private static void checksanityCCSCSize(final double[] cc, final double[] sc) {
        if (cc.length != 5 || sc.length != 5) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the degree of the set
     */
    public int getDegree() {
        return this.degree;
    }

    /**
     * @return the order of the set
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * Getter for normalized coefC
     *
     * @return the coefC
     */
    public double getCoefC() {
        return this.coefC;
    }

    /**
     * Getter for normalized coefCDrift
     *
     * @return the coefCDrift
     */
    public double getCoefCDrift() {
        return this.coefCDrift;
    }

    /**
     * Getter for normalized coefCSin1A
     *
     * @return the coefCSin1A
     */
    public double getCoefCSin1A() {
        return this.coefCSin1A;
    }

    /**
     * Getter for normalized coefCCos1A
     *
     * @return the coefCCos1A
     */
    public double getCoefCCos1A() {
        return this.coefCCos1A;
    }

    /**
     * Getter for normalized coefCSin2A
     *
     * @return the coefCSin2A
     */
    public double getCoefCSin2A() {
        return this.coefCSin2A;
    }

    /**
     * Getter for normalized coefCCos2A
     *
     * @return the coefCCos2A
     */
    public double getCoefCCos2A() {
        return this.coefCCos2A;
    }

    /**
     * Getter for normalized coefS
     *
     * @return the coefS
     */
    public double getCoefS() {
        return this.coefS;
    }

    /**
     * Getter for normalized coefSDrift
     *
     * @return the coefSDrift
     */
    public double getCoefSDrift() {
        return this.coefSDrift;
    }

    /**
     * Getter for normalized coefSSin1A
     *
     * @return the coefSSin1A
     */
    public double getCoefSSin1A() {
        return this.coefSSin1A;
    }

    /**
     * Getter for normalized coefSCos1A
     *
     * @return the coefSCos1A
     */
    public double getCoefSCos1A() {
        return this.coefSCos1A;
    }

    /**
     * Getter for normalized coefSSin2A
     *
     * @return the coefSSin2A
     */
    public double getCoefSSin2A() {
        return this.coefSSin2A;
    }

    /**
     * Getter for normalized coefSCos2A
     *
     * @return the coefSCos2A
     */
    public double getCoefSCos2A() {
        return this.coefSCos2A;
    }

    /**
     * Compute the normalized drift component of the C coefficient
     *
     * @param driftFunction
     *        between two dates value
     * @return double
     *         C drift component
     */
    public double computeCDriftComponent(final double driftFunction) {
        return this.coefCDrift * driftFunction;
    }

    /**
     * Compute the normalized drift component of the S coefficient
     *
     * @param driftFunction
     *        between two dates value
     * @return double
     *         S drift component
     */
    public double computeSDriftComponent(final double driftFunction) {
        return this.coefSDrift * driftFunction;
    }

    /**
     * Compute the normalized periodic component of the C coefficient
     *
     * @param sin2Pi
     *        sinus(2*pi*time) component
     * @param cos2Pi
     *        cosinus(2*pi*time) component
     * @param sin4Pi
     *        sinus(4*pi*time) component
     * @param cos4Pi
     *        cosinus(4*pi*time) component
     * @return double
     *         C periodic component
     */
    public double computeCPeriodicComponent(final double sin2Pi, final double cos2Pi, final double sin4Pi,
                                            final double cos4Pi) {
        return this.coefCSin1A * sin2Pi + this.coefCCos1A * cos2Pi + this.coefCSin2A * sin4Pi + this.coefCCos2A
                * cos4Pi;
    }

    /**
     * Compute the normalized periodic component of the S coefficient
     *
     * @param sin2Pi
     *        sinus(2*pi*time) component
     * @param cos2Pi
     *        cosinus(2*pi*time) component
     * @param sin4Pi
     *        sinus(4*pi*time) component
     * @param cos4Pi
     *        cosinus(4*pi*time) component
     * @return double
     *         S periodic component
     */
    public double computeSPeriodicComponent(final double sin2Pi, final double cos2Pi, final double sin4Pi,
                                            final double cos4Pi) {
        return this.coefSSin1A * sin2Pi + this.coefSCos1A * cos2Pi + this.coefSSin2A * sin4Pi + this.coefSCos2A
                * cos4Pi;
    }

    /**
     * Compute drift function to provide to {@code #computeCDriftComponent} or {@code #computeSDriftComponent}
     *
     * @param date
     *        final date
     * @param refDate
     *        starting date
     * @return double
     *         drift function
     */
    public static double computeDriftFunction(final AbsoluteDate date, final AbsoluteDate refDate) {
        return (date.durationFrom(refDate)) / Constants.JULIAN_YEAR;
    }

    /**
     * Compute periodic functions to provide to {@code #computeCPeriodicComponent} or {@code #computeSPeriodicComponent}
     *
     * @param date
     *        choosen date
     * @return double[] 
     *         Values of sin2Pi, cos2Pi, sin4Pi, cos4Pi for the provided date
     */
    public static double[] computePeriodicFunctions(final AbsoluteDate date) {
        // Time for the periodic functions
        final double elapsedPeriodic = periodicComputationMethod.computeElapsedPeriodic(date);

        // Evaluation of the periodic functions
        final double[] sinCosAndSinCos2 = new double[4];
        MathLib.sinAndCos(2 * FastMath.PI * elapsedPeriodic, sinCosAndSinCos2);
        final double sin2Pi = sinCosAndSinCos2[0];
        final double cos2Pi = sinCosAndSinCos2[1];
        sinCosAndSinCos2[2] = 2. * sin2Pi * cos2Pi;
        sinCosAndSinCos2[3] = cos2Pi * cos2Pi - sin2Pi * sin2Pi;
        return sinCosAndSinCos2;
    }

    /**
     * Getter for periodicComputationMethod
     *
     * @return the periodicComputationMethod
     */
    public static PeriodicComputationMethod getPeriodicComputationMethod() {
        return periodicComputationMethod;
    }

    /**
     * Setter for periodicComputationMethod
     *
     * @param periodicComputationMethod the periodicComputationMethod to set
     */
    public static void setPeriodicComputationMethod(final PeriodicComputationMethod periodicComputationMethod) {
        VariablePotentialCoefficientsSet.periodicComputationMethod = periodicComputationMethod;
    }

    /**
     * Enum designating the method to compute the phase of the periodic functions
     *
     */
    public enum PeriodicComputationMethod {

        /**
         * This method does not depend on the leap years.
         */
        HOMOGENEOUS {
            @Override
            public double computeElapsedPeriodic(final AbsoluteDate date) {
                return date.durationFrom(PERIODIC_ORIGIN) / Constants.JULIAN_YEAR;
            }
        },
        /**
         * This method has the advantage to have a sin function to 0 (and a cosine function to 1) every 1st january.
         */
        LEAP_YEAR {
            @Override
            public double computeElapsedPeriodic(final AbsoluteDate date) {

                final DateTimeComponents dateTimeComponents = date.getComponents(TimeScalesFactory.getTAI());
                final double decimalDay = dateTimeComponents.getTime().getSecondsInDay() / Constants.JULIAN_DAY;
                final int daysSinceBeginningOfYear = dateTimeComponents.getDate().getDayOfYear() - 1;
                final int year = dateTimeComponents.getDate().getYear();
                double elapsedPeriodic = 0;
                if (isLeapYear(year)) {
                    elapsedPeriodic = (daysSinceBeginningOfYear + decimalDay) / NUMBER_OF_DAYS_IN_LEAP_YEAR;
                } else {
                    elapsedPeriodic = (daysSinceBeginningOfYear + decimalDay) / NUMBER_OF_DAYS_IN_CLASSICAL_YEAR;
                }
                return elapsedPeriodic;
            }
        };

        /**
         * Origin of the date for the Homogeneous method
         */
        private static final AbsoluteDate PERIODIC_ORIGIN = new AbsoluteDate(2005, 1, 1, TimeScalesFactory.getTAI());

        /**
         * Compute the phase of the periodic function
         *
         * @param  date
         *         initial date
         * @return double
         *         fraction of year
         */
        public abstract double computeElapsedPeriodic(AbsoluteDate date);

        /**
         * Return if the year is a leap one or not
         *
         * @param year
         *        year to be checked if it is a leap one or not
         * @return boolean
         *         true if the year is a leap year. If not, return false
         */
        public boolean isLeapYear(final int year) {
            // To be a leap year the year shall be divisible by 4 but not by 100 or divisible by 400
            boolean leapYear = false;
            if (((year % FOUR) == 0 && (year % HUNDRED) != 0) || (year % FOUR_HUNDRED) == 0) {
                leapYear = true;
            }
            return leapYear;
        }
    }

}