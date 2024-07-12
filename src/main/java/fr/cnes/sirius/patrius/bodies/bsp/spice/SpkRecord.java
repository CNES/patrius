/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class for the reading and evaluation of SPK records. 
 * The interpolation and evaluation of the Chebyshev polynome is 
 * also done here.
 * @author T0281925
 *
 */
public final class SpkRecord {

    /** Max size of a record*/
    private static final int MAXREC = 198;
    
    
    /**
     * Constructor
     */
    private SpkRecord(){
     // Nothing to do
    }
    
    /**
     * Read a single SPK data record from a segment of type 2 (Chebyshev, position only).
     * Based on the SPKR02 routine from the SPICE library.
     * @param handle File handle.
     * @param descr Segment descriptor.
     * @param epoch Evaluation epoch.
     * @return RECORD. It is an array of data from the specified segment which,
     *         when evaluated at epoch ET, will give the state
     *         (position and velocity) of the target body identified
     *         by the input segment descriptor. The descriptor
     *         specifies the center of motion and reference frame of
     *         the state.
     *
     *         The structure of the record is as follows:
     *
     *            +--------------------------------------+
     *            | record size (excluding this element) |
     *            +--------------------------------------+
     *            | Coverage interval midpoint           |
     *            +--------------------------------------+
     *            | Coverage interval radius             |
     *            +--------------------------------------+
     *            | Coeffs for X position component      |
     *            +--------------------------------------+
     *            | Coeffs for Y position component      |
     *            +--------------------------------------+
     *            | Coeffs for Z position component      |
     *            +--------------------------------------+
     *
     *         In the above record
     *
     *            - Times are expressed as seconds past J2000 TDB.
     *            - Position components have units of km.
     *
     *         RECORD must be declared by the caller with size large
     *         enough to accommodate the largest record that can be
     *         returned by this routine. 
     * @throws PatriusException if there is a problem reading the records.
     */
    public static double[] readType2(final int handle, 
                                     final double[] descr, 
                                     final double epoch) throws PatriusException {
        
        final double[] dc = new double[SpkFile.ND];
        final int[] ic = new int[SpkFile.NI];
        
        // Unpack the segment descriptor
        SpiceCommon.unpackSummary(descr, SpkFile.ND, SpkFile.NI, dc, ic);
        
        final int begin = ic[4];
        final int end = ic[5];
        
        // The segment is made up of a number of logical records, each
        // having the same size, and covering the same length of time.
        //
        // We can determine which record to return using the input epoch,
        // the initial time of the first record's coverage interval, and the
        // length of the interval covered by each record. These constants
        // are located at the end of the segment, along with the size of
        // each logical record and the total number of records.
        final double[] recordFind = DafReader.readDataDaf(handle, end - 3, end);
        
        final double init = recordFind[0];
        final double intlen = recordFind[1];
        final int recsiz = (int) recordFind[2];
        final int nrec = (int) recordFind[3];
        
        int recno = ( (int) ((epoch -init)/intlen ) ) + 1;
        recno = MathLib.min(recno, nrec);
        
        // Compute the address of the desired record
        final int recadr = (recno - 1) * recsiz + begin;
        
        // Along with the record, return the size of the record.
        final double[] record = new double[MAXREC];
        record[0] = recordFind[2];
        
        final double[] temp = DafReader.readDataDaf(handle, recadr, recadr + recsiz - 1);
        System.arraycopy(temp, 0, record, 1, recsiz);
        return record;
    }
    
    /**
     * Evaluate a single data record from an PCK or SPK segment of type
     * 2 (Chebyshev Polynomials, 3 components).
     * 
     * Based on the SPKE02 routine from the SPICE library
     * 
     * @param epoch Evaluation epoch.
     * @param record Data record.
     * @return is a 6-vector. In order, the components of XYZDOT are
     *         X, Y, Z, X', Y', and Z'. Units for state evaluations
     *         will be km and km/sec. Units for angles will be
     *         radians and radians/sec.
     * @throws PatriusException if there is a problem with the number of coefs or the domain radius
     */
    public static double[] evaluateType2(final double epoch,
                                          final double[] record) throws PatriusException {     
        // The first number in the record is the record size.  Following it
        // are two numbers that will be used later, then the three sets of
        // coefficients.  The number of coefficients for each variable can
        // be determined from the record size, since there are the same
        // number of coefficients for each variable.
        final int ncof = ( ((int) record[0]) - 2 )/ 3;
        
        if (ncof < 1) {
            throw new PatriusException(PatriusMessages.PDB_NCOF_SPK_RECORD_NEGATIVE, ncof);
        }
        
        // Check the radius of the domain interval.
        
        if ( record[2] <= 0 ) {
            throw new PatriusException(PatriusMessages.PDB_SPK_INTERVAL_RADIUS_NEGATIVE, record[2]);
        }
        
        // The degree of each polynomial is one less than the number of coefficients.
        final int degp = ncof - 1;
        
        // Call interpolateChebyshevExpansion once for each variable to evaluate the position and velocity values.
        final double[] xyzdot = new double[6];
        for (int i = 0; i <= 2; i++) {
            // The coefficients for each variable are located contiguously,
            // following the first three words in the record.
            final int cofloc = ncof * i + 3;
            
            // interpolateChebyshevExpansion needs as input the coefficients, the degree of the
            // polynomial, the epoch, and also two variable transformation
            // parameters, which are located, in our case, in the second and
            // third slots of the record.
            final double[] comp = interpolateChebyshevExpansion(Arrays.copyOfRange(record, cofloc, cofloc + ncof), 
                                                                degp,
                                                                Arrays.copyOfRange(record, 1, 3),
                                                                epoch); 
            xyzdot[i] = comp[0];
            xyzdot[i + 3] = comp[1];
        }
        
        return xyzdot;
    }
    
    /**
     * Read a single SPK data record from a segment of type 3 (Chebyshev coefficients, position and velocity).
     * Based on the SPKR03 routine from the SPICE library
     * @param handle File handle.
     * @param descr Segment descriptor.
     * @param epoch Evaluation epoch.
     * @return RECORD. It is an array of data from the specified segment which,
     *         when evaluated at epoch ET, will give the state
     *         (position and velocity) of the target body identified
     *         by the input segment descriptor. The descriptor
     *         specifies the center of motion and reference frame of
     *         the state.
     *
     *         The structure of the record is as follows:
     *
     *            +--------------------------------------+
     *            | record size (excluding this element) |
     *            +--------------------------------------+
     *            | Coverage interval midpoint           |
     *            +--------------------------------------+
     *            | Coverage interval radius             |
     *            +--------------------------------------+
     *            | Coeffs for X position component      |
     *            +--------------------------------------+
     *            | Coeffs for Y position component      |
     *            +--------------------------------------+
     *            | Coeffs for Z position component      |
     *            +--------------------------------------+
     *            | Coeffs for X velocity component      |
     *            +--------------------------------------+
     *            | Coeffs for Y velocity component      |
     *            +--------------------------------------+
     *            | Coeffs for Z velocity component      |
     *            +--------------------------------------+
     *
     *         In the above record
     *
     *            - Times are expressed as seconds past J2000 TDB.
     *            - Position components have units of km.
     *            - Velocity components have units of km/s.
     *
     * @throws PatriusException if there is a problem reading the records.
     */
    public static double[] readType3(final int handle, 
                                     final double[] descr, 
                                     final double epoch) throws PatriusException{
        final double[] dc = new double[SpkFile.ND];
        final int[] ic = new int[SpkFile.NI];
        
        // Unpack the segment descriptor
        SpiceCommon.unpackSummary(descr, SpkFile.ND, SpkFile.NI, dc, ic);
        
        final int begin = ic[4];
        final int end = ic[5];
        
        // The segment is made up of a number of logical records, each
        // having the same size, and covering the same length of time.
        //
        // We can determine which record to return using the input epoch,
        // the initial time of the first record's coverage interval, and the
        // length of the interval covered by each record. These constants
        // are located at the end of the segment, along with the size of
        // each logical record and the total number of records.
        final double[] recordFind = DafReader.readDataDaf(handle, end - 3, end);
        
        final double init = recordFind[0];
        final double intlen = recordFind[1];
        final int recsiz = (int) recordFind[2];
        final int nrec = (int) recordFind[3];
        
        int recno = ( (int) ((epoch -init)/intlen ) ) + 1;
        recno = MathLib.min(recno, nrec);
        
        // Compute the address of the desired record
        final int recadr = (recno - 1) * recsiz + begin;
        
        // Along with the record, return the size of the record.
        final double[] record = new double[MAXREC];
        record[0] = recordFind[2];
        
        final double[] temp = DafReader.readDataDaf(handle, recadr, recadr + recsiz - 1);
        System.arraycopy(temp, 0, record, 1, recsiz);
        return record;     
    }
    
    /**
     * Evaluate a single data record from asegment of type 3
     * (Chebyshev Polynomials, position and velocity).
     * 
     * Based on the SPKE02 routine from the SPICE library
     * 
     * @param epoch Evaluation epoch.
     * @param record Data record.
     * @return is a 6-vector. In order, the components of XYZDOT are
     *         X, Y, Z, X', Y', and Z'. Units for state evaluations
     *         will be km and km/sec. Units for angles will be
     *         radians and radians/sec.
     * @throws PatriusException if there is a problem with the number of coefs or the domain radius
     */
    public static double[] evaluateType3(final double epoch,
                                         final double[] record) throws PatriusException { 
        // The first number in the record is the record size.  Following it
        // are two numbers that will be used later, then the three sets of
        // coefficients.  The number of coefficients for each variable can
        // be determined from the record size, since there are the same
        // number of coefficients for each variable.
        final int ncof = ( ((int) record[0]) - 2 )/ 6;
        
        if (ncof < 1) {
            throw new PatriusException(PatriusMessages.PDB_NCOF_SPK_RECORD_NEGATIVE, ncof);
        }
        
        // Check the radius of the domain interval.
        
        if ( record[2] <= 0 ) {
            throw new PatriusException(PatriusMessages.PDB_SPK_INTERVAL_RADIUS_NEGATIVE, record[2]);
        }
        
        // The degree of each polynomial is one less than the number of coefficients.
        final int degp = ncof - 1;
        
        // Call evaluateChebyshevExpansion once for each variable to evaluate the position and velocity values.
        final double[] state = new double[6];
        for (int i = 0; i <= 5; i++) {
            // The coefficients for each variable are located contiguously,
            // following the first three words in the record.
            final int cofloc = ncof * i + 3;
            
            // interpolateChebyshevExpansion needs as input the coefficients, the degree of the
            // polynomial, the epoch, and also two variable transformation
            // parameters, which are located, in our case, in the second and
            // third slots of the record.
            state[i] = evaluateChebyshevExpansion(Arrays.copyOfRange(record, cofloc, cofloc + ncof), 
                                                                degp,
                                                                Arrays.copyOfRange(record, 1, 3),
                                                                epoch); 
        }
        
        return state;
    }
    
    /**
     * Return the value of a polynomial and its derivative, evaluated at
     * the input X, using the coefficients of the Chebyshev expansion of
     * the polynomial.
     * Based on the CHBINT routine from the SPICE routine.
     * 
     * @param chparam DEGP+1 Chebyshev polynomial coefficients.
     * @param degp Degree of polynomial.
     * @param x2s Transformation parameters of polynomial.
     * @param x Value for which the polynomial is to be evaluated
     * @return double array containing the value of the polynomial at X and 
     *         the value of the derivative of the polynomial at X
     */
    private static double[] interpolateChebyshevExpansion(final double[] chparam, 
                                                          final int degp, 
                                                          final double[] x2s, 
                                                          final double x) {
        final double[] pdev = new double[2];
        
        // Transform X to S and initialize temporary variables.
        final double s = (x - x2s[0]) / x2s[1];
        final double s2 = 2 * s;
        int j = degp;
        final double[] w = new double[3];
        Arrays.fill(w, 0);
        final double[] dw = new double[3];
        Arrays.fill(dw, 0);
        
        // Evaluate the polynomial and its derivative using recursion.
        while (j > 0) {
            w[2] = w[1];
            w[1] = w[0];
            w[0] = chparam[j] + (s2 * w[1] - w[2]);
            
            dw[2] = dw[1];
            dw[1] = dw[0];
            dw[0] = w[1] * 2 + s2 * dw[1] - dw[2];
            
            j--;
        }
        
        pdev[0] = chparam[0] + ( s * w[0] - w[1]);
        pdev[1] = w[0] + s*dw[0] - dw[1];
        
        // Scale the derivative by 1/X2S(2) so that we have the derivative
        //
        //                   d P(S)
        //                   ------
        //                     dX
        pdev[1] /= x2s[1];
        
        return pdev;
    }
    
    /**
     * Return the value of a polynomial evaluated at the input X using
     * the coefficients for the Chebyshev expansion of the polynomial.
     * Based on the CHBVAL routine from the SPICE routine.
     * 
     * @param chparam DEGP+1 Chebyshev polynomial coefficients.
     * @param degp Degree of polynomial.
     * @param x2s Transformation parameters of polynomial.
     * @param x Value for which the polynomial is to be evaluated
     * @return double containing the value of the polynomial at X.
     */
    private static double evaluateChebyshevExpansion(final double[] chparam, 
                                                     final int degp, 
                                                     final double[] x2s, 
                                                     final double x) {        
        // Transform X to S and initialize temporary variables.
        final double s = (x - x2s[0]) / x2s[1];
        final double s2 = 2 * s;
        int j = degp;
        final double[] w = new double[3];
        // Fill the array with 0s.
        Arrays.fill(w, 0);
        
        // Evaluate the polynomial and using recursion.
        while (j > 0) {
            w[2] = w[1];
            w[1] = w[0];
            w[0] = chparam[j] + (s2 * w[1] - w[2]);
            
            j--;
        }
        
        // Return the last evaluation
        return ( s* w[0] - w[1]) + chparam[0];
    }
}
